package mar.runtime.host;

import mar.runtime.config.RuntimeConfig;
import mar.runtime.groovy.GroovyRuntime;
import mar.runtime.io.EvalOutputCapture;
import mar.runtime.io.ThreadLocalPrintRouter;
import mar.runtime.rpc.RpcServer;
import mar.runtime.session.SessionManager;
import mar.runtime.state.RuntimeState;
import mar.runtime.state.RuntimeStateWriter;
import mar.runtime.thread.ClientExecutionTarget;
import mar.runtime.thread.IntegratedServerExecutionTarget;
import mar.runtime.thread.RawExecutionTarget;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RuntimeHost implements AutoCloseable {
    private static final int STATE_SCHEMA = 1;

    private final Path projectRoot;
    private final RuntimeConfig config;
    private final RuntimeStateWriter stateWriter;
    private final ThreadLocalPrintRouter outputRouter;
    private final GroovyRuntime groovyRuntime;
    private final SessionManager sessionManager;
    private final RpcServer rpcServer;
    private final Instant startedAt;
    private final String runtimeVersion;
    private final AtomicBoolean closed = new AtomicBoolean();

    private RuntimeHost(Path projectRoot, RuntimeConfig config, String runtimeVersion) {
        this.projectRoot = projectRoot;
        this.config = config;
        this.runtimeVersion = runtimeVersion;
        this.stateWriter = new RuntimeStateWriter(projectRoot);
        this.startedAt = Instant.now();
        this.outputRouter = ThreadLocalPrintRouter.install();
        GroovyRuntime createdGroovy = null;
        SessionManager createdSessions = null;
        RpcServer createdRpc = null;
        try {
            createdGroovy = new GroovyRuntime();
            createdSessions = new SessionManager(
                    projectRoot, createdGroovy, new EvalOutputCapture(outputRouter));
            createdSessions.defaultSession();
            RawExecutionTarget rawTarget = new RawExecutionTarget();
            ClientExecutionTarget clientTarget = new ClientExecutionTarget(createdGroovy.parentClassLoader());
            IntegratedServerExecutionTarget serverTarget = new IntegratedServerExecutionTarget(clientTarget);
            createdRpc = new RpcServer(
                    config.rpc().host(), config.rpc().port(), createdSessions,
                    rawTarget, clientTarget, serverTarget);
        } catch (Throwable error) {
            if (createdRpc != null) {
                createdRpc.close();
            }
            if (createdSessions != null) {
                createdSessions.close();
            }
            if (createdGroovy != null) {
                createdGroovy.close();
            }
            outputRouter.close();
            throw error;
        }
        this.groovyRuntime = createdGroovy;
        this.sessionManager = createdSessions;
        this.rpcServer = createdRpc;
    }

    public static RuntimeHost start(Path projectRoot, RuntimeConfig config) {
        RuntimeHost host = new RuntimeHost(
                Objects.requireNonNull(projectRoot, "projectRoot").toAbsolutePath().normalize(),
                Objects.requireNonNull(config, "config"),
                runtimeVersion());
        try {
            host.writeState("starting", config.rpc().port());
            host.rpcServer.start();
            host.writeState("ready", host.rpcServer.port());
            return host;
        } catch (RuntimeException error) {
            host.failStartup(error);
            throw error;
        }
    }

    public Path projectRoot() {
        return projectRoot;
    }

    public RuntimeConfig config() {
        return config;
    }

    public SessionManager sessions() {
        return sessionManager;
    }

    public String rpcHost() {
        return rpcServer.host();
    }

    public int rpcPort() {
        return rpcServer.port();
    }

    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            RuntimeException failure = null;
            int statePort = currentRpcPort();
            failure = runClosingStep(failure, () -> writeState("stopping", statePort));
            failure = runClosingStep(failure, rpcServer::close);
            failure = runClosingStep(failure, sessionManager::close);
            failure = runClosingStep(failure, groovyRuntime::close);
            failure = runClosingStep(failure, outputRouter::close);
            failure = runClosingStep(failure, () -> writeState("stopped", statePort));
            if (failure != null) {
                throw failure;
            }
        }
    }

    private void failStartup(RuntimeException startupError) {
        closed.set(true);
        RuntimeException failure = null;
        failure = runClosingStep(failure, rpcServer::close);
        failure = runClosingStep(failure, sessionManager::close);
        failure = runClosingStep(failure, groovyRuntime::close);
        failure = runClosingStep(failure, outputRouter::close);
        failure = runClosingStep(failure, () -> writeState("failed", config.rpc().port()));
        if (failure != null) {
            startupError.addSuppressed(failure);
        }
    }

    private int currentRpcPort() {
        try {
            return rpcServer.port();
        } catch (IllegalStateException ignored) {
            return config.rpc().port();
        }
    }

    private static RuntimeException runClosingStep(RuntimeException primary, Runnable step) {
        try {
            step.run();
        } catch (RuntimeException error) {
            if (primary == null) {
                return error;
            }
            primary.addSuppressed(error);
        }
        return primary;
    }

    private void writeState(String status, int port) {
        stateWriter.write(new RuntimeState(
                STATE_SCHEMA,
                status,
                runtimeVersion,
                ProcessHandle.current().pid(),
                config.rpc().host(),
                port,
                startedAt,
                projectRoot.toString(),
                "unknown"));
    }

    private static String runtimeVersion() {
        String implementationVersion = RuntimeHost.class.getPackage().getImplementationVersion();
        return implementationVersion == null || implementationVersion.isBlank()
                ? "development"
                : implementationVersion;
    }
}
