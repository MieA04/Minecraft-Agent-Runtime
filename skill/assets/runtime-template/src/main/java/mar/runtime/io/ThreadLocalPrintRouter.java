package mar.runtime.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ThreadLocalPrintRouter implements AutoCloseable {
    private final PrintStream originalOut;
    private final PrintStream originalErr;
    private final ThreadLocal<ByteArrayOutputStream> stdoutTarget = new ThreadLocal<>();
    private final ThreadLocal<ByteArrayOutputStream> stderrTarget = new ThreadLocal<>();
    private final PrintStream routedOut;
    private final PrintStream routedErr;
    private final AtomicBoolean closed = new AtomicBoolean();

    private ThreadLocalPrintRouter(PrintStream originalOut, PrintStream originalErr) {
        this.originalOut = Objects.requireNonNull(originalOut, "originalOut");
        this.originalErr = Objects.requireNonNull(originalErr, "originalErr");
        this.routedOut = new PrintStream(
                new RoutingOutputStream(originalOut, stdoutTarget), true, StandardCharsets.UTF_8);
        this.routedErr = new PrintStream(
                new RoutingOutputStream(originalErr, stderrTarget), true, StandardCharsets.UTF_8);
    }

    public static ThreadLocalPrintRouter install() {
        ThreadLocalPrintRouter router = new ThreadLocalPrintRouter(System.out, System.err);
        System.setOut(router.routedOut);
        System.setErr(router.routedErr);
        return router;
    }

    static ThreadLocalPrintRouter install(PrintStream fallbackOut, PrintStream fallbackErr) {
        ThreadLocalPrintRouter router = new ThreadLocalPrintRouter(fallbackOut, fallbackErr);
        System.setOut(router.routedOut);
        System.setErr(router.routedErr);
        return router;
    }

    PrintWriter groovyOut() {
        return new PrintWriter(routedOut, true);
    }

    CaptureContext openCapture() {
        if (closed.get()) {
            throw new IllegalStateException("Thread-local print router is closed");
        }
        if (stdoutTarget.get() != null || stderrTarget.get() != null) {
            throw new IllegalStateException("Nested eval output capture is not supported on the same thread");
        }
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        stdoutTarget.set(stdout);
        stderrTarget.set(stderr);
        return new CaptureContext(stdout, stderr);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            stdoutTarget.remove();
            stderrTarget.remove();
            if (System.out == routedOut) {
                System.setOut(originalOut);
            }
            if (System.err == routedErr) {
                System.setErr(originalErr);
            }
        }
    }

    final class CaptureContext implements AutoCloseable {
        private final ByteArrayOutputStream stdout;
        private final ByteArrayOutputStream stderr;
        private boolean contextClosed;

        private CaptureContext(ByteArrayOutputStream stdout, ByteArrayOutputStream stderr) {
            this.stdout = stdout;
            this.stderr = stderr;
        }

        String stdout() {
            return stdout.toString(StandardCharsets.UTF_8);
        }

        String stderr() {
            return stderr.toString(StandardCharsets.UTF_8);
        }

        @Override
        public void close() {
            if (!contextClosed) {
                if (stdoutTarget.get() == stdout) {
                    stdoutTarget.remove();
                }
                if (stderrTarget.get() == stderr) {
                    stderrTarget.remove();
                }
                contextClosed = true;
            }
        }
    }

    private static final class RoutingOutputStream extends OutputStream {
        private final PrintStream fallback;
        private final ThreadLocal<ByteArrayOutputStream> target;

        private RoutingOutputStream(PrintStream fallback, ThreadLocal<ByteArrayOutputStream> target) {
            this.fallback = fallback;
            this.target = target;
        }

        @Override
        public void write(int value) {
            ByteArrayOutputStream current = target.get();
            if (current == null) {
                fallback.write(value);
            } else {
                current.write(value);
            }
        }

        @Override
        public void write(byte[] values, int offset, int length) {
            ByteArrayOutputStream current = target.get();
            if (current == null) {
                fallback.write(values, offset, length);
            } else {
                current.write(values, offset, length);
            }
        }

        @Override
        public void flush() throws IOException {
            if (target.get() == null) {
                fallback.flush();
            }
        }
    }
}
