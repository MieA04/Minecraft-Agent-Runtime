package mar.runtime.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import mar.runtime.result.EvaluationError;
import mar.runtime.result.EvaluationResult;

public record RpcResponse(
        String id,
        boolean ok,
        Object result,
        String stdout,
        String stderr,
        @JsonInclude(JsonInclude.Include.NON_NULL) EvaluationError error) {

    static RpcResponse fromEvaluation(String id, EvaluationResult evaluation) {
        return new RpcResponse(
                id,
                evaluation.ok(),
                evaluation.result(),
                evaluation.stdout(),
                evaluation.stderr(),
                evaluation.error());
    }

    static RpcResponse error(String id, EvaluationError error) {
        return new RpcResponse(id, false, null, "", "", error);
    }
}
