package mar.runtime.io;

import mar.runtime.result.EvaluationResult;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ThreadLocalPrintRouterTest {
    @Test
    void routesOnlyCurrentThreadAndRestoresFallbackAfterCapture() throws Exception {
        PrintStream previousOut = System.out;
        PrintStream previousErr = System.err;
        ByteArrayOutputStream fallbackOutBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream fallbackErrBytes = new ByteArrayOutputStream();
        PrintStream fallbackOut = new PrintStream(fallbackOutBytes, true, StandardCharsets.UTF_8);
        PrintStream fallbackErr = new PrintStream(fallbackErrBytes, true, StandardCharsets.UTF_8);

        ThreadLocalPrintRouter router = ThreadLocalPrintRouter.install(fallbackOut, fallbackErr);
        try {
            EvalOutputCapture capture = new EvalOutputCapture(router);
            EvaluationResult result = capture.capture(() -> {
                System.out.print("captured-out");
                System.err.print("captured-err");
                Thread unrelated = new Thread(() -> {
                    System.out.print("unrelated-out");
                    System.err.print("unrelated-err");
                }, "mar-unrelated-output-test");
                unrelated.start();
                try {
                    unrelated.join();
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(error);
                }
                return EvaluationResult.success(3);
            });

            assertEquals("captured-out", result.stdout());
            assertEquals("captured-err", result.stderr());
            assertEquals("unrelated-out", fallbackOutBytes.toString(StandardCharsets.UTF_8));
            assertEquals("unrelated-err", fallbackErrBytes.toString(StandardCharsets.UTF_8));

            System.out.print("after-context");
            assertEquals("unrelated-outafter-context", fallbackOutBytes.toString(StandardCharsets.UTF_8));
        } finally {
            router.close();
            assertSame(fallbackOut, System.out);
            assertSame(fallbackErr, System.err);
            System.setOut(previousOut);
            System.setErr(previousErr);
        }
    }

    @Test
    void thrownCaptureActionStillCleansThreadContext() {
        PrintStream previousOut = System.out;
        PrintStream previousErr = System.err;
        ByteArrayOutputStream fallbackBytes = new ByteArrayOutputStream();
        PrintStream fallback = new PrintStream(fallbackBytes, true, StandardCharsets.UTF_8);
        ThreadLocalPrintRouter router = ThreadLocalPrintRouter.install(fallback, fallback);
        try {
            EvalOutputCapture capture = new EvalOutputCapture(router);
            assertThrows(IllegalStateException.class, () -> capture.capture(() -> {
                System.out.print("discarded-with-failure");
                throw new IllegalStateException("capture-action-failed");
            }));

            EvaluationResult next = capture.capture(() -> {
                System.out.print("clean-next-context");
                return EvaluationResult.success(null);
            });
            assertEquals("clean-next-context", next.stdout());
        } finally {
            router.close();
            System.setOut(previousOut);
            System.setErr(previousErr);
        }
    }
}
