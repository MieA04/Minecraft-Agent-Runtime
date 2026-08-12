package mar.runtime.rpc;

final class RpcRequestException extends IllegalArgumentException {
    private final String responseId;

    RpcRequestException(String responseId, String message) {
        super(message);
        this.responseId = responseId;
    }

    String responseId() {
        return responseId;
    }
}
