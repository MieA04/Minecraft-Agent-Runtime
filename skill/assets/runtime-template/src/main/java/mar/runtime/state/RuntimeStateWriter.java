package mar.runtime.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import mar.runtime.config.ProjectRootResolver;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public final class RuntimeStateWriter {
    public static final Path STATE_RELATIVE_PATH = Path.of(
            ProjectRootResolver.WORKSPACE_DIRECTORY, "state", "runtime.json");

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final Path stateFile;

    public RuntimeStateWriter(Path projectRoot) {
        this.stateFile = projectRoot.resolve(STATE_RELATIVE_PATH);
    }

    public Path stateFile() {
        return stateFile;
    }

    public void write(RuntimeState state) {
        Path directory = stateFile.getParent();
        Path temporary = null;
        try {
            Files.createDirectories(directory);
            temporary = Files.createTempFile(directory, "runtime-", ".json.tmp");
            try (OutputStream output = Files.newOutputStream(
                    temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                JSON.writeValue(output, state);
                output.flush();
            }
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            Files.move(temporary, stateFile,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            temporary = null;
        } catch (AtomicMoveNotSupportedException error) {
            throw new IllegalStateException("Atomic Runtime state replacement is not supported for " + stateFile, error);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot write Runtime state atomically: " + stateFile, error);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // The primary write failure remains authoritative.
                }
            }
        }
    }
}
