package mar.runtime.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandleRegistryTest {
    @Test
    void startsAtOneAndReusesHandleOnlyForSameIdentity() {
        HandleRegistry handles = new HandleRegistry("default");
        EqualValue first = new EqualValue("same");
        EqualValue second = new EqualValue("same");

        String firstHandle = handles.register(first);
        String repeatedHandle = handles.register(first);
        String equalButDistinctHandle = handles.register(second);

        assertEquals("@1", firstHandle);
        assertEquals(firstHandle, repeatedHandle);
        assertEquals("@2", equalButDistinctHandle);
        assertNotEquals(firstHandle, equalButDistinctHandle);
        assertSame(first, handles.resolve(firstHandle));
        assertSame(second, handles.resolve(equalButDistinctHandle));
        assertEquals(2, handles.size());
    }

    @Test
    void clearInvalidatesHandlesAndRestartsSequence() {
        HandleRegistry handles = new HandleRegistry("alpha");
        String oldHandle = handles.register(new Object());

        handles.clear();

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> handles.resolve(oldHandle));
        assertTrue(error.getMessage().contains("Session alpha"));
        assertEquals("@1", handles.register(new Object()));
    }

    private record EqualValue(String value) {
    }
}
