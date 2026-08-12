package mar.runtime.session;

import mar.runtime.groovy.GroovyRuntime;
import mar.runtime.io.EvalOutputCapture;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class SessionManager implements AutoCloseable {
    public static final String DEFAULT_SESSION = "default";

    private final Path projectRoot;
    private final GroovyRuntime groovyRuntime;
    private final EvalOutputCapture outputCapture;
    private final ConcurrentMap<String, RuntimeSession> sessions = new ConcurrentHashMap<>();

    public SessionManager(Path projectRoot, GroovyRuntime groovyRuntime) {
        this(projectRoot, groovyRuntime, EvalOutputCapture.disabled());
    }

    public SessionManager(
            Path projectRoot,
            GroovyRuntime groovyRuntime,
            EvalOutputCapture outputCapture) {
        this.projectRoot = Objects.requireNonNull(projectRoot, "projectRoot");
        this.groovyRuntime = Objects.requireNonNull(groovyRuntime, "groovyRuntime");
        this.outputCapture = Objects.requireNonNull(outputCapture, "outputCapture");
    }

    public RuntimeSession defaultSession() {
        return getOrCreate(DEFAULT_SESSION);
    }

    public RuntimeSession getOrCreate(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Session name must not be blank");
        }
        return sessions.computeIfAbsent(
                name, key -> new RuntimeSession(key, projectRoot, groovyRuntime, outputCapture));
    }

    public Collection<RuntimeSession> sessions() {
        return List.copyOf(sessions.values());
    }

    @Override
    public void close() {
        sessions.clear();
    }
}
