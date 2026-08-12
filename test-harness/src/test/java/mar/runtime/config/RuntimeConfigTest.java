package mar.runtime.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeConfigTest {
    @TempDir
    Path projectRoot;

    @Test
    void missingConfigUsesFrozenDefaults() {
        assertEquals(RuntimeConfig.defaults(), RuntimeConfig.load(projectRoot));
    }

    @Test
    void loadsSchemaOneLoopbackConfig() throws IOException {
        writeConfig("""
                {"schema":1,"rpc":{"host":"localhost","port":41234}}
                """);

        RuntimeConfig config = RuntimeConfig.load(projectRoot);

        assertEquals(1, config.schema());
        assertEquals("localhost", config.rpc().host());
        assertEquals(41234, config.rpc().port());
    }

    @Test
    void unsupportedSchemaFailsClearly() throws IOException {
        writeConfig("""
                {"schema":2,"rpc":{"host":"127.0.0.1","port":0}}
                """);

        RuntimeConfigException error = assertThrows(RuntimeConfigException.class,
                () -> RuntimeConfig.load(projectRoot));
        assertTrue(error.getMessage().contains("Unsupported Runtime config schema: 2"));
    }

    @Test
    void nonLoopbackHostIsRejected() throws IOException {
        writeConfig("""
                {"schema":1,"rpc":{"host":"192.0.2.1","port":0}}
                """);

        RuntimeConfigException error = assertThrows(RuntimeConfigException.class,
                () -> RuntimeConfig.load(projectRoot));
        assertTrue(error.getMessage().contains("loopback"));
    }

    @Test
    void malformedJsonFailsClearly() throws IOException {
        writeConfig("{not-json");

        RuntimeConfigException error = assertThrows(RuntimeConfigException.class,
                () -> RuntimeConfig.load(projectRoot));
        assertTrue(error.getMessage().contains("Cannot read Runtime config"));
    }

    private void writeConfig(String json) throws IOException {
        Path config = projectRoot.resolve(RuntimeConfig.CONFIG_RELATIVE_PATH);
        Files.createDirectories(config.getParent());
        Files.writeString(config, json);
    }
}
