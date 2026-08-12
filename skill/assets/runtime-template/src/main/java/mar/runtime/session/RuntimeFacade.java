package mar.runtime.session;

import mar.runtime.tool.ToolManager;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RuntimeFacade {
    private final String sessionName;
    private final Path projectRoot;
    private final ToolManager tools;

    RuntimeFacade(String sessionName, Path projectRoot, ToolManager tools) {
        this.sessionName = sessionName;
        this.projectRoot = projectRoot;
        this.tools = tools;
    }

    public String getSessionName() {
        return sessionName;
    }

    public String getProjectRoot() {
        return projectRoot.toString();
    }

    public ToolManager getTools() {
        return tools;
    }

    public Map<String, Object> info() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("session", sessionName);
        info.put("projectRoot", projectRoot.toString());
        return info;
    }
}
