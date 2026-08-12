package mar.runtime.acceptance;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureBoundaryTest {
    private static final Pattern BUSINESS_API = Pattern.compile(
            "\\b(inventory|crafting|attack|mining|pathfinding)\\b|ui\\.click|screen\\.findbutton");

    @Test
    void productionRuntimeContainsNoHighLevelMinecraftBusinessApi() throws IOException {
        Path sourceRoot = repositoryRoot().resolve(
                "skill/assets/runtime-template/src/main/java/mar/runtime");
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            List<Path> violations = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> containsBusinessApi(read(path)))
                    .toList();
            assertTrue(violations.isEmpty(), "Runtime business API violations: " + violations);
        }
    }

    @Test
    void bootstrapRemainsThinOrchestration() throws IOException {
        String source = read(repositoryRoot().resolve(
                "skill/assets/runtime-template/src/main/java/mar/runtime/bootstrap/MinecraftAgentRuntime.java"));
        String lower = source.toLowerCase(Locale.ROOT);

        assertFalse(containsBusinessApi(source));
        assertFalse(lower.contains("class.forname"));
        assertFalse(lower.contains("mar.runtime.tool"));
        assertFalse(lower.contains("net.minecraft."));
        assertTrue(source.contains("ProjectRootResolver.resolve()"));
        assertTrue(source.contains("RuntimeConfig.load(projectRoot)"));
        assertTrue(source.contains("RuntimeHost.start(projectRoot, config)"));
    }

    @Test
    void installerHasNoDestructiveWorkspaceOperation() throws IOException {
        String installer = read(repositoryRoot().resolve("skill/scripts/install_mar.py"));
        assertFalse(installer.contains("shutil.rmtree"));
        assertFalse(installer.contains("os.remove("));
        assertTrue(installer.contains("copy_missing_tree"));
        assertTrue(installer.contains("elif not target.exists():"));
    }

    private static boolean containsBusinessApi(String source) {
        return BUSINESS_API.matcher(source.toLowerCase(Locale.ROOT)).find();
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read acceptance source " + path, error);
        }
    }

    private static Path repositoryRoot() {
        return Path.of(System.getProperty("mar.repositoryRoot")).toAbsolutePath().normalize();
    }
}
