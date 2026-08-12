package mar.runtime.host;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mar.runtime.config.RuntimeConfig;
import mar.runtime.state.RuntimeStateWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeHostTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path projectRoot;

    @Test
    void lifecyclePublishesReadyOnlyAfterRpcStartsThenStopsCleanly() throws IOException {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        RuntimeHost host = RuntimeHost.start(projectRoot, RuntimeConfig.defaults());

        assertFalse(host.isClosed());
        assertEquals("ready", currentStatus());
        assertTrue(host.rpcPort() > 0);
        assertEquals(host.rpcPort(), currentState().get("port").intValue());
        assertEquals(3, host.sessions().defaultSession().evaluate("1 + 2"));

        host.close();
        host.close();

        assertTrue(host.isClosed());
        assertEquals("stopped", currentStatus());
        assertEquals(originalOut, System.out);
        assertEquals(originalErr, System.err);
    }

    private String currentStatus() throws IOException {
        return currentState().get("status").textValue();
    }

    private JsonNode currentState() throws IOException {
        return JSON.readTree(projectRoot.resolve(RuntimeStateWriter.STATE_RELATIVE_PATH).toFile());
    }
}
