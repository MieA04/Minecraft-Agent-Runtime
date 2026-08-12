package mar.runtime.groovy;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import mar.runtime.bootstrap.MinecraftAgentRuntime;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public final class GroovyRuntime implements AutoCloseable {
    private final ClassLoader parentClassLoader;
    private final GroovyClassLoader groovyClassLoader;
    private final MinecraftClientResolver minecraftClientResolver;

    public GroovyRuntime() {
        this(resolveParentClassLoader(), new MinecraftClientResolver());
    }

    GroovyRuntime(ClassLoader parentClassLoader, MinecraftClientResolver minecraftClientResolver) {
        this.parentClassLoader = Objects.requireNonNull(parentClassLoader, "parentClassLoader");
        this.groovyClassLoader = new GroovyClassLoader(parentClassLoader);
        this.minecraftClientResolver = Objects.requireNonNull(minecraftClientResolver, "minecraftClientResolver");
    }

    public ClassLoader parentClassLoader() {
        return parentClassLoader;
    }

    public GroovyShell createShell(Binding binding) {
        return new GroovyShell(groovyClassLoader, Objects.requireNonNull(binding, "binding"));
    }

    public Optional<Object> resolveMinecraftClient() {
        return minecraftClientResolver.resolve(parentClassLoader);
    }

    @Override
    public void close() {
        try {
            groovyClassLoader.close();
        } catch (IOException error) {
            throw new IllegalStateException("Cannot close MAR Groovy ClassLoader", error);
        }
    }

    private static ClassLoader resolveParentClassLoader() {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        if (context != null) {
            return context;
        }
        ClassLoader anchor = MinecraftAgentRuntime.class.getClassLoader();
        if (anchor != null) {
            return anchor;
        }
        throw new IllegalStateException(
                "Cannot initialize MAR Groovy Runtime: no context or Runtime ClassLoader is available");
    }
}
