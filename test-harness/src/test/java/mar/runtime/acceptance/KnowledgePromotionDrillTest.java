package mar.runtime.acceptance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgePromotionDrillTest {
    private static final Pattern PID = Pattern.compile("PROMOTION_OK pid=(\\d+)");

    @TempDir
    Path temporaryRoot;

    @Test
    void projectDiscoveryToolAndPublicPromotionPassInIndependentProcesses() throws Exception {
        Path repository = Path.of(System.getProperty("mar.repositoryRoot"))
                .toAbsolutePath().normalize();
        Path discovery = repository.resolve(
                "test-harness/fixtures/promotion-drill/project-a/knowledge/discoveries/"
                        + "20260812-001-class-visibility.md");
        Path projectTool = repository.resolve(
                "test-harness/fixtures/promotion-drill/project-a/tools/loader/class-visibility.groovy");
        Path publicTool = repository.resolve("skill/tools/loader/class-visibility.groovy");
        Path publicReference = repository.resolve("skill/references/public-loader-tools.md");

        assertTrue(Files.readString(discovery).contains("Status: verified"));
        assertEquivalentToolBodies(projectTool, publicTool);
        String publicSource = Files.readString(publicTool).toLowerCase();
        assertTrue(publicSource.contains("project dependencies: none"));
        assertTrue(!publicSource.contains("elementalrunes") && !publicSource.contains("org.miea"));
        assertTrue(Files.readString(publicReference).contains("independent clean runtime fixture process"));

        ProbeResult first = runProbe(temporaryRoot.resolve("environment-a"), projectTool);
        ProbeResult second = runProbe(temporaryRoot.resolve("environment-b"), publicTool);

        assertNotEquals(first.pid(), second.pid());
        assertTrue(first.output().contains("PROMOTION_OK"));
        assertTrue(second.output().contains("PROMOTION_OK"));
    }

    private static ProbeResult runProbe(Path projectRoot, Path tool) throws Exception {
        Files.createDirectories(projectRoot);
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java")
                .toString();
        String classpath = System.getProperty(
                "surefire.test.class.path", System.getProperty("java.class.path"));
        Process process = new ProcessBuilder(
                javaExecutable,
                "-cp", classpath,
                PromotionProbeMain.class.getName(),
                projectRoot.toString(),
                tool.toString())
                .redirectErrorStream(true)
                .start();
        boolean exited = process.waitFor(30, TimeUnit.SECONDS);
        if (!exited) {
            process.destroyForcibly();
            throw new IllegalStateException("Promotion probe timed out");
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), output);
        Matcher matcher = PID.matcher(output);
        assertTrue(matcher.find(), output);
        return new ProbeResult(Long.parseLong(matcher.group(1)), output);
    }

    private static void assertEquivalentToolBodies(Path projectTool, Path publicTool) throws IOException {
        String projectBody = Files.readString(projectTool);
        String publicBody = Files.readString(publicTool);
        projectBody = projectBody.substring(projectBody.indexOf("return ["));
        publicBody = publicBody.substring(publicBody.indexOf("return ["));
        assertEquals(projectBody, publicBody);
    }

    private record ProbeResult(long pid, String output) {
    }
}
