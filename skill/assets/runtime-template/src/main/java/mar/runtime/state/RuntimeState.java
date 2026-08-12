package mar.runtime.state;

import java.time.Instant;

public record RuntimeState(
        int schema,
        String status,
        String runtimeVersion,
        long pid,
        String host,
        int port,
        Instant startedAt,
        String projectRoot,
        String processRole) {
}
