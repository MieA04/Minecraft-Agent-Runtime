package mar.runtime.thread;

import mar.runtime.groovy.GroovyRuntime;
import mar.runtime.io.EvalOutputCapture;
import mar.runtime.io.ThreadLocalPrintRouter;
import mar.runtime.result.EvaluationResult;
import mar.runtime.session.RuntimeSession;
import mar.runtime.session.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionTargetTest {
    @TempDir
    Path projectRoot;

    private ThreadLocalPrintRouter router;
    private GroovyRuntime groovyRuntime;
    private SessionManager sessions;

    @BeforeEach
    void setUp() {
        router = ThreadLocalPrintRouter.install();
        groovyRuntime = new GroovyRuntime();
        sessions = new SessionManager(projectRoot, groovyRuntime, new EvalOutputCapture(router));
    }

    @AfterEach
    void tearDown() {
        sessions.close();
        groovyRuntime.close();
        router.close();
    }

    @Test
    void schedulesEvaluationOnTargetThreadAndCapturesThere() {
        RuntimeSession session = sessions.defaultSession();
        try (FakeTarget target = new FakeTarget("phase6-fake-target")) {
            EvaluationResult result = ReflectiveExecutionSupport.executeOn(
                    target,
                    session,
                    "print Thread.currentThread().name; Thread.currentThread().name",
                    "Fake Target");

            assertTrue(result.ok());
            assertEquals("phase6-fake-target", result.result());
            assertEquals("phase6-fake-target", result.stdout());
        }
    }

    @Test
    void currentTargetThreadExecutesDirectlyWithoutSelfDeadlock() throws Exception {
        RuntimeSession session = sessions.defaultSession();
        try (FakeTarget target = new FakeTarget("phase6-self-target")) {
            Future<EvaluationResult> future = target.submit(() -> ReflectiveExecutionSupport.executeOn(
                    target, session, "Thread.currentThread().name", "Fake Target"));

            EvaluationResult result = future.get(5, TimeUnit.SECONDS);
            assertEquals("phase6-self-target", result.result());
        }
    }

    @Test
    void missingThreadIdentityMethodIsUnavailableInsteadOfUnsafeFallback() {
        Executor unsafeTarget = Runnable::run;

        assertThrows(TargetUnavailableException.class, () -> ReflectiveExecutionSupport.executeOn(
                unsafeTarget, sessions.defaultSession(), "1 + 2", "Unsafe Target"));
    }

    @Test
    void absentMinecraftClassesMakeBothTargetsUnavailable() {
        ClientExecutionTarget client = new ClientExecutionTarget(groovyRuntime.parentClassLoader());
        IntegratedServerExecutionTarget server = new IntegratedServerExecutionTarget(client);

        assertThrows(TargetUnavailableException.class,
                () -> client.execute(sessions.defaultSession(), "1 + 2"));
        assertThrows(TargetUnavailableException.class,
                () -> server.execute(sessions.defaultSession(), "1 + 2"));
    }

    @Test
    void creatingSessionDoesNotEagerlyResolveClientOnlyClasses() {
        RuntimeSession session = sessions.defaultSession();

        assertFalse(session.binding().hasVariable("mc"));
    }

    public static final class FakeTarget implements Executor, AutoCloseable {
        private final AtomicReference<Thread> targetThread = new AtomicReference<>();
        private final ExecutorService executor;

        FakeTarget(String threadName) {
            executor = Executors.newSingleThreadExecutor(task -> new Thread(() -> {
                targetThread.set(Thread.currentThread());
                task.run();
            }, threadName));
        }

        public boolean isSameThread() {
            return Thread.currentThread() == targetThread.get();
        }

        @Override
        public void execute(Runnable command) {
            executor.execute(command);
        }

        <T> Future<T> submit(java.util.concurrent.Callable<T> operation) {
            return executor.submit(operation);
        }

        @Override
        public void close() {
            executor.shutdownNow();
        }
    }
}
