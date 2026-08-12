package mar.runtime.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mar.runtime.config.RuntimeConfig;
import mar.runtime.host.RuntimeHost;
import mar.runtime.state.RuntimeStateWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RpcIntegrationTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path projectRoot;

    private RuntimeHost host;

    @BeforeEach
    void setUp() {
        host = RuntimeHost.start(projectRoot, RuntimeConfig.defaults());
    }

    @AfterEach
    void tearDown() {
        host.close();
    }

    @Test
    void readyStateAdvertisesActuallyConnectableDynamicPort() throws IOException {
        JsonNode state = JSON.readTree(projectRoot.resolve(RuntimeStateWriter.STATE_RELATIVE_PATH).toFile());

        assertEquals("ready", state.get("status").textValue());
        assertEquals("127.0.0.1", state.get("host").textValue());
        assertEquals(host.rpcPort(), state.get("port").intValue());
        assertNotEquals(0, host.rpcPort());

        try (RpcTestClient client = new RpcTestClient(host.rpcHost(), host.rpcPort())) {
            JsonNode response = client.eval("state-smoke", null, "eval.raw", "1 + 2");
            assertTrue(response.get("ok").booleanValue());
            assertEquals(3, response.get("result").intValue());
        }
    }

    @Test
    void persistentConnectionSupportsOneHundredSequentialEvals() throws IOException {
        try (RpcTestClient client = new RpcTestClient(host.rpcHost(), host.rpcPort())) {
            for (int index = 0; index < 100; index++) {
                String id = "repeat-" + index;
                JsonNode response = client.eval(id, "repeat", "eval.raw", "1 + 2");
                assertEquals(id, response.get("id").textValue());
                assertTrue(response.get("ok").booleanValue());
                assertEquals(3, response.get("result").intValue());
            }
        }
    }

    @Test
    void reconnectKeepsNamedSessionAndSessionsStayIsolated() throws IOException {
        try (RpcTestClient first = new RpcTestClient(host.rpcHost(), host.rpcPort())) {
            assertEquals(41, first.eval("set-a", "alpha", "eval.raw", "vars.value = 41")
                    .get("result").intValue());
        }

        try (RpcTestClient second = new RpcTestClient(host.rpcHost(), host.rpcPort())) {
            assertEquals(42, second.eval("read-a", "alpha", "eval.raw", "vars.value + 1")
                    .get("result").intValue());
            assertEquals("beta", second.eval("set-b", "beta", "eval.raw", "vars.value = 'beta'")
                    .get("result").textValue());
            assertEquals(41, second.eval("still-a", "alpha", "eval.raw", "vars.value")
                    .get("result").intValue());
        }
    }

    @Test
    void invalidProtocolInputReturnsStructuredErrorsAndConnectionSurvives() throws IOException {
        try (RpcTestClient client = new RpcTestClient(host.rpcHost(), host.rpcPort())) {
            JsonNode invalidJson = client.sendLine("{not-json");
            assertTrue(invalidJson.get("id").isNull());
            assertEquals("INVALID_JSON", invalidJson.path("error").path("code").textValue());

            JsonNode blank = client.sendLine("   ");
            assertEquals("INVALID_JSON", blank.path("error").path("code").textValue());

            JsonNode trailingJson = client.sendLine(
                    "{\"id\":\"first\",\"method\":\"eval.raw\",\"code\":\"1\"} {}" );
            assertEquals("INVALID_JSON", trailingJson.path("error").path("code").textValue());

            JsonNode invalidRequest = client.sendLine(
                    "{\"id\":\"bad-request\",\"method\":\"eval.raw\"}");
            assertEquals("bad-request", invalidRequest.get("id").textValue());
            assertEquals("INVALID_REQUEST", invalidRequest.path("error").path("code").textValue());

            JsonNode invalidId = client.sendLine(
                    "{\"id\":1,\"method\":\"eval.raw\",\"code\":\"1\"}");
            assertTrue(invalidId.get("id").isNull());
            assertEquals("INVALID_REQUEST", invalidId.path("error").path("code").textValue());

            JsonNode blankSession = client.sendLine(
                    "{\"id\":\"blank-session\",\"session\":\"  \",\"method\":\"eval.raw\",\"code\":\"1\"}");
            assertEquals("blank-session", blankSession.get("id").textValue());
            assertEquals("INVALID_REQUEST", blankSession.path("error").path("code").textValue());

            JsonNode unknownMethod = client.eval("unknown", null, "inventory.list", "1");
            assertEquals("METHOD_NOT_FOUND", unknownMethod.path("error").path("code").textValue());

            JsonNode recovered = client.eval("recovered", null, "eval.raw", "6 * 7");
            assertTrue(recovered.get("ok").booleanValue());
            assertEquals(42, recovered.get("result").intValue());
        }
    }

    @Test
    void multilineCodeOutputExceptionAndNullResultFollowWireShape() throws IOException {
        try (RpcTestClient client = new RpcTestClient(host.rpcHost(), host.rpcPort())) {
            JsonNode multiline = client.eval("multiline", null, "eval.raw", """
                    print 'before-out'
                    System.err.print('before-err')
                    40 + 2
                    """);
            assertEquals(42, multiline.get("result").intValue());
            assertEquals("before-out", multiline.get("stdout").textValue());
            assertEquals("before-err", multiline.get("stderr").textValue());
            assertFalse(multiline.has("error"));

            JsonNode failed = client.eval("failed", null, "eval.raw", """
                    print 'exception-out'
                    throw new IllegalStateException('rpc-boom')
                    """);
            assertFalse(failed.get("ok").booleanValue());
            assertTrue(failed.get("result").isNull());
            assertEquals("exception-out", failed.get("stdout").textValue());
            assertEquals("EVAL_EXCEPTION", failed.path("error").path("code").textValue());
            assertTrue(failed.path("error").path("stack").textValue().contains("rpc-boom"));

            JsonNode invalidGroovy = client.eval("compile-failed", null, "eval.raw", "def = broken");
            assertFalse(invalidGroovy.get("ok").booleanValue());
            assertEquals("EVAL_EXCEPTION", invalidGroovy.path("error").path("code").textValue());
            assertTrue(invalidGroovy.path("error").path("stack").textValue().contains("MultipleCompilationErrorsException"));

            JsonNode unknownHandle = client.eval("unknown-handle", null, "eval.raw", "ref('@999')");
            assertFalse(unknownHandle.get("ok").booleanValue());
            assertEquals("EVAL_EXCEPTION", unknownHandle.path("error").path("code").textValue());
            assertTrue(unknownHandle.path("error").path("message").textValue().contains("Unknown handle"));

            JsonNode nullResult = client.eval("null", null, "eval.raw", "null");
            assertTrue(nullResult.has("result"));
            assertTrue(nullResult.get("result").isNull());
        }
    }

    @Test
    void clientAndServerMethodsAreRecognizedButUnavailableWithoutTargets() throws IOException {
        try (RpcTestClient client = new RpcTestClient(host.rpcHost(), host.rpcPort())) {
            for (String method : Set.of("eval.client", "eval.server")) {
                JsonNode response = client.eval(method, null, method, "Thread.currentThread().name");
                assertEquals("TARGET_UNAVAILABLE", response.path("error").path("code").textValue());
                assertFalse(response.get("ok").booleanValue());
            }
        }
    }

    @Test
    void separateConnectionsCanProgressConcurrently() throws Exception {
        CountDownLatch bothEntered = new CountDownLatch(2);
        host.sessions().getOrCreate("alpha").vars().setProperty("bothEntered", bothEntered);
        host.sessions().getOrCreate("beta").vars().setProperty("bothEntered", bothEntered);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try (RpcTestClient alpha = new RpcTestClient(host.rpcHost(), host.rpcPort());
             RpcTestClient beta = new RpcTestClient(host.rpcHost(), host.rpcPort())) {
            Future<JsonNode> first = executor.submit(() -> alpha.eval("a", "alpha", "eval.raw", """
                    vars.bothEntered.countDown()
                    assert vars.bothEntered.await(2, java.util.concurrent.TimeUnit.SECONDS)
                    'alpha'
                    """));
            Future<JsonNode> second = executor.submit(() -> beta.eval("b", "beta", "eval.raw", """
                    vars.bothEntered.countDown()
                    assert vars.bothEntered.await(2, java.util.concurrent.TimeUnit.SECONDS)
                    'beta'
                    """));

            assertEquals("alpha", first.get(10, TimeUnit.SECONDS).get("result").textValue());
            assertEquals("beta", second.get(10, TimeUnit.SECONDS).get("result").textValue());
        } finally {
            executor.shutdownNow();
        }
    }
}
