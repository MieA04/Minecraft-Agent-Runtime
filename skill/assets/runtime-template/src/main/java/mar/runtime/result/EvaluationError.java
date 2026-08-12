package mar.runtime.result;

import java.io.PrintWriter;
import java.io.StringWriter;

public record EvaluationError(String code, String type, String message, String stack) {
    public static final String EVAL_EXCEPTION = "EVAL_EXCEPTION";

    public static EvaluationError from(Throwable error) {
        return from(EVAL_EXCEPTION, error);
    }

    public static EvaluationError from(String code, Throwable error) {
        StringWriter stack = new StringWriter();
        error.printStackTrace(new PrintWriter(stack));
        return new EvaluationError(
                code,
                error.getClass().getName(),
                error.getMessage(),
                stack.toString());
    }

    public static EvaluationError protocol(String code, String type, String message) {
        return new EvaluationError(code, type, message, "");
    }
}
