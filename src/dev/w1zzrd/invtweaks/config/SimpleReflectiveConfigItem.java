package dev.w1zzrd.invtweaks.config;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SimpleReflectiveConfigItem implements ConfigurationSerializable {

    public SimpleReflectiveConfigItem(final Map<String, Object> mappings) {
        deserializeMapped(mappings);
    }

    @Override
    public Map<String, Object> serialize() {
        final HashMap<String, Object> values = new HashMap<>();
        Arrays.stream(getClass().getDeclaredFields())
                .filter(it -> !Modifier.isTransient(it.getModifiers()) && !Modifier.isStatic(it.getModifiers()))
                .forEach(it -> {
                    try {
                        it.setAccessible(true);
                        values.put(it.getName(), it.get(this));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                });
        return values;
    }

    private void deserializeMapped(final Map<String, Object> mappings) {
        for (final Field field : getClass().getDeclaredFields()) {
            if (Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers()))
                continue;

            try {
                if (mappings.containsKey(field.getName()))
                    parse(mappings.get(field.getName()), field, this);
            } catch (IllegalAccessException | InvocationTargetException e) {
                // This shouldn't happen
                e.printStackTrace();
            }
        }
    }

    private static void parse(final Object value, final Field field, final Object instance) throws IllegalAccessException, InvocationTargetException {
        field.setAccessible(true);

        if (field.getType().isPrimitive() && value == null)
            throw new NullPointerException("Attempt to assign null to a primitive field");

        final Class<?> boxed = getBoxedType(field.getType());

        if (boxed.isAssignableFrom(value.getClass())) {
            field.set(instance, value);
            return;
        }

        if (value instanceof String) {
            final Method parser = locateParser(boxed, field.getType().isPrimitive() ? field.getType() : null);
            if (parser != null)
                field.set(instance, parser.invoke(null, value));
        }

        throw new IllegalArgumentException(String.format("No defined parser for value \"%s\"", value));
    }

    private static Class<?> getBoxedType(final Class<?> cls) {
        if (cls == int.class) return Integer.class;
        else if (cls == double.class) return Double.class;
        else if (cls == long.class) return Long.class;
        else if (cls == float.class) return Float.class;
        else if (cls == char.class) return Character.class;
        else if (cls == byte.class) return Byte.class;
        else if (cls == short.class) return Short.class;
        else if (cls == boolean.class) return Boolean.class;
        else return cls;
    }

    private static Method locateParser(final Class<?> cls, final Class<?> prim) {
        for (final Method method : cls.getDeclaredMethods()) {
            final Class<?>[] params = method.getParameterTypes();
            if (method.getName().startsWith("parse" + cls.getSimpleName()) &&
                    Modifier.isStatic(method.getModifiers()) &&
                    method.getReturnType().equals(prim != null ? prim : cls) &&
                    params.length == 1 && params[0].equals(String.class))
                method.setAccessible(true);
                return method;
        }

        return null;
    }
}
