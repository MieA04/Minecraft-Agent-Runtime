package mar.runtime.state;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeStateWriterTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path projectRoot;

    @Test
    void atomicallyCreatesAndReplacesCompleteState() throws IOException {
        RuntimeStateWriter writer = new RuntimeStateWriter(projectRoot);
        Instant startedAt = Instant.parse("2026-08-11T12:00:00Z");

        writer.write(state("starting", 0, startedAt));
        writer.write(state("ready", 49152, startedAt));

        JsonNode json = JSON.readTree(writer.stateFile().toFile());
        assertEquals(1, json.get("schema").intValue());
        assertEquals("ready", json.get("status").textValue());
        assertEquals(49152, json.get("port").intValue());
        assertEquals("2026-08-11T12:00:00Z", json.get("startedAt").textValue());

        try (var files = Files.list(writer.stateFile().getParent())) {
            List<Path> temporaryFiles = files
                    .filter(path -> path.getFileName().toString().endsWith(".tmp"))
                    .toList();
            assertTrue(temporaryFiles.isEmpty(), "successful replacement must not leave temporary files");
        }
    }

    private RuntimeState state(String status, int port, Instant startedAt) {
        return new RuntimeState(
                1, status, "test", 123, "127.0.0.1", port, startedAt,
                projectRoot.toString(), "unknown");
    }
}
