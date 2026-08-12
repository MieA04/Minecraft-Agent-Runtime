package mar.runtime.tool;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.util.Expando;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public final class ToolManager {
    private static final String WORKSPACE_DIRECTORY = ".minecraft-agent-runtime";
    private static final String TOOLS_DIRECTORY = "tools";
    private static final String GROOVY_SUFFIX = ".groovy";
    private static final Set<String> STABLE_ROOTS = Set.of("minecraft", "loader", "mod");
    private static final Set<String> ALL_ROOTS = Set.of("minecraft", "loader", "mod", "experimental");

    private final Path toolRoot;
    private final Binding sessionBinding;
    private final Expando toolsNamespace;
    private final ClassLoader parentClassLoader;
    private final Set<String> loadedPaths = new LinkedHashSet<>();

    public ToolManager(
            Path projectRoot,
            Binding sessionBinding,
            Expando toolsNamespace,
            ClassLoader parentClassLoader) {
        this.toolRoot = Objects.requireNonNull(projectRoot, "projectRoot")
                .toAbsolutePath().normalize()
                .resolve(WORKSPACE_DIRECTORY).resolve(TOOLS_DIRECTORY);
        this.sessionBinding = Objects.requireNonNull(sessionBinding, "sessionBinding");
        this.toolsNamespace = Objects.requireNonNull(toolsNamespace, "toolsNamespace");
        this.parentClassLoader = Objects.requireNonNull(parentClassLoader, "parentClassLoader");
    }

    public synchronized Map<String, Object> reloadPath(String requestedPath) {
        String phase = "validate";
        String normalizedPath = requestedPath;
        try {
            ToolPath toolPath = resolveToolPath(requestedPath);
            normalizedPath = toolPath.relativePath();

            phase = "read";
            Path realRoot = toolRoot.toRealPath();
            Path realFile = toolPath.file().toRealPath();
            if (!realFile.startsWith(realRoot) || !Files.isRegularFile(realFile)) {
                throw new IOException("Tool path is not a regular file inside the project tool root");
            }
            String source = Files.readString(realFile);

            Object tool;
            phase = "compile";
            try (GroovyClassLoader loader = new GroovyClassLoader(parentClassLoader)) {
                Binding temporaryBinding = new Binding(copyBindingVariables(sessionBinding));
                GroovyShell shell = new GroovyShell(loader, temporaryBinding);
                Script script = shell.parse(source, normalizedPath);

                phase = "init";
                tool = script.run();
                if (tool == null) {
                    throw new IllegalStateException("Tool script must return a non-null object");
                }
            }

            phase = "replace";
            replaceNamespace(toolPath.namespaceSegments(), tool);
            loadedPaths.add(normalizedPath);
            return success(normalizedPath, toolPath.namespace(), false);
        } catch (VirtualMachineError fatal) {
            throw fatal;
        } catch (Throwable error) {
            return failure(normalizedPath, phase, error);
        }
    }

    public synchronized List<Map<String, Object>> reloadAllStable() {
        List<String> discovered = new ArrayList<>();
        List<Map<String, Object>> results = new ArrayList<>();

        for (String rootName : STABLE_ROOTS.stream().sorted().toList()) {
            Path category = toolRoot.resolve(rootName);
            if (!Files.exists(category)) {
                continue;
            }
            try (Stream<Path> paths = Files.walk(category)) {
                paths.filter(Files::isRegularFile)
                        .map(toolRoot::relativize)
                        .map(ToolManager::portablePath)
                        .filter(path -> path.endsWith(GROOVY_SUFFIX))
                        .forEach(discovered::add);
            } catch (IOException error) {
                results.add(failure(rootName, "read", error));
            }
        }

        discovered.sort(Comparator.naturalOrder());
        for (String path : discovered) {
            results.add(reloadPath(path));
        }

        Set<String> present = new LinkedHashSet<>(discovered);
        List<String> deleted = loadedPaths.stream()
                .filter(ToolManager::isStablePath)
                .filter(path -> !present.contains(path))
                .sorted()
                .toList();
        for (String path : deleted) {
            ToolPath toolPath;
            try {
                toolPath = resolveToolPath(path);
                removeNamespace(toolPath.namespaceSegments());
                loadedPaths.remove(path);
                results.add(success(path, toolPath.namespace(), true));
            } catch (VirtualMachineError fatal) {
                throw fatal;
            } catch (Throwable error) {
                results.add(failure(path, "replace", error));
            }
        }
        return List.copyOf(results);
    }

    private ToolPath resolveToolPath(String requestedPath) {
        if (requestedPath == null || requestedPath.isBlank()) {
            throw new IllegalArgumentException("Tool path must be a non-empty relative path");
        }
        String portable = requestedPath.replace('\\', '/');
        Path relative = Path.of(portable).normalize();
        if (relative.isAbsolute() || relative.getNameCount() < 2
                || relative.startsWith("..") || !portablePath(relative).endsWith(GROOVY_SUFFIX)) {
            throw new IllegalArgumentException("Tool path must be a .groovy file below a tool category");
        }

        String normalized = portablePath(relative);
        String category = relative.getName(0).toString();
        if (!ALL_ROOTS.contains(category)) {
            throw new IllegalArgumentException("Unknown tool category: " + category);
        }

        Path file = toolRoot.resolve(relative).normalize();
        if (!file.startsWith(toolRoot)) {
            throw new IllegalArgumentException("Tool path escapes the project tool root");
        }

        List<String> namespaceSegments = new ArrayList<>();
        for (int index = 0; index < relative.getNameCount(); index++) {
            String segment = relative.getName(index).toString();
            if (index == relative.getNameCount() - 1) {
                segment = segment.substring(0, segment.length() - GROOVY_SUFFIX.length());
            }
            if (segment.isBlank()) {
                throw new IllegalArgumentException("Tool namespace contains an empty segment");
            }
            namespaceSegments.add(segment);
        }
        return new ToolPath(normalized, file, List.copyOf(namespaceSegments));
    }

    private void replaceNamespace(List<String> segments, Object tool) {
        Expando cursor = toolsNamespace;
        for (int index = 0; index < segments.size() - 1; index++) {
            String segment = segments.get(index);
            Object existing = cursor.getProperties().get(segment);
            if (existing != null && !(existing instanceof Expando)) {
                throw new IllegalStateException("Tool namespace conflicts at " + segment);
            }
            if (existing == null) {
                Expando child = new Expando();
                cursor.setProperty(segment, child);
                cursor = child;
            } else {
                cursor = (Expando) existing;
            }
        }
        cursor.setProperty(segments.get(segments.size() - 1), tool);
    }

    private void removeNamespace(List<String> segments) {
        List<Expando> parents = new ArrayList<>();
        Expando cursor = toolsNamespace;
        parents.add(cursor);
        for (int index = 0; index < segments.size() - 1; index++) {
            Object child = cursor.getProperties().get(segments.get(index));
            if (!(child instanceof Expando next)) {
                return;
            }
            cursor = next;
            parents.add(cursor);
        }
        cursor.getProperties().remove(segments.get(segments.size() - 1));
        for (int index = parents.size() - 1; index > 0; index--) {
            Expando child = parents.get(index);
            if (!child.getProperties().isEmpty()) {
                break;
            }
            parents.get(index - 1).getProperties().remove(segments.get(index - 1));
        }
    }

    private static boolean isStablePath(String path) {
        int separator = path.indexOf('/');
        return separator > 0 && STABLE_ROOTS.contains(path.substring(0, separator));
    }

    @SuppressWarnings("rawtypes")
    private static Map<String, Object> copyBindingVariables(Binding binding) {
        Map variables = binding.getVariables();
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Object key : variables.keySet()) {
            copy.put(String.valueOf(key), variables.get(key));
        }
        return copy;
    }

    private static String portablePath(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static Map<String, Object> success(String path, String namespace, boolean removed) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("path", path);
        result.put("namespace", namespace);
        result.put("ok", true);
        result.put("phase", "replace");
        result.put("removed", removed);
        return result;
    }

    private static Map<String, Object> failure(String path, String phase, Throwable error) {
        StringWriter stack = new StringWriter();
        error.printStackTrace(new PrintWriter(stack));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("path", path);
        result.put("ok", false);
        result.put("phase", phase);
        result.put("exceptionType", error.getClass().getName());
        result.put("message", String.valueOf(error.getMessage()));
        result.put("stack", stack.toString());
        return result;
    }

    private record ToolPath(String relativePath, Path file, List<String> namespaceSegments) {
        String namespace() {
            return "vars.tools." + String.join(".", namespaceSegments);
        }
    }
}
