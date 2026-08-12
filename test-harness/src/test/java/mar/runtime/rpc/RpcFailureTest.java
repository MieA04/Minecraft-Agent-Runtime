package mar.runtime.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mar.runtime.config.RuntimeConfig;
import mar.runtime.host.RuntimeHost;
import mar.runtime.state.RuntimeStateWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RpcFailureTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path projectRoot;

    @Test
    void portBindFailureDoesNotPublishReadyAndRestoresSystemStreams() throws IOException {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        try (ServerSocket occupied = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            RuntimeConfig config = new RuntimeConfig(
                    1, new RuntimeConfig.Rpc("127.0.0.1", occupied.getLocalPort()));

            assertThrows(IllegalStateException.class, () -> RuntimeHost.start(projectRoot, config));

            JsonNode state = JSON.readTree(projectRoot.resolve(RuntimeStateWriter.STATE_RELATIVE_PATH).toFile());
            assertEquals("failed", state.get("status").textValue());
            assertEquals(occupied.getLocalPort(), state.get("port").intValue());
            assertSame(originalOut, System.out);
            assertSame(originalErr, System.err);
        }
    }

    @Test
    void productionDispatcherSurfaceContainsExactlyThreeEvalMethods() {
        assertEquals(Set.of("eval.raw", "eval.client", "eval.server"), RpcDispatcher.METHODS);
    }
}
