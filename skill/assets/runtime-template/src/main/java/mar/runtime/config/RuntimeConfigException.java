package mar.runtime.config;

public final class RuntimeConfigException extends IllegalStateException {
    public RuntimeConfigException(String message) {
        super(message);
    }

    public RuntimeConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
