package mar.runtime.groovy;

import groovy.lang.Binding;
import mar.runtime.bootstrap.MinecraftAgentRuntime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class GroovyRuntimeTest {
    @Test
    void usesThreadContextClassLoaderAsParentAndSeesHarnessClass() {
        ClassLoader expectedParent = Thread.currentThread().getContextClassLoader();

        try (GroovyRuntime runtime = new GroovyRuntime()) {
            assertSame(expectedParent, runtime.parentClassLoader());
            Object result = runtime.createShell(new Binding()).evaluate(
                    "mar.runtime.fixture.HarnessVisible.VALUE");
            assertEquals("visible-from-parent", result);
        }
    }

    @Test
    void clientBindingIsAbsentWhenMinecraftClientClassIsUnavailable() {
        try (GroovyRuntime runtime = new GroovyRuntime()) {
            assertFalse(runtime.resolveMinecraftClient().isPresent());
        }
    }

    @Test
    void fallsBackToRuntimeClassLoaderWhenContextLoaderIsUnavailable() {
        Thread current = Thread.currentThread();
        ClassLoader previous = current.getContextClassLoader();
        current.setContextClassLoader(null);
        try (GroovyRuntime runtime = new GroovyRuntime()) {
            assertSame(MinecraftAgentRuntime.class.getClassLoader(), runtime.parentClassLoader());
            assertEquals(3, runtime.createShell(new Binding()).evaluate("1 + 2"));
        } finally {
            current.setContextClassLoader(previous);
        }
    }
}
