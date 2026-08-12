package mar.runtime.result;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

public final class HandleRegistry {
    private final String sessionName;
    private final Map<String, Object> handleToObject = new HashMap<>();
    private final IdentityHashMap<Object, String> objectToHandle = new IdentityHashMap<>();
    private long nextHandle = 1;

    public HandleRegistry(String sessionName) {
        if (sessionName == null || sessionName.isBlank()) {
            throw new IllegalArgumentException("Session name must not be blank");
        }
        this.sessionName = sessionName;
    }

    public synchronized String register(Object object) {
        Objects.requireNonNull(object, "object");
        String existing = objectToHandle.get(object);
        if (existing != null) {
            return existing;
        }

        String handle = "@" + nextHandle++;
        handleToObject.put(handle, object);
        objectToHandle.put(object, handle);
        return handle;
    }

    public synchronized Object resolve(String handle) {
        if (handle == null || handle.isBlank()) {
            throw new IllegalArgumentException("Handle must not be blank");
        }
        Object object = handleToObject.get(handle);
        if (object == null) {
            throw new IllegalArgumentException("Unknown handle in Session " + sessionName + ": " + handle);
        }
        return object;
    }

    public synchronized int size() {
        return handleToObject.size();
    }

    public synchronized void clear() {
        handleToObject.clear();
        objectToHandle.clear();
        nextHandle = 1;
    }
}
