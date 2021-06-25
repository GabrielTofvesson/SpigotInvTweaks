package dev.w1zzrd.invtweaks.entity;

import java.lang.reflect.*;
import java.util.Objects;

public final class Reflect {
    private Reflect() { throw new UnsupportedOperationException("Functional class"); }

    public static boolean contains(final String[] array, final String find) {
        for (final String check : array)
            if (check.equals(find))
                return true;
        return false;
    }

    public static <T> Method findDeclaredMethod(final Class<T> rootType, final String[] methodNames, final Object[] args) {
        Class<? super T> current = rootType;

        do {
            for (final Method check : current.getDeclaredMethods())
                if (contains(methodNames, check.getName()) && argsMatch(check.getParameterTypes(), args))
                    return check;

            current = current.getSuperclass();
        } while (true);
    }

    public static <T> Field findDeclaredField(final Class<T> rootType, final Class<?> expectedType, final String... fieldNames) {
        Class<? super T> current = rootType;

        do {
            for (final Field check : current.getDeclaredFields())
                if (contains(fieldNames, check.getName()) && (expectedType == null || check.getType().equals(expectedType)))
                    return check;

            current = current.getSuperclass();
        } while (true);
    }

    public static <T> Constructor<T> findDeclaredConstructor(final Class<T> type, final Object[] args) {
        for (final Constructor<?> check : type.getDeclaredConstructors())
            if (argsMatch(check.getParameterTypes(), args))
                return (Constructor<T>) check;
        return null;
    }

    public static Object reflectInvoke(final Object target, final String[] methodNames, final Object... args) {
        final Method targetMethod = findDeclaredMethod(target.getClass(), methodNames, args);
        targetMethod.setAccessible(true);

        try {
            return targetMethod.invoke(target, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void reflectSetStaticField(final Class<?> target, final Object value, final String... fieldNames) {
        reflectSetStaticField(target, null, value, fieldNames);
    }
    public static void reflectSetStaticField(final Class<?> target, final Class<?> expectedType, final Object value, final String... fieldNames) {
        final Field targetField = findDeclaredField(target, expectedType, fieldNames);
        targetField.setAccessible(true);

        try {
            targetField.set(null, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static <T> T reflectGetStaticField(final Class<?> target, final String... fieldNames) {
        return (T) reflectGetStaticField(target, null, fieldNames);
    }

    public static <T, R> T reflectGetGenericStaticField(final Class<?> target, final Class<T> fieldType, final Class<R> genericType) {
        for (final Field check : target.getDeclaredFields()) {
            if (fieldType.isAssignableFrom(check.getType())) {
                final Type checkFieldType = check.getGenericType();

                if (checkFieldType instanceof final ParameterizedType pCFT && pCFT.getActualTypeArguments().length != 0) {
                    for (final Type typeArg : pCFT.getActualTypeArguments()) {
                        if (typeArg == genericType) {
                            try {
                                return (T)check.get(null);
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                                return null;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    public static <T> T reflectGetStaticField(final Class<?> target, final Class<T> expectedType, final String... fieldNames) {
        final Field targetField = findDeclaredField(target, expectedType, fieldNames);
        targetField.setAccessible(true);

        try {
            return (T)targetField.get(null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void reflectSetField(final Object target, final Object value, final String... fieldNames) {
        reflectSetField(target, null, value, fieldNames);
    }
    public static void reflectSetField(final Object target, final Class<?> expectedType, final Object value, final String... fieldNames) {
        final Field targetField = findDeclaredField(target.getClass(), expectedType, fieldNames);
        targetField.setAccessible(true);

        try {
            targetField.set(target, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static Object reflectGetField(final Object target, final String... fieldNames) {
        return reflectGetField(target, null, fieldNames);
    }
    public static <T> T reflectGetField(final Object target, final Class<T> expectedType, final String... fieldNames) {
        final Field targetField = findDeclaredField(target.getClass(), expectedType, fieldNames);
        targetField.setAccessible(true);

        try {
            return (T)targetField.get(target);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static <T> T reflectConstruct(final Class<T> targetType, final Object... args) {
        final Constructor<T> targetConstructor = findDeclaredConstructor(targetType, args);

        assert targetConstructor != null;
        targetConstructor.setAccessible(true);

        try {
            return targetConstructor.newInstance(args);
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Class<?> loadClass(final Package from, final String... names) {
        for (final String possibleName : names)
            try {
                return Class.forName(from.getName() + "." + possibleName);
            } catch (ClassNotFoundException e) {

            }

        return null;
    }

    private static boolean argsMatch(final Class<?>[] types, final Object[] args) {
        if (types.length != args.length)
            return false;

        for (int i = 0; i < args.length; ++i)
            if (isNotSoftAssignable(types[i], args[i]))
                return false;

        return true;
    }

    private static boolean isNotSoftAssignable(final Class<?> type, final Object arg) {
        return (arg == null && type.isPrimitive()) || (arg != null && !type.isAssignableFrom(arg.getClass()) && !isBoxedPrimitive(type, arg.getClass()));
    }

    private static boolean isBoxedPrimitive(final Class<?> primitive, final Class<?> objectType) {
        return (primitive == boolean.class && objectType == Boolean.class) ||
                (primitive == byte.class && objectType == Byte.class) ||
                (primitive == short.class && objectType == Short.class) ||
                (primitive == int.class && objectType == Integer.class) ||
                (primitive == long.class && objectType == Long.class) ||
                (primitive == float.class && objectType == Float.class) ||
                (primitive == double.class && objectType == Double.class);
    }

    private interface DeclarationGetter<T, R> {
        R[] getDeclared(final Class<? super T> t);
    }

    private interface NameGetter<T> {
        String getName(final T t);
    }
}
