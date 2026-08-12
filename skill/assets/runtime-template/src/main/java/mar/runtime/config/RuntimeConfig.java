package mar.runtime.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;

public record RuntimeConfig(int schema, Rpc rpc) {
    public static final int SUPPORTED_SCHEMA = 1;
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 0;
    public static final Path CONFIG_RELATIVE_PATH = Path.of(
            ProjectRootResolver.WORKSPACE_DIRECTORY, "runtime", "config", "runtime.json");

    private static final ObjectMapper JSON = new ObjectMapper();

    public RuntimeConfig {
        if (schema != SUPPORTED_SCHEMA) {
            throw new RuntimeConfigException("Unsupported Runtime config schema: " + schema);
        }
        if (rpc == null) {
            throw new RuntimeConfigException("Runtime config field rpc is required");
        }
        validateHost(rpc.host());
        if (rpc.port() < 0 || rpc.port() > 65535) {
            throw new RuntimeConfigException("Runtime RPC port must be between 0 and 65535: " + rpc.port());
        }
    }

    public static RuntimeConfig defaults() {
        return new RuntimeConfig(SUPPORTED_SCHEMA, new Rpc(DEFAULT_HOST, DEFAULT_PORT));
    }

    public static RuntimeConfig load(Path projectRoot) {
        Path path = projectRoot.resolve(CONFIG_RELATIVE_PATH);
        if (!Files.exists(path)) {
            return defaults();
        }
        if (!Files.isRegularFile(path)) {
            throw new RuntimeConfigException("Runtime config is not a regular file: " + path);
        }

        try {
            JsonNode root = JSON.readTree(path.toFile());
            if (root == null || !root.isObject()) {
                throw new RuntimeConfigException("Runtime config root must be a JSON object: " + path);
            }
            JsonNode schemaNode = root.get("schema");
            JsonNode rpcNode = root.get("rpc");
            if (schemaNode == null || !schemaNode.isIntegralNumber()) {
                throw new RuntimeConfigException("Runtime config schema must be an integer: " + path);
            }
            if (rpcNode == null || !rpcNode.isObject()) {
                throw new RuntimeConfigException("Runtime config rpc must be an object: " + path);
            }
            JsonNode hostNode = rpcNode.get("host");
            JsonNode portNode = rpcNode.get("port");
            if (hostNode == null || !hostNode.isTextual()) {
                throw new RuntimeConfigException("Runtime config rpc.host must be a string: " + path);
            }
            if (portNode == null || !portNode.isIntegralNumber() || !portNode.canConvertToInt()) {
                throw new RuntimeConfigException("Runtime config rpc.port must be an integer: " + path);
            }
            return new RuntimeConfig(schemaNode.intValue(), new Rpc(hostNode.textValue(), portNode.intValue()));
        } catch (RuntimeConfigException error) {
            throw error;
        } catch (IOException error) {
            throw new RuntimeConfigException("Cannot read Runtime config: " + path, error);
        }
    }

    private static void validateHost(String host) {
        if (host == null || host.isBlank()) {
            throw new RuntimeConfigException("Runtime RPC host must not be blank");
        }
        try {
            if (!InetAddress.getByName(host).isLoopbackAddress()) {
                throw new RuntimeConfigException("Runtime RPC host must resolve to loopback: " + host);
            }
        } catch (UnknownHostException error) {
            throw new RuntimeConfigException("Runtime RPC host cannot be resolved: " + host, error);
        }
    }

    public record Rpc(String host, int port) {
    }
}
