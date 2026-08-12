package mar.runtime.io;

import mar.runtime.result.EvaluationResult;

import java.io.PrintWriter;
import java.util.Objects;
import java.util.function.Supplier;

public final class EvalOutputCapture {
    private final ThreadLocalPrintRouter router;

    public EvalOutputCapture(ThreadLocalPrintRouter router) {
        this.router = Objects.requireNonNull(router, "router");
    }

    private EvalOutputCapture() {
        this.router = null;
    }

    public static EvalOutputCapture disabled() {
        return new EvalOutputCapture();
    }

    public PrintWriter groovyOut() {
        return router == null ? new PrintWriter(System.out, true) : router.groovyOut();
    }

    public EvaluationResult capture(Supplier<EvaluationResult> evaluation) {
        Objects.requireNonNull(evaluation, "evaluation");
        if (router == null) {
            return evaluation.get();
        }

        ThreadLocalPrintRouter.CaptureContext context = router.openCapture();
        EvaluationResult result;
        try (context) {
            result = evaluation.get();
        }
        return result.withOutput(context.stdout(), context.stderr());
    }
}
