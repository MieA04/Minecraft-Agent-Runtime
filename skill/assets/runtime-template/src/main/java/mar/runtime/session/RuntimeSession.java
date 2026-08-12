package mar.runtime.session;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.util.Expando;
import mar.runtime.groovy.GroovyRuntime;
import mar.runtime.io.EvalOutputCapture;
import mar.runtime.result.EvaluationResult;
import mar.runtime.result.HandleRegistry;
import mar.runtime.result.ResultBridge;
import mar.runtime.tool.ToolManager;

import java.nio.file.Path;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public final class RuntimeSession {
    private final String name;
    private final Instant createdAt;
    private final Binding binding;
    private final Expando vars;
    private final GroovyShell shell;
    private final HandleRegistry handles;
    private final ResultBridge resultBridge;
    private final EvalOutputCapture outputCapture;
    private final PrintWriter groovyOut;
    private final ReentrantLock evalMutex = new ReentrantLock();

    RuntimeSession(
            String name,
            Path projectRoot,
            GroovyRuntime groovyRuntime,
            EvalOutputCapture outputCapture) {
        this.name = Objects.requireNonNull(name, "name");
        this.createdAt = Instant.now();
        this.vars = new Expando();
        Expando tools = new Expando();
        this.vars.setProperty("tools", tools);
        this.handles = new HandleRegistry(name);
        this.resultBridge = new ResultBridge(handles);
        this.outputCapture = Objects.requireNonNull(outputCapture, "outputCapture");
        this.groovyOut = outputCapture.groovyOut();
        this.binding = new Binding();
        this.binding.setVariable("vars", vars);
        this.binding.setVariable("ref", new RefBinding(handles));
        ToolManager toolManager = new ToolManager(
                projectRoot, binding, tools, groovyRuntime.parentClassLoader());
        this.binding.setVariable("runtime", new RuntimeFacade(name, projectRoot, toolManager));
        this.binding.setVariable("out", groovyOut);
        this.shell = groovyRuntime.createShell(binding);
    }

    public String name() {
        return name;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Binding binding() {
        return binding;
    }

    public Expando vars() {
        return vars;
    }

    public Object evaluate(String code) {
        Objects.requireNonNull(code, "code");
        evalMutex.lock();
        try {
            return shell.evaluate(code);
        } finally {
            groovyOut.flush();
            evalMutex.unlock();
        }
    }

    public EvaluationResult evaluateBridged(String code) {
        Objects.requireNonNull(code, "code");
        evalMutex.lock();
        try {
            return outputCapture.capture(() -> evaluateAndBridge(code));
        } finally {
            evalMutex.unlock();
        }
    }

    private EvaluationResult evaluateAndBridge(String code) {
        try {
            Object raw = shell.evaluate(code);
            return EvaluationResult.success(resultBridge.bridge(raw));
        } catch (VirtualMachineError fatal) {
            throw fatal;
        } catch (Throwable error) {
            return EvaluationResult.failure(error);
        } finally {
            groovyOut.flush();
        }
    }
}
