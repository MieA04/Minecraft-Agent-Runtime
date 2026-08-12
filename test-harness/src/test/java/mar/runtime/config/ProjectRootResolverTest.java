package mar.runtime.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectRootResolverTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void explicitPropertyRootTakesPriority() throws IOException {
        Path propertyRoot = createProject("property-root");
        Path searchRoot = createProject("search-root");
        Path nested = Files.createDirectories(searchRoot.resolve("a/b"));

        assertEquals(propertyRoot.toAbsolutePath().normalize(),
                ProjectRootResolver.resolve(propertyRoot.toString(), nested.toString()));
    }

    @Test
    void searchesParentsFromUserDirectory() throws IOException {
        Path root = createProject("parent-root");
        Path nested = Files.createDirectories(root.resolve("build/run/client"));

        assertEquals(root.toAbsolutePath().normalize(),
                ProjectRootResolver.resolve(null, nested.toString()));
    }

    @Test
    void missingWorkspaceFailsWithoutCreatingOne() throws IOException {
        Path root = Files.createDirectory(temporaryDirectory.resolve("missing-workspace"));
        ProjectRootResolutionException error = assertThrows(
                ProjectRootResolutionException.class,
                () -> ProjectRootResolver.resolve(root.toString(), temporaryDirectory.toString()));

        assertTrue(error.getMessage().contains(ProjectRootResolver.WORKSPACE_DIRECTORY));
        assertTrue(Files.notExists(root.resolve(ProjectRootResolver.WORKSPACE_DIRECTORY)));
    }

    @Test
    void nonexistentExplicitRootFailsWithoutCreatingIt() {
        Path root = temporaryDirectory.resolve("missing-root");

        assertThrows(ProjectRootResolutionException.class,
                () -> ProjectRootResolver.resolve(root.toString(), temporaryDirectory.toString()));
        assertTrue(Files.notExists(root));
    }

    private Path createProject(String name) throws IOException {
        Path root = Files.createDirectories(temporaryDirectory.resolve(name));
        Files.createDirectory(root.resolve(ProjectRootResolver.WORKSPACE_DIRECTORY));
        return root;
    }
}
