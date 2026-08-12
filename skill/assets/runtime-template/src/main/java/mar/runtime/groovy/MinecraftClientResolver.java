package mar.runtime.groovy;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

final class MinecraftClientResolver {
    private static final String CLIENT_CLASS = "net.minecraft.client.Minecraft";
    private static final String SINGLETON_METHOD = "getInstance";

    Optional<Object> resolve(ClassLoader classLoader) {
        try {
            Class<?> minecraftClass = Class.forName(CLIENT_CLASS, false, classLoader);
            Method getter = minecraftClass.getMethod(SINGLETON_METHOD);
            if (!Modifier.isStatic(getter.getModifiers()) || getter.getParameterCount() != 0) {
                return Optional.empty();
            }
            return Optional.ofNullable(getter.invoke(null));
        } catch (ClassNotFoundException error) {
            return Optional.empty();
        } catch (ReflectiveOperationException | LinkageError | SecurityException error) {
            return Optional.empty();
        }
    }
}
