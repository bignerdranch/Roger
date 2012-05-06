package com.bignerdranch.franklin.roger;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.app.Activity;
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

        private int getDefaultThemeResource() {
            try {
                Class<?> c = getReflectedClass("com.android.internal.R.style");
                Object v = getFieldValue(c, "Theme");
                return (int)(Integer)(v);
            } catch (Exception ex) {
                Log.i(TAG, "failed to get default theme resource", ex);
                return 0;
            }
        }

        private Resources.Theme getThemeGingerbread() {
            // would be really awesome to be able to fix the default
            // theme handling here. -BP
            //
            // corresponding code from android source:
            //
            //if (mThemeResource == 0) {
            //    mThemeResource = com.android.internal.R.style.Theme;
            //}
            //mTheme = mResources.newTheme();
            //mTheme.applyStyle(mThemeResource, true);
            //

            if (theme == null) {
                if (themeResource == 0) {
                    themeResource = getDefaultThemeResource();
                }
                theme = getResources().newTheme();
                theme.applyStyle(0, true);
            }

            return theme;
        }
        

        private Resources.Theme getThemePostAPI10() {
            // hack around it?
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

        public Resources.Theme getTheme() {
            if (Build.VERSION.SDK_INT < 11) {
                return getThemeGingerbread();
            } else {
                return getThemePostAPI10();
            }
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
            //info.sharedLibraryFiles = containingInfo.sharedLibraryFiles;
            //info.dataDir = containingInfo.dataDir;
            info.targetSdkVersion = containingInfo.targetSdkVersion;

            info.className = android.app.Application.class.getName();;
            info.descriptionRes = 0;
            info.theme = 0; // populate later on from apk?
            info.manageSpaceActivityName = null;
            info.flags = 0;
            info.publicSourceDir = info.sourceDir;
            info.sharedLibraryFiles = null;
            info.dataDir = null;
            info.enabled = false;

            // newer stuff? not sure if we need it
            //info.backupAgentName = null;
            //info.resourceDirs = null;
            //info.nativeLibraryDir = null;
            //info.installLocation = PackageInfo.INSTALL_LOCATION_UNSPECIFIED;

            if (Build.VERSION.SDK_INT >= 9) {
                Object value = getFieldValue(containingInfo, "nativeLibraryDir");
                setFieldValue(info, "nativeLibraryDir", value);
            }

            return info;
        }
    }

    public static final String TAG = "LocalApk";

    protected Activity activity;
    protected Context context;
    protected String packageName;

    public LocalApk(Activity a, String packageName) {
        this.activity = a;
        this.context = a.getApplicationContext();
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

    public void showAllFields(Object instance) {
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

    private <C> Constructor<C> getConstructor(Class<C> klass, Class<?>... paramTypes) {
        for (Constructor<C> constructor : klass.getDeclaredConstructors()) {
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

    private Object getFieldValue(Class<?> klass, String name) {
        Field f = getField(klass, name);
        try {
            return f.get(null);
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

    public LayoutInflater getLayoutInflaterHackingAroundCrap(LayoutInflater original, Context c) {
        LayoutInflater inflater = getLayoutInflaterUsingCloneInContext(original, c);

        setFieldValue(inflater, "mContext", c);

        return inflater;
    }

    public LayoutInflater getLayoutInflaterUsingCloneInContext(LayoutInflater original, Context c) {
        Log.i(TAG, "and calling getApplicationInfo");

        LayoutInflater inflater = original.cloneInContext(c);
        showAllFields(inflater);

        return inflater;
    }

    public LayoutInflater getLayoutInflater(LayoutInflater original) {
        Resources r = getResources();
        Context c = createPackageContext(r);
        return getLayoutInflaterHackingAroundCrap(original, c);
    }

    private Class<?> getReflectedClass(String name) throws ClassNotFoundException {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        return cl.loadClass(name);
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
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    
}
