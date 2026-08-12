package mar.runtime.rpc;

import mar.runtime.result.EvaluationError;
import mar.runtime.session.RuntimeSession;
import mar.runtime.session.SessionManager;
import mar.runtime.thread.ExecutionTarget;
import mar.runtime.thread.TargetUnavailableException;

import java.util.Set;

final class RpcDispatcher {
    static final String EVAL_RAW = "eval.raw";
    static final String EVAL_CLIENT = "eval.client";
    static final String EVAL_SERVER = "eval.server";
    static final Set<String> METHODS = Set.of(EVAL_RAW, EVAL_CLIENT, EVAL_SERVER);

    private final SessionManager sessions;
    private final ExecutionTarget rawTarget;
    private final ExecutionTarget clientTarget;
    private final ExecutionTarget serverTarget;

    RpcDispatcher(
            SessionManager sessions,
            ExecutionTarget rawTarget,
            ExecutionTarget clientTarget,
            ExecutionTarget serverTarget) {
        this.sessions = sessions;
        this.rawTarget = rawTarget;
        this.clientTarget = clientTarget;
        this.serverTarget = serverTarget;
    }

    RpcResponse dispatch(RpcRequest request) {
        if (!METHODS.contains(request.method())) {
            return RpcResponse.error(request.id(), EvaluationError.protocol(
                    "METHOD_NOT_FOUND",
                    RpcDispatcher.class.getName(),
                    "Unknown RPC method: " + request.method()));
        }

        RuntimeSession session = sessions.getOrCreate(request.session());
        ExecutionTarget target = switch (request.method()) {
            case EVAL_RAW -> rawTarget;
            case EVAL_CLIENT -> clientTarget;
            case EVAL_SERVER -> serverTarget;
            default -> throw new IllegalStateException("Validated method was not dispatchable: " + request.method());
        };
        try {
            return RpcResponse.fromEvaluation(request.id(), target.execute(session, request.code()));
        } catch (TargetUnavailableException error) {
            return RpcResponse.error(request.id(), EvaluationError.protocol(
                    "TARGET_UNAVAILABLE",
                    error.getClass().getName(),
                    error.getMessage()));
        }
    }
}
