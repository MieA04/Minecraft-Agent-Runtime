package mar.runtime.result;

import groovy.lang.GString;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ResultBridge {
    private final HandleRegistry handles;

    public ResultBridge(HandleRegistry handles) {
        this.handles = Objects.requireNonNull(handles, "handles");
    }

    public Object bridge(Object value) {
        SimpleAttempt attempt = simple(value, new IdentityHashMap<>());
        return attempt.simple() ? attempt.value() : descriptor(value);
    }

    private SimpleAttempt simple(Object value, IdentityHashMap<Object, Boolean> visiting) {
        if (value == null) {
            return SimpleAttempt.success(null);
        }
        if (value instanceof Boolean) {
            return SimpleAttempt.success(value);
        }
        if (value instanceof Number number) {
            return SimpleAttempt.success(normalizeNumber(number));
        }
        if (value instanceof String || value instanceof GString || value instanceof Character) {
            return SimpleAttempt.success(value.toString());
        }
        if (value instanceof List<?> list) {
            return simpleList(list, visiting);
        }
        if (value instanceof Map<?, ?> map) {
            return simpleMap(map, visiting);
        }
        return SimpleAttempt.complex();
    }

    private SimpleAttempt simpleList(List<?> list, IdentityHashMap<Object, Boolean> visiting) {
        if (visiting.put(list, Boolean.TRUE) != null) {
            return SimpleAttempt.complex();
        }
        try {
            List<Object> bridged = new ArrayList<>(list.size());
            for (Object element : list) {
                SimpleAttempt item = simple(element, visiting);
                if (!item.simple()) {
                    return SimpleAttempt.complex();
                }
                bridged.add(item.value());
            }
            return SimpleAttempt.success(bridged);
        } finally {
            visiting.remove(list);
        }
    }

    private SimpleAttempt simpleMap(Map<?, ?> map, IdentityHashMap<Object, Boolean> visiting) {
        if (visiting.put(map, Boolean.TRUE) != null) {
            return SimpleAttempt.complex();
        }
        try {
            Map<String, Object> bridged = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    return SimpleAttempt.complex();
                }
                SimpleAttempt item = simple(entry.getValue(), visiting);
                if (!item.simple()) {
                    return SimpleAttempt.complex();
                }
                bridged.put(key, item.value());
            }
            return SimpleAttempt.success(bridged);
        } finally {
            visiting.remove(map);
        }
    }

    private HandleDescriptor descriptor(Object value) {
        String handle = handles.register(value);
        String type = value.getClass().getName();
        return new HandleDescriptor(handle, type, safeString(value, type));
    }

    private Object normalizeNumber(Number number) {
        if (number instanceof Double value) {
            if (value.isNaN()) {
                return "NaN";
            }
            if (value == Double.POSITIVE_INFINITY) {
                return "Infinity";
            }
            if (value == Double.NEGATIVE_INFINITY) {
                return "-Infinity";
            }
        }
        if (number instanceof Float value) {
            if (value.isNaN()) {
                return "NaN";
            }
            if (value == Float.POSITIVE_INFINITY) {
                return "Infinity";
            }
            if (value == Float.NEGATIVE_INFINITY) {
                return "-Infinity";
            }
        }
        return number;
    }

    private String safeString(Object value, String type) {
        try {
            return String.valueOf(value);
        } catch (VirtualMachineError fatal) {
            throw fatal;
        } catch (Throwable ignored) {
            return "<toString failed: " + type + ">";
        }
    }

    private record SimpleAttempt(boolean simple, Object value) {
        static SimpleAttempt success(Object value) {
            return new SimpleAttempt(true, value);
        }

        static SimpleAttempt complex() {
            return new SimpleAttempt(false, null);
        }
    }
}
