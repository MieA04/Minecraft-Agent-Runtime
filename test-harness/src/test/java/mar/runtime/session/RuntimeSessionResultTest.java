package mar.runtime.session;

import mar.runtime.groovy.GroovyRuntime;
import mar.runtime.result.EvaluationResult;
import mar.runtime.result.HandleDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeSessionResultTest {
    @TempDir
    Path projectRoot;

    private GroovyRuntime groovyRuntime;
    private SessionManager sessions;

    @BeforeEach
    void setUp() {
        groovyRuntime = new GroovyRuntime();
        sessions = new SessionManager(projectRoot, groovyRuntime);
    }

    @AfterEach
    void tearDown() {
        sessions.close();
        groovyRuntime.close();
    }

    @Test
    void bridgesSimpleEvaluationResult() {
        EvaluationResult result = sessions.defaultSession().evaluateBridged(
                "[answer: 40 + 2, nested: [true, 'text']]" );

        assertTrue(result.ok());
        assertNull(result.error());
        assertEquals(MapBuilder.expectedSimpleResult(), result.result());
    }

    @Test
    void complexResultCanBeRecoveredByRefWithSameIdentity() {
        RuntimeSession session = sessions.defaultSession();
        EvaluationResult first = session.evaluateBridged(
                "vars.object = new StringBuilder('value'); vars.object");
        HandleDescriptor descriptor = assertInstanceOf(HandleDescriptor.class, first.result());

        EvaluationResult identity = session.evaluateBridged(
                "ref('" + descriptor.handle() + "').is(vars.object)");
        EvaluationResult repeated = session.evaluateBridged("vars.object");

        assertEquals(true, identity.result());
        assertEquals(descriptor.handle(),
                assertInstanceOf(HandleDescriptor.class, repeated.result()).handle());
    }

    @Test
    void handleCannotBeResolvedFromAnotherSession() {
        RuntimeSession alpha = sessions.getOrCreate("alpha");
        RuntimeSession beta = sessions.getOrCreate("beta");
        HandleDescriptor descriptor = assertInstanceOf(
                HandleDescriptor.class,
                alpha.evaluateBridged("new StringBuilder('alpha')").result());

        EvaluationResult result = beta.evaluateBridged("ref('" + descriptor.handle() + "')");

        assertFalse(result.ok());
        assertEquals("EVAL_EXCEPTION", result.error().code());
        assertTrue(result.error().message().contains("Session beta"));
    }

    @Test
    void oldHandleIsAbsentAfterRuntimeRestart() {
        String oldHandle;
        try (GroovyRuntime firstRuntime = new GroovyRuntime()) {
            SessionManager firstSessions = new SessionManager(projectRoot, firstRuntime);
            oldHandle = assertInstanceOf(
                    HandleDescriptor.class,
                    firstSessions.defaultSession().evaluateBridged("new Object()").result()).handle();
            firstSessions.close();
        }

        try (GroovyRuntime restartedRuntime = new GroovyRuntime()) {
            SessionManager restartedSessions = new SessionManager(projectRoot, restartedRuntime);
            EvaluationResult result = restartedSessions.defaultSession().evaluateBridged(
                    "ref('" + oldHandle + "')");
            assertFalse(result.ok());
            assertTrue(result.error().message().contains("Unknown handle"));
            restartedSessions.close();
        }
    }

    @Test
    void exceptionContainsTypeMessageAndFullStackAndSessionRecovers() {
        RuntimeSession session = sessions.defaultSession();

        EvaluationResult failed = session.evaluateBridged(
                "throw new IllegalStateException('phase-three-boom')");

        assertFalse(failed.ok());
        assertNull(failed.result());
        assertEquals("EVAL_EXCEPTION", failed.error().code());
        assertEquals(IllegalStateException.class.getName(), failed.error().type());
        assertEquals("phase-three-boom", failed.error().message());
        assertTrue(failed.error().stack().contains("java.lang.IllegalStateException: phase-three-boom"));
        assertTrue(failed.error().stack().contains("Script"));

        EvaluationResult recovered = session.evaluateBridged("1 + 2");
        assertTrue(recovered.ok());
        assertEquals(3, recovered.result());
    }

    private static final class MapBuilder {
        private static java.util.Map<String, Object> expectedSimpleResult() {
            java.util.Map<String, Object> expected = new java.util.LinkedHashMap<>();
            expected.put("answer", 42);
            expected.put("nested", java.util.List.of(true, "text"));
            return expected;
        }
    }
}
