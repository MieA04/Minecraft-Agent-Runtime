package mar.runtime.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class ProjectRootResolver {
    public static final String PROJECT_ROOT_PROPERTY = "mar.projectRoot";
    public static final String WORKSPACE_DIRECTORY = ".minecraft-agent-runtime";

    private ProjectRootResolver() {
    }

    public static Path resolve() {
        return resolve(System.getProperty(PROJECT_ROOT_PROPERTY), System.getProperty("user.dir"));
    }

    static Path resolve(String configuredRoot, String userDirectory) {
        if (configuredRoot != null && !configuredRoot.isBlank()) {
            return validateProjectRoot(Path.of(configuredRoot), "system property " + PROJECT_ROOT_PROPERTY);
        }

        if (userDirectory == null || userDirectory.isBlank()) {
            throw new ProjectRootResolutionException(
                    "Cannot resolve project root: user.dir is missing and " + PROJECT_ROOT_PROPERTY + " is not set");
        }

        Path candidate = Path.of(userDirectory).toAbsolutePath().normalize();
        while (candidate != null) {
            if (Files.isDirectory(candidate.resolve(WORKSPACE_DIRECTORY))) {
                return candidate;
            }
            candidate = candidate.getParent();
        }

        throw new ProjectRootResolutionException(
                "Cannot resolve project root from user.dir=" + userDirectory
                        + "; no parent contains " + WORKSPACE_DIRECTORY);
    }

    private static Path validateProjectRoot(Path path, String source) {
        Objects.requireNonNull(path, "path");
        Path normalized = path.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalized)) {
            throw new ProjectRootResolutionException("Project root from " + source + " is not a directory: " + normalized);
        }
        if (!Files.isDirectory(normalized.resolve(WORKSPACE_DIRECTORY))) {
            throw new ProjectRootResolutionException(
                    "Project root from " + source + " does not contain " + WORKSPACE_DIRECTORY + ": " + normalized);
        }
        return normalized;
    }
}
