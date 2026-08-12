package mar.runtime.session;

import mar.runtime.groovy.GroovyRuntime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionManagerTest {
    @TempDir
    Path projectRoot;

    private GroovyRuntime groovyRuntime;
    private SessionManager sessions;

    @BeforeEach
    void setUp() {
        groovyRuntime = new GroovyRuntime();
        sessions = new SessionManager(projectRoot, groovyRuntime);
    }

    @AfterEach
    void tearDown() {
        sessions.close();
        groovyRuntime.close();
    }

    @Test
    void evaluatesGroovyAndKeepsVarsAcrossCalls() {
        RuntimeSession session = sessions.defaultSession();

        assertEquals(3, session.evaluate("1 + 2"));
        assertEquals(41, session.evaluate("vars.value = 41"));
        assertEquals(42, session.evaluate("vars.value + 1"));
        assertSame(session, sessions.defaultSession());
    }

    @Test
    void unknownNamedSessionIsCreatedAndIsolated() {
        RuntimeSession alpha = sessions.getOrCreate("alpha");
        RuntimeSession beta = sessions.getOrCreate("beta");

        alpha.evaluate("vars.value = 'A'");
        beta.evaluate("vars.value = 'B'");

        assertNotSame(alpha, beta);
        assertEquals("A", alpha.evaluate("vars.value"));
        assertEquals("B", beta.evaluate("vars.value"));
        assertEquals(Set.of("alpha", "beta"),
                sessions.sessions().stream().map(RuntimeSession::name).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void installsOnlyTheFrozenBaseBindingInServerLikeHarness() {
        RuntimeSession session = sessions.defaultSession();

        assertTrue(session.binding().hasVariable("vars"));
        assertTrue(session.binding().hasVariable("ref"));
        assertTrue(session.binding().hasVariable("runtime"));
        assertFalse(session.binding().hasVariable("mc"));
        for (String forbidden : Set.of("player", "screen", "server", "inventory", "world", "ui")) {
            assertFalse(session.binding().hasVariable(forbidden), forbidden + " must not be pre-bound");
        }
        assertTrue(session.evaluate("vars.tools != null") instanceof Boolean);
        assertEquals("default", session.evaluate("runtime.sessionName"));
        assertEquals(projectRoot.toString(), session.evaluate("runtime.projectRoot"));
    }

    @Test
    void unknownRefFailsClearlyWithoutInventingAHandle() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> sessions.defaultSession().evaluate("ref('@1')"));

        assertTrue(error.getMessage().contains("Unknown handle"));
        assertTrue(error.getMessage().contains("@1"));
    }

    @Test
    void blankSessionNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> sessions.getOrCreate(""));
        assertThrows(IllegalArgumentException.class, () -> sessions.getOrCreate("  "));
    }

    @Test
    void sameSessionEvaluationIsSerialized() throws Exception {
        RuntimeSession session = sessions.defaultSession();
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximum = new AtomicInteger();
        session.vars().setProperty("active", active);
        session.vars().setProperty("maximum", maximum);

        int calls = 12;
        ExecutorService executor = Executors.newFixedThreadPool(calls);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?>[] futures = new Future<?>[calls];
            for (int index = 0; index < calls; index++) {
                futures[index] = executor.submit(() -> {
                    start.await();
                    session.evaluate("""
                            def now = vars.active.incrementAndGet()
                            vars.maximum.accumulateAndGet(now) { previous, current -> Math.max(previous, current) }
                            Thread.sleep(5)
                            vars.active.decrementAndGet()
                            """);
                    return null;
                });
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, maximum.get());
        assertEquals(0, active.get());
    }

    @Test
    void differentSessionsCanExecuteConcurrently() throws Exception {
        RuntimeSession alpha = sessions.getOrCreate("alpha");
        RuntimeSession beta = sessions.getOrCreate("beta");
        CountDownLatch bothEntered = new CountDownLatch(2);
        alpha.vars().setProperty("bothEntered", bothEntered);
        beta.vars().setProperty("bothEntered", bothEntered);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> alpha.evaluate("""
                    vars.bothEntered.countDown()
                    assert vars.bothEntered.await(2, java.util.concurrent.TimeUnit.SECONDS)
                    'alpha'
                    """));
            Future<?> second = executor.submit(() -> beta.evaluate("""
                    vars.bothEntered.countDown()
                    assert vars.bothEntered.await(2, java.util.concurrent.TimeUnit.SECONDS)
                    'beta'
                    """));

            assertEquals("alpha", first.get(5, TimeUnit.SECONDS));
            assertEquals("beta", second.get(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }
}
