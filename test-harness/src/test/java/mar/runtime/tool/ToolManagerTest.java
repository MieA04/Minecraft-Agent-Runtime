package mar.runtime.tool;

import mar.runtime.groovy.GroovyRuntime;
import mar.runtime.io.EvalOutputCapture;
import mar.runtime.io.ThreadLocalPrintRouter;
import mar.runtime.session.RuntimeSession;
import mar.runtime.session.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolManagerTest {
    @TempDir
    Path projectRoot;

    private ThreadLocalPrintRouter router;
    private GroovyRuntime groovyRuntime;
    private SessionManager sessions;
    private RuntimeSession session;

    @BeforeEach
    void setUp() {
        router = ThreadLocalPrintRouter.install();
        groovyRuntime = new GroovyRuntime();
        sessions = new SessionManager(projectRoot, groovyRuntime, new EvalOutputCapture(router));
        session = sessions.defaultSession();
    }

    @AfterEach
    void tearDown() {
        sessions.close();
        groovyRuntime.close();
        router.close();
    }

    @Test
    void hotReloadReplacesOnePathWithoutRestartingSession() throws IOException {
        writeTool("minecraft/inventory.groovy", "return [version: { -> 'v1' }]");

        Map<?, ?> first = reloadPath("minecraft/inventory.groovy");
        assertTrue((Boolean) first.get("ok"));
        assertEquals("vars.tools.minecraft.inventory", first.get("namespace"));
        assertEquals("v1", session.evaluate("vars.tools.minecraft.inventory.version()"));

        writeTool("minecraft/inventory.groovy", "return [version: { -> 'v2' }]");
        Map<?, ?> second = reloadPath("minecraft/inventory.groovy");

        assertTrue((Boolean) second.get("ok"));
        assertEquals("v2", session.evaluate("vars.tools.minecraft.inventory.version()"));
    }

    @Test
    void syntaxErrorReturnsCompileDetailsAndRetainsPreviousTool() throws IOException {
        writeTool("minecraft/inventory.groovy", "return [version: { -> 'v1' }]");
        reloadPath("minecraft/inventory.groovy");
        writeTool("minecraft/inventory.groovy", "return [broken:");

        Map<?, ?> failure = reloadPath("minecraft/inventory.groovy");

        assertFalse((Boolean) failure.get("ok"));
        assertEquals("compile", failure.get("phase"));
        assertEquals("minecraft/inventory.groovy", failure.get("path"));
        assertTrue(((String) failure.get("exceptionType")).contains("Compilation"));
        assertFalse(((String) failure.get("stack")).isBlank());
        assertEquals("v1", session.evaluate("vars.tools.minecraft.inventory.version()"));
    }

    @Test
    void stableReloadIsDeterministicAndIgnoresExperimental() throws IOException {
        writeTool("mod/zeta.groovy", "return [name: 'zeta']");
        writeTool("loader/alpha.groovy", "return [name: 'alpha']");
        writeTool("minecraft/nested/beta.groovy", "return [name: 'beta']");
        writeTool("experimental/unsafe.groovy", "return [loaded: true]");

        List<?> results = (List<?>) session.evaluate("runtime.tools.reloadAllStable()");

        assertEquals(List.of(
                "loader/alpha.groovy",
                "minecraft/nested/beta.groovy",
                "mod/zeta.groovy"), results.stream()
                .map(result -> ((Map<?, ?>) result).get("path"))
                .toList());
        assertEquals("alpha", session.evaluate("vars.tools.loader.alpha.name"));
        assertEquals("beta", session.evaluate("vars.tools.minecraft.nested.beta.name"));
        assertEquals("zeta", session.evaluate("vars.tools.mod.zeta.name"));
        assertEquals(false, session.evaluate(
                "vars.tools.properties.containsKey('experimental')"));
    }

    @Test
    void stableReloadRemovesDeletedNamespaceAndPrunesEmptyParents() throws IOException {
        Path tool = writeTool("minecraft/nested/temporary.groovy", "return [value: 7]");
        session.evaluate("runtime.tools.reloadAllStable()");
        Files.delete(tool);

        List<?> results = (List<?>) session.evaluate("runtime.tools.reloadAllStable()");

        Map<?, ?> removal = (Map<?, ?>) results.get(0);
        assertEquals(true, removal.get("ok"));
        assertEquals(true, removal.get("removed"));
        assertEquals("minecraft/nested/temporary.groovy", removal.get("path"));
        assertEquals(false, session.evaluate(
                "vars.tools.properties.containsKey('minecraft')"));
    }

    @Test
    void explicitExperimentalLoadIsAllowedButRemainsOutsideStableReload() throws IOException {
        writeTool("experimental/probe.groovy", "return [value: 11]");

        Map<?, ?> result = reloadPath("experimental/probe.groovy");

        assertTrue((Boolean) result.get("ok"));
        assertEquals(11, session.evaluate("vars.tools.experimental.probe.value"));
        assertTrue(((List<?>) session.evaluate("runtime.tools.reloadAllStable()")).isEmpty());
        assertEquals(11, session.evaluate("vars.tools.experimental.probe.value"));
    }

    @Test
    void invalidPathAndNullReturnAreStructuredFailures() throws IOException {
        Map<?, ?> invalid = reloadPath("../outside.groovy");
        writeTool("mod/null-tool.groovy", "return null");
        Map<?, ?> nullTool = reloadPath("mod/null-tool.groovy");

        assertEquals(false, invalid.get("ok"));
        assertEquals("validate", invalid.get("phase"));
        assertEquals(false, nullTool.get("ok"));
        assertEquals("init", nullTool.get("phase"));
        assertTrue(((String) nullTool.get("message")).contains("non-null"));
    }

    @Test
    void toolNamespacesRemainSessionScoped() throws IOException {
        writeTool("mod/session.groovy", "return [value: 3]");
        reloadPath("mod/session.groovy");
        RuntimeSession other = sessions.getOrCreate("other");

        assertEquals(3, session.evaluate("vars.tools.mod.session.value"));
        assertEquals(false, other.evaluate("vars.tools.properties.containsKey('mod')"));
    }

    private Map<?, ?> reloadPath(String path) {
        return (Map<?, ?>) session.evaluate(
                "runtime.tools.reloadPath('" + path.replace("'", "\\'") + "')");
    }

    private Path writeTool(String relativePath, String source) throws IOException {
        Path file = projectRoot.resolve(".minecraft-agent-runtime/tools").resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, source);
        return file;
    }
}
