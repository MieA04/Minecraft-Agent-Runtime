package mar.runtime.result;

public record HandleDescriptor(String kind, String handle, String type, String string) {
    public static final String KIND = "handle";

    public HandleDescriptor(String handle, String type, String string) {
        this(KIND, handle, type, string);
    }
}
