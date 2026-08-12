package mar.runtime.result;

public record EvaluationResult(
        boolean ok,
        Object result,
        String stdout,
        String stderr,
        EvaluationError error) {
    public static EvaluationResult success(Object result) {
        return new EvaluationResult(true, result, "", "", null);
    }

    public static EvaluationResult failure(Throwable error) {
        return new EvaluationResult(false, null, "", "", EvaluationError.from(error));
    }

    public EvaluationResult withOutput(String stdout, String stderr) {
        return new EvaluationResult(ok, result, stdout, stderr, error);
    }
}
