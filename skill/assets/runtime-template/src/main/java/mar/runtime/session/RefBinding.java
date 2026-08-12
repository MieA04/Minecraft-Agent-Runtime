package mar.runtime.session;

import mar.runtime.result.HandleRegistry;

public final class RefBinding {
    private final HandleRegistry handles;

    RefBinding(HandleRegistry handles) {
        this.handles = handles;
    }

    public Object call(String handle) {
        return handles.resolve(handle);
    }
}
