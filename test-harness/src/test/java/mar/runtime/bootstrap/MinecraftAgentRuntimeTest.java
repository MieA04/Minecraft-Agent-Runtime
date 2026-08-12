package mar.runtime.bootstrap;

import mar.runtime.config.ProjectRootResolver;
import mar.runtime.host.RuntimeHost;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertSame;

class MinecraftAgentRuntimeTest {
    @TempDir
    Path projectRoot;

    @Test
    void repeatedStartReturnsTheExistingHost() throws IOException {
        Files.createDirectory(projectRoot.resolve(ProjectRootResolver.WORKSPACE_DIRECTORY));
        String previous = System.getProperty(ProjectRootResolver.PROJECT_ROOT_PROPERTY);
        System.setProperty(ProjectRootResolver.PROJECT_ROOT_PROPERTY, projectRoot.toString());
        RuntimeHost first = null;
        try {
            first = MinecraftAgentRuntime.start();
            RuntimeHost second = MinecraftAgentRuntime.start();
            assertSame(first, second);
        } finally {
            if (first != null) {
                first.close();
            }
            if (previous == null) {
                System.clearProperty(ProjectRootResolver.PROJECT_ROOT_PROPERTY);
            } else {
                System.setProperty(ProjectRootResolver.PROJECT_ROOT_PROPERTY, previous);
            }
        }
    }
}
