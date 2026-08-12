package mar.runtime.thread;

public final class TargetUnavailableException extends IllegalStateException {
    public TargetUnavailableException(String message) {
        super(message);
    }

    public TargetUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
