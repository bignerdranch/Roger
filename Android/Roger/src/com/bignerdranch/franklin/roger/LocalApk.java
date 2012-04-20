package com.bignerdranch.franklin.roger;

import java.io.File;

import java.lang.ClassLoader;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.ContextWrapper;

import android.content.pm.ApplicationInfo;

import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;

import android.os.Build;

import android.util.DisplayMetrics;
import android.util.Log;

import android.view.LayoutInflater;
import android.view.WindowManager;

public abstract class LocalApk {
    private class LocalApkContext extends ContextWrapper {
        private LocalApkContext(Context c) {
            super(c);
        }

        Resources.Theme theme = null;
        int themeResource = 0;

        @Override
        public Object getSystemService(String name) {
            try {
                return super.getSystemService(name);
            } catch (RuntimeException e) {
                throw e;
            }
        }

        public int getThemeResId() {
            return themeResource;
        }

        public Resources.Theme getTheme() {
            if (theme == null) {
                Method selectDefaultTheme = getMethod(Resources.class, 
                        "selectDefaultTheme", int.class, int.class);
                try {
                    themeResource = (int)(Integer)selectDefaultTheme.invoke(null, 
                            themeResource, getApplicationInfo().targetSdkVersion);
                    theme = getResources().newTheme();
                    theme.applyStyle(themeResource, true);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return theme;
        }

        @Override
        public String getPackageName() {
            return packageName;
        }

        @Override
        public ApplicationInfo getApplicationInfo() {
            ApplicationInfo containingInfo = context.getApplicationInfo();

            ApplicationInfo info = new ApplicationInfo();
            info.sourceDir = getFile().getParent();
            info.packageName = packageName;
            info.uid = containingInfo.uid;
            info.sharedLibraryFiles = containingInfo.sharedLibraryFiles;
            info.dataDir = containingInfo.dataDir;
            info.targetSdkVersion = containingInfo.targetSdkVersion;

            if (Build.VERSION.SDK_INT >= 9) {
                Object value = getFieldValue(containingInfo, "nativeLibraryDir");
                setFieldValue(info, "nativeLibraryDir", value);
            }

            return info;
        }
    }

    public static final String TAG = "LocalApk";

    protected Context context;
    protected String packageName;

    public LocalApk(Context c, String packageName) {
        this.context = c.getApplicationContext();
        this.packageName = packageName;
    }

    protected File getFile() {
        return null;
    }

    public Resources getResources() {
        Class<AssetManager> amClass = AssetManager.class;
        Resources resources = null;
        
        try {
        	Constructor<AssetManager> con = amClass.getConstructor();
            
            Method m = amClass.getMethod("addAssetPath", String.class);
            AssetManager am = con.newInstance();

            m.invoke(am, getFile().getPath());

            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager wm = (WindowManager)context
                .getSystemService(Context.WINDOW_SERVICE);
            wm.getDefaultDisplay().getMetrics(metrics);
            
            Configuration configuration = context.getResources()
                .getConfiguration();

            resources = new Resources(am, metrics, configuration);
        } catch (Exception e) {
            Log.i(TAG, "failure to do something or another", e);
        }
        
        return resources;
    }

    private void scan(Class<?> klass) {
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

    private boolean areEqual(Class<?>[] a, Class<?>[] b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.length != b.length) return false;

        boolean equal = true;
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(b[i])) {
                equal = false;
                break;
            }
        }

        return equal;
    }

    private Context findBaseContext(Context c) {
        if (c instanceof ContextWrapper) {
            Field f = getField(ContextWrapper.class, "mBase");
            try {
                return findBaseContext((Context)f.get(c));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            return c;
        }
    }

    private Method getMethod(Class<?> klass, String name, Class<?>... paramTypes) {
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

        throw new RuntimeException("failed to find " + name);
    }

    private Constructor<?> getConstructor(Class<?> klass, Class<?>... paramTypes) {
        for (Constructor<?> constructor : klass.getDeclaredConstructors()) {
            if (areEqual(paramTypes, constructor.getParameterTypes())) {
                constructor.setAccessible(true);
                return constructor;
            }
        }

        throw new RuntimeException("failed to find constructor");
    }

    private void setFieldValue(Object o, String name, Object value) {
        Field f = getField(o.getClass(), name);
        try {
            f.set(o, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object getFieldValue(Object o, String name) {
        Field f = getField(o.getClass(), name);
        try {
            return f.get(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Field getField(Class<?> klass, String name) {
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

    public LayoutInflater getLayoutInflater(LayoutInflater original) {
        Resources r = getResources();
        Context c = createPackageContext(r);
        Log.i(TAG, "and calling getApplicationInfo");

        return original.cloneInContext(c);

        //Constructor<?> constructor = getConstructor(LayoutInflater.class, LayoutInflater.class, Context.class);
        //try {
        //    Log.i(TAG, "constructor args: ");
        //    for (Class<?> klass : constructor.getParameterTypes()) {
        //        Log.i(TAG, "    " + klass +"");
        //    }
        //    Log.i(TAG, "is it accessible? " + constructor.isAccessible() + "");
        //    //return (LayoutInflater)constructor.newInstance(c);
        //} catch (Exception e) {
        //    Log.e(TAG, "failed to construct layoutinflater", e);
        //    throw new RuntimeException(e);
        //}
    }

    public Context createPackageContext(Resources resources) {
        try {
            String contextImplName = "android.app.ContextImpl";
            String activityThreadName = "android.app.ActivityThread";

            ClassLoader cl = ClassLoader.getSystemClassLoader();
            Class<?> contextImplClass = (Class<?>)cl.loadClass(contextImplName);
            Class<?> activityThreadClass = (Class<?>)cl.loadClass(activityThreadName);

            Log.i(TAG, "getting constructor and method");
            Constructor<?> contextImplConstructor = getConstructor(contextImplClass);
            Method init = getMethod(contextImplClass, "init", Resources.class, activityThreadClass);

            Log.i(TAG, "getting the activity thread");
            Context baseContext = findBaseContext(context);
            Field field = getField(baseContext.getClass(), "mMainThread");
            Object activityThread = field.get(baseContext);

            Log.i(TAG, "got everything, it looks like");
            Context c = (Context)contextImplConstructor.newInstance();
            init.invoke(c, resources, activityThread);

            return new LocalApkContext(c);

            //if (c.mResources != null) {
            //}

            //// Should be a better exception.
            //throw new PackageManager.NameNotFoundException(
            //    "Application package " + packageName + " not found");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    
}
