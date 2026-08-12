package mar.runtime.thread;

import mar.runtime.result.EvaluationResult;
import mar.runtime.session.RuntimeSession;

public interface ExecutionTarget {
    EvaluationResult execute(RuntimeSession session, String code);
}
