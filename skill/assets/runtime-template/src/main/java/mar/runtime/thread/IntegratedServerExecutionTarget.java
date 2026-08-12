package mar.runtime.thread;

import mar.runtime.result.EvaluationResult;
import mar.runtime.session.RuntimeSession;

public final class IntegratedServerExecutionTarget implements ExecutionTarget {
    private static final String SERVER_ACCESSOR = "getSingleplayerServer";

    private final ClientExecutionTarget clientTarget;

    public IntegratedServerExecutionTarget(ClientExecutionTarget clientTarget) {
        this.clientTarget = clientTarget;
    }

    @Override
    public EvaluationResult execute(RuntimeSession session, String code) {
        Object client = clientTarget.resolveClient();
        Object server = ReflectiveExecutionSupport.invokeNoArg(
                client, SERVER_ACCESSOR, "Integrated Server Thread");
        if (server == null) {
            throw new TargetUnavailableException("Integrated Server is not available");
        }
        session.binding().setVariable("mc", client);
        return ReflectiveExecutionSupport.executeOn(server, session, code, "Integrated Server Thread");
    }
}
