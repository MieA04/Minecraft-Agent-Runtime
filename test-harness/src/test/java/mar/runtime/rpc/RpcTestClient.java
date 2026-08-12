package mar.runtime.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class RpcTestClient implements AutoCloseable {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Socket socket;
    private final BufferedReader input;
    private final BufferedWriter output;

    RpcTestClient(String host, int port) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5_000);
        socket.setSoTimeout(10_000);
        input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    JsonNode eval(String id, String session, String method, String code) throws IOException {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("id", id);
        if (session != null) {
            request.put("session", session);
        }
        request.put("method", method);
        request.put("code", code);
        return sendLine(JSON.writeValueAsString(request));
    }

    JsonNode sendLine(String line) throws IOException {
        output.write(line);
        output.write('\n');
        output.flush();
        String response = input.readLine();
        if (response == null) {
            throw new IOException("RPC server closed before returning a response");
        }
        return JSON.readTree(response);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
