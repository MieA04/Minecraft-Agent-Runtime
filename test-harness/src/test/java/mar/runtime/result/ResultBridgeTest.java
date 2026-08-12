package mar.runtime.result;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class ResultBridgeTest {
    private HandleRegistry handles;
    private ResultBridge bridge;

    @BeforeEach
    void setUp() {
        handles = new HandleRegistry("default");
        bridge = new ResultBridge(handles);
    }

    @Test
    void preservesPrimitiveValuesAndNormalizesStringLikeValues() {
        assertEquals(null, bridge.bridge(null));
        assertEquals(true, bridge.bridge(true));
        assertEquals(42, bridge.bridge(42));
        assertEquals(new BigDecimal("12.50"), bridge.bridge(new BigDecimal("12.50")));
        assertEquals("text", bridge.bridge("text"));
        assertEquals("x", bridge.bridge('x'));
    }

    @Test
    void convertsNonFiniteNumbersToFrozenStrings() {
        assertEquals("NaN", bridge.bridge(Double.NaN));
        assertEquals("Infinity", bridge.bridge(Double.POSITIVE_INFINITY));
        assertEquals("-Infinity", bridge.bridge(Double.NEGATIVE_INFINITY));
        assertEquals("NaN", bridge.bridge(Float.NaN));
        assertEquals("Infinity", bridge.bridge(Float.POSITIVE_INFINITY));
        assertEquals("-Infinity", bridge.bridge(Float.NEGATIVE_INFINITY));
    }

    @Test
    void recursivelyCopiesNestedSimpleListAndMap() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("enabled", true);
        nested.put("values", List.of(1, 2, "three"));
        List<Object> source = List.of("root", nested);

        Object result = bridge.bridge(source);

        assertEquals(source, result);
        assertInstanceOf(ArrayList.class, result);
        assertInstanceOf(LinkedHashMap.class, ((List<?>) result).get(1));
    }

    @Test
    void complexElementMakesEntireListAHandle() {
        List<Object> source = new ArrayList<>();
        source.add("simple");
        source.add(new StringBuilder("complex"));

        HandleDescriptor descriptor = assertInstanceOf(HandleDescriptor.class, bridge.bridge(source));

        assertEquals("handle", descriptor.kind());
        assertEquals("@1", descriptor.handle());
        assertEquals(ArrayList.class.getName(), descriptor.type());
        assertSame(source, handles.resolve(descriptor.handle()));
    }

    @Test
    void nonStringMapKeyMakesEntireMapAHandle() {
        Map<Object, Object> source = new LinkedHashMap<>();
        source.put(1, "value");

        HandleDescriptor descriptor = assertInstanceOf(HandleDescriptor.class, bridge.bridge(source));

        assertSame(source, handles.resolve(descriptor.handle()));
    }

    @Test
    void listCycleBecomesHandleWithoutRecursingForever() {
        List<Object> source = new ArrayList<>();
        source.add("before-cycle");
        source.add(source);

        HandleDescriptor descriptor = assertInstanceOf(HandleDescriptor.class, bridge.bridge(source));

        assertSame(source, handles.resolve(descriptor.handle()));
    }

    @Test
    void mapCycleBecomesHandleWithoutRecursingForever() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("self", source);

        HandleDescriptor descriptor = assertInstanceOf(HandleDescriptor.class, bridge.bridge(source));

        assertSame(source, handles.resolve(descriptor.handle()));
    }

    @Test
    void descriptorReusesIdentityHandleAndDoesNotUseEquals() {
        EqualValue first = new EqualValue("same");
        EqualValue second = new EqualValue("same");

        HandleDescriptor firstDescriptor = assertInstanceOf(HandleDescriptor.class, bridge.bridge(first));
        HandleDescriptor repeatedDescriptor = assertInstanceOf(HandleDescriptor.class, bridge.bridge(first));
        HandleDescriptor secondDescriptor = assertInstanceOf(HandleDescriptor.class, bridge.bridge(second));

        assertEquals(firstDescriptor.handle(), repeatedDescriptor.handle());
        assertEquals("@1", firstDescriptor.handle());
        assertEquals("@2", secondDescriptor.handle());
    }

    @Test
    void throwingToStringProducesSafeDescriptorText() {
        ThrowingToString source = new ThrowingToString();

        HandleDescriptor descriptor = assertInstanceOf(HandleDescriptor.class, bridge.bridge(source));

        assertEquals("<toString failed: " + ThrowingToString.class.getName() + ">", descriptor.string());
        assertSame(source, handles.resolve(descriptor.handle()));
    }

    private record EqualValue(String value) {
    }

    private static final class ThrowingToString {
        @Override
        public String toString() {
            throw new AssertionError("broken toString");
        }
    }
}
