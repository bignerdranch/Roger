package com.bignerdranch.franklin.roger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.util.Log;

public class Rxn {
    private static final String TAG = "Rxn";

    public static class Null {
        public Class<?> type;

        public Null(Class<?> type) {
            this.type = type;
        }
    }

    public static void showAllFields(Object instance) {
        try {
        Class<?> klass = instance.getClass();

        Log.i(TAG, "showing everything for instance: " + instance + "");
        while (klass != null) {
            Log.i(TAG, "   for class: " + klass.getName() + "");
            for (Field field : klass.getDeclaredFields()) {
                field.setAccessible(true);
                
                Log.i(TAG, "    field name: " + field.getName() + " class: " + field.getType().getName() + 
                        "\n        value: " + field.get(instance) + "");
            }

            klass = klass.getSuperclass();
        }
        } catch (Exception ex) {
            Log.i(TAG, "exception while showing all fields", ex);
        }
    }

    // debugging
    public static void scan(Class<?> klass) {
        Log.i(TAG, "scanning " + klass + "");
        Log.i(TAG, "    constructors:");
        for (Constructor<?> constructor : klass.getDeclaredConstructors()) {
            Log.i(TAG, "        " + constructor + "");
        }
        Log.i(TAG, "    fields:");
        for (Field field : klass.getDeclaredFields()) {
            Log.i(TAG, "        " + field + "");
        }
        Log.i(TAG, "    methods:");
        for (Method method : klass.getDeclaredMethods()) {
            Log.i(TAG, "        " + method + "");
        }
        if (klass.getSuperclass() != null) {
            scan(klass.getSuperclass());
        }
    }

    public static boolean areEqual(Class<?>[] a, Class<?>[] b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.length != b.length) return false;

        boolean equal = true;
        for (int i = 0; i < a.length; i++) {
            if (!a[i].getName().equals(b[i].getName())) {
                equal = false;
                break;
            }
        }

        return equal;
    }

    public static Object invoke(Object target, String methodName, Object... params) {
        Class<?>[] paramTypes = new Class<?>[params.length];

        for (int i = 0; i < params.length; i++) {
            if (params[i] instanceof Null) {
                paramTypes[i] = ((Null)params[i]).type;
                params[i] = null;
            } else if (params[i] == null) {
                throw new RuntimeException("Invalid argument. For null args, pass in Null(Class<?>) with arg type.");
            } else {
                paramTypes[i] = params[i].getClass();
            }
        }

        Method method = getMethod(target.getClass(), methodName, paramTypes);

        try {
            return method.invoke(target, params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Method getMethod(Class<?> klass, String name, Class<?>... paramTypes) {
        for (Method method : klass.getDeclaredMethods()) {
            if (!method.getName().equals(name)) {
                continue;
            }

            Class<?>[] methodParamTypes = method.getParameterTypes();

            if (areEqual(methodParamTypes, paramTypes)) {
                method.setAccessible(true);
                return method;
            }
        }

        if (klass.getSuperclass() != null) {
            return getMethod(klass.getSuperclass(), name, paramTypes);
        }

        throw new RuntimeException("failed to find " + name);
    }

    public static <C> Constructor<C> getConstructor(Class<C> klass, Class<?>... paramTypes) {
        for (Constructor<C> constructor : klass.getDeclaredConstructors()) {
            if (areEqual(paramTypes, constructor.getParameterTypes())) {
                constructor.setAccessible(true);
                return constructor;
            }
        }

        throw new RuntimeException("failed to find constructor");
    }

    public static void setFieldValue(Object o, String name, Object value) {
        Field f = getField(o.getClass(), name);
        try {
            f.set(o, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getFieldValue(Class<?> klass, String name) {
        Field f = getField(klass, name);
        try {
            return f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getFieldValue(Object o, String name) {
        Field f = getField(o.getClass(), name);
        try {
            return f.get(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Field getField(Class<?> klass, String name) {
        for (Field field : klass.getDeclaredFields()) {
            if (field.getName().equals(name)) {
                field.setAccessible(true);
                return field;
            }
        }

        Class<?> superclass = klass.getSuperclass();
        if (superclass != null) {
            return getField(superclass, name);
        }

        throw new RuntimeException("failed to find field " + name);
    }

    public static Class<?> getReflectedClass(String name) {
        try {
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            return cl.loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
