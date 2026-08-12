package mar.runtime.thread;

import mar.runtime.result.EvaluationResult;
import mar.runtime.session.RuntimeSession;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ClientExecutionTarget implements ExecutionTarget {
    private static final String CLIENT_CLASS = "net.minecraft.client.Minecraft";
    private static final String SINGLETON_METHOD = "getInstance";

    private final ClassLoader classLoader;

    public ClientExecutionTarget(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public EvaluationResult execute(RuntimeSession session, String code) {
        Object client = resolveClient();
        session.binding().setVariable("mc", client);
        return ReflectiveExecutionSupport.executeOn(client, session, code, "Minecraft Client Thread");
    }

    Object resolveClient() {
        try {
            Class<?> clientClass = Class.forName(CLIENT_CLASS, false, classLoader);
            Method getter = clientClass.getMethod(SINGLETON_METHOD);
            Object client = getter.invoke(null);
            if (client == null) {
                throw new TargetUnavailableException("Minecraft Client singleton is not available");
            }
            return client;
        } catch (ClassNotFoundException error) {
            throw new TargetUnavailableException(
                    "Minecraft Client classes are not available in this Runtime", error);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
                 | LinkageError | SecurityException error) {
            throw new TargetUnavailableException("Cannot resolve Minecraft Client singleton", error);
        }
    }
}
