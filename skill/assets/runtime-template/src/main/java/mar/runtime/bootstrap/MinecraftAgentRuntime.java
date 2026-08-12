package mar.runtime.bootstrap;

import mar.runtime.config.ProjectRootResolver;
import mar.runtime.config.RuntimeConfig;
import mar.runtime.host.RuntimeHost;

import java.nio.file.Path;

public final class MinecraftAgentRuntime {
    private static final Object LIFECYCLE_LOCK = new Object();

    private static volatile RuntimeHost host;
    private static volatile Throwable startupFailure;
    private static volatile boolean starting;
    private static volatile boolean shutdownHookInstalled;

    private MinecraftAgentRuntime() {
    }

    public static RuntimeHost start() {
        synchronized (LIFECYCLE_LOCK) {
            while (starting) {
                try {
                    LIFECYCLE_LOCK.wait();
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for MAR Runtime startup", error);
                }
            }
            if (host != null && !host.isClosed()) {
                return host;
            }
            if (startupFailure != null) {
                throw new IllegalStateException(
                        "MAR Runtime startup previously failed; call retryStart() for an explicit retry",
                        startupFailure);
            }
            starting = true;
        }

        RuntimeHost created = null;
        try {
            Path projectRoot = ProjectRootResolver.resolve();
            RuntimeConfig config = RuntimeConfig.load(projectRoot);
            created = RuntimeHost.start(projectRoot, config);
            installShutdownHook();
            synchronized (LIFECYCLE_LOCK) {
                host = created;
                return created;
            }
        } catch (Throwable error) {
            if (created != null) {
                created.close();
            }
            synchronized (LIFECYCLE_LOCK) {
                startupFailure = error;
            }
            throw error;
        } finally {
            synchronized (LIFECYCLE_LOCK) {
                starting = false;
                LIFECYCLE_LOCK.notifyAll();
            }
        }
    }

    public static RuntimeHost retryStart() {
        synchronized (LIFECYCLE_LOCK) {
            if (starting) {
                throw new IllegalStateException("Cannot retry MAR Runtime while startup is in progress");
            }
            if (host != null && !host.isClosed()) {
                return host;
            }
            startupFailure = null;
        }
        return start();
    }

    private static void installShutdownHook() {
        synchronized (LIFECYCLE_LOCK) {
            if (shutdownHookInstalled) {
                return;
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                RuntimeHost current = host;
                if (current != null) {
                    current.close();
                }
            }, "mar-runtime-shutdown"));
            shutdownHookInstalled = true;
        }
    }
}
