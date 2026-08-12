package mar.runtime.io;

import mar.runtime.groovy.GroovyRuntime;
import mar.runtime.result.EvaluationResult;
import mar.runtime.session.RuntimeSession;
import mar.runtime.session.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvalOutputCaptureIntegrationTest {
    @TempDir
    Path projectRoot;

    private PrintStream previousOut;
    private PrintStream previousErr;
    private ByteArrayOutputStream fallbackOutBytes;
    private ByteArrayOutputStream fallbackErrBytes;
    private ThreadLocalPrintRouter router;
    private GroovyRuntime groovyRuntime;
    private SessionManager sessions;

    @BeforeEach
    void setUp() {
        previousOut = System.out;
        previousErr = System.err;
        fallbackOutBytes = new ByteArrayOutputStream();
        fallbackErrBytes = new ByteArrayOutputStream();
        PrintStream fallbackOut = new PrintStream(fallbackOutBytes, true, StandardCharsets.UTF_8);
        PrintStream fallbackErr = new PrintStream(fallbackErrBytes, true, StandardCharsets.UTF_8);
        router = ThreadLocalPrintRouter.install(fallbackOut, fallbackErr);
        groovyRuntime = new GroovyRuntime();
        sessions = new SessionManager(projectRoot, groovyRuntime, new EvalOutputCapture(router));
    }

    @AfterEach
    void tearDown() {
        sessions.close();
        groovyRuntime.close();
        router.close();
        System.setOut(previousOut);
        System.setErr(previousErr);
    }

    @Test
    void capturesGroovyPrintAndCurrentThreadSystemStreams() {
        EvaluationResult result = sessions.defaultSession().evaluateBridged("""
                print 'groovy-输出'
                println '-line'
                System.out.print('|system-out|')
                System.err.print('system-err')
                42
                """);

        assertTrue(result.ok());
        assertEquals(42, result.result());
        assertTrue(result.stdout().startsWith("groovy-输出-line"));
        assertTrue(result.stdout().contains("|system-out|"));
        assertEquals("system-err", result.stderr());
    }

    @Test
    void preservesOutputWrittenBeforeExceptionAndNextEvalStartsClean() {
        RuntimeSession session = sessions.defaultSession();

        EvaluationResult failed = session.evaluateBridged("""
                print 'before-failure-out'
                System.err.print('before-failure-err')
                throw new IllegalStateException('boom-after-output')
                """);
        EvaluationResult next = session.evaluateBridged("print 'next-only'; 3");

        assertFalse(failed.ok());
        assertEquals("before-failure-out", failed.stdout());
        assertEquals("before-failure-err", failed.stderr());
        assertTrue(failed.error().stack().contains("boom-after-output"));
        assertEquals("next-only", next.stdout());
        assertEquals("", next.stderr());
    }

    @Test
    void concurrentSessionsDoNotMixOutput() throws Exception {
        RuntimeSession alpha = sessions.getOrCreate("alpha");
        RuntimeSession beta = sessions.getOrCreate("beta");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<EvaluationResult> first = executor.submit(() -> alpha.evaluateBridged("""
                    20.times { print 'A'; Thread.sleep(1) }
                    'alpha'
                    """));
            Future<EvaluationResult> second = executor.submit(() -> beta.evaluateBridged("""
                    20.times { print 'B'; Thread.sleep(1) }
                    'beta'
                    """));

            EvaluationResult alphaResult = first.get(10, TimeUnit.SECONDS);
            EvaluationResult betaResult = second.get(10, TimeUnit.SECONDS);
            assertEquals("A".repeat(20), alphaResult.stdout());
            assertEquals("B".repeat(20), betaResult.stdout());
            assertFalse(alphaResult.stdout().contains("B"));
            assertFalse(betaResult.stdout().contains("A"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void scriptCreatedBackgroundThreadGoesToFallbackNotResponse() {
        EvaluationResult result = sessions.defaultSession().evaluateBridged("""
                def worker = new Thread({ System.out.print('background-log') } as Runnable)
                worker.start()
                worker.join()
                print 'foreground-output'
                true
                """);

        assertEquals("foreground-output", result.stdout());
        assertTrue(fallbackOutBytes.toString(StandardCharsets.UTF_8).contains("background-log"));
        assertFalse(result.stdout().contains("background-log"));
    }
}
