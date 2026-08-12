package mar.runtime.thread;

import mar.runtime.result.EvaluationResult;
import mar.runtime.session.RuntimeSession;

public final class RawExecutionTarget implements ExecutionTarget {
    @Override
    public EvaluationResult execute(RuntimeSession session, String code) {
        return session.evaluateBridged(code);
    }
}
