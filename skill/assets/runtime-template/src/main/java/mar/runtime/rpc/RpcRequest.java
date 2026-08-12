package mar.runtime.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import mar.runtime.session.SessionManager;

public record RpcRequest(String id, String session, String method, String code) {
    static RpcRequest parse(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw new RpcRequestException(null, "Request must be a JSON object");
        }

        JsonNode idNode = root.get("id");
        String responseId = idNode != null && idNode.isTextual() ? idNode.textValue() : null;
        if (idNode == null || !idNode.isTextual()) {
            throw new RpcRequestException(null, "Request field id must be a string");
        }

        JsonNode sessionNode = root.get("session");
        String session = SessionManager.DEFAULT_SESSION;
        if (sessionNode != null) {
            if (!sessionNode.isTextual() || sessionNode.textValue().isBlank()) {
                throw new RpcRequestException(responseId, "Request field session must be a non-blank string");
            }
            session = sessionNode.textValue();
        }

        JsonNode methodNode = root.get("method");
        if (methodNode == null || !methodNode.isTextual()) {
            throw new RpcRequestException(responseId, "Request field method must be a string");
        }

        JsonNode codeNode = root.get("code");
        if (codeNode == null || !codeNode.isTextual()) {
            throw new RpcRequestException(responseId, "Request field code must be a string");
        }

        return new RpcRequest(responseId, session, methodNode.textValue(), codeNode.textValue());
    }
}
