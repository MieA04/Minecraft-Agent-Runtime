package mar.runtime.rpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mar.runtime.result.EvaluationError;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

final class RpcConnection implements Runnable {
    private final Socket socket;
    private final ObjectMapper json;
    private final RpcDispatcher dispatcher;
    private final Runnable onClose;

    RpcConnection(Socket socket, ObjectMapper json, RpcDispatcher dispatcher, Runnable onClose) {
        this.socket = socket;
        this.json = json;
        this.dispatcher = dispatcher;
        this.onClose = onClose;
    }

    @Override
    public void run() {
        try (socket;
             BufferedReader input = new BufferedReader(new InputStreamReader(
                     socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter output = new BufferedWriter(new OutputStreamWriter(
                     socket.getOutputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = input.readLine()) != null) {
                RpcResponse response = process(line);
                output.write(json.writeValueAsString(response));
                output.write('\n');
                output.flush();
            }
        } catch (IOException ignored) {
            // Disconnects are connection-scoped and do not stop the Runtime.
        } finally {
            onClose.run();
        }
    }

    private RpcResponse process(String line) {
        if (line.isBlank()) {
            return RpcResponse.error(null, EvaluationError.protocol(
                    "INVALID_JSON", JsonProcessingException.class.getName(), "NDJSON line must contain one JSON object"));
        }
        JsonNode root;
        try {
            root = json.readTree(line);
        } catch (JsonProcessingException error) {
            return RpcResponse.error(null, EvaluationError.protocol(
                    "INVALID_JSON", error.getClass().getName(), error.getOriginalMessage()));
        }

        RpcRequest request;
        try {
            request = RpcRequest.parse(root);
        } catch (RpcRequestException error) {
            return RpcResponse.error(error.responseId(), EvaluationError.protocol(
                    "INVALID_REQUEST", error.getClass().getName(), error.getMessage()));
        }

        try {
            return dispatcher.dispatch(request);
        } catch (VirtualMachineError fatal) {
            throw fatal;
        } catch (Throwable error) {
            return RpcResponse.error(request.id(), EvaluationError.from("RUNTIME_INTERNAL_ERROR", error));
        }
    }
}
