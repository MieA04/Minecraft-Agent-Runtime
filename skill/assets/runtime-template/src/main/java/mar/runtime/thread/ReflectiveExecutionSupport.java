package mar.runtime.thread;

import mar.runtime.result.EvaluationResult;
import mar.runtime.session.RuntimeSession;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

final class ReflectiveExecutionSupport {
    private ReflectiveExecutionSupport() {
    }

    static EvaluationResult executeOn(Object target, RuntimeSession session, String code, String targetName) {
        Method sameThread = publicNoArgMethod(target.getClass(), "isSameThread", targetName);
        if (invokeBoolean(sameThread, target, targetName)) {
            return session.evaluateBridged(code);
        }
        if (!(target instanceof Executor executor)) {
            throw new TargetUnavailableException(targetName + " does not implement java.util.concurrent.Executor");
        }

        CompletableFuture<EvaluationResult> completion = new CompletableFuture<>();
        try {
            executor.execute(() -> {
                try {
                    completion.complete(session.evaluateBridged(code));
                } catch (VirtualMachineError fatal) {
                    completion.completeExceptionally(fatal);
                    throw fatal;
                } catch (Throwable error) {
                    completion.completeExceptionally(error);
                }
            });
        } catch (RuntimeException error) {
            throw new TargetUnavailableException("Cannot submit eval to " + targetName, error);
        }

        try {
            return completion.get();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for " + targetName + " eval", error);
        } catch (ExecutionException error) {
            Throwable cause = error.getCause();
            if (cause instanceof VirtualMachineError fatal) {
                throw fatal;
            }
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error fatal) {
                throw fatal;
            }
            throw new IllegalStateException(targetName + " eval failed", cause);
        }
    }

    static Object invokeNoArg(Object target, String methodName, String targetName) {
        Method method = publicNoArgMethod(target.getClass(), methodName, targetName);
        try {
            return method.invoke(target);
        } catch (IllegalAccessException | InvocationTargetException error) {
            throw new TargetUnavailableException(
                    "Cannot invoke " + methodName + " while resolving " + targetName, error);
        }
    }

    private static Method publicNoArgMethod(Class<?> type, String name, String targetName) {
        try {
            Method method = type.getMethod(name);
            if (method.getParameterCount() != 0) {
                throw new NoSuchMethodException(name + " has parameters");
            }
            return method;
        } catch (ReflectiveOperationException | SecurityException error) {
            throw new TargetUnavailableException(
                    targetName + " does not expose required method " + name + "()", error);
        }
    }

    private static boolean invokeBoolean(Method method, Object target, String targetName) {
        try {
            Object result = method.invoke(target);
            if (!(result instanceof Boolean value)) {
                throw new TargetUnavailableException(
                        targetName + ".isSameThread() did not return boolean");
            }
            return value;
        } catch (IllegalAccessException | InvocationTargetException error) {
            throw new TargetUnavailableException(
                    "Cannot determine current thread for " + targetName, error);
        }
    }
}
