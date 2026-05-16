package org.jetby.treexBuyer.tools;

public class ReflectionUtil {

    public static Object callMethod(
            String className,
            String methodName,
            Object instance, // null if static
            Object... args
    ) {
        try {
            Class<?> clazz = Class.forName(className);

            Class<?>[] paramTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i].getClass();
            }

            var method = clazz.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);

            return method.invoke(instance, args);

        } catch (Exception e) {
            throw new RuntimeException("Reflection call failed: " + className + "#" + methodName, e);
        }
    }

    public static Object callMethodSafe(
            String className,
            String methodName,
            Object instance,
            Object... args
    ) {
        try {
            Class<?> clazz = Class.forName(className);

            for (var method : clazz.getDeclaredMethods()) {
                if (!method.getName().equals(methodName)) continue;
                if (method.getParameterCount() != args.length) continue;

                method.setAccessible(true);
                return method.invoke(instance, args);
            }

            throw new NoSuchMethodException(methodName);

        } catch (Exception e) {
            throw new RuntimeException("Reflection call failed: " + className + "#" + methodName, e);
        }
    }
}
