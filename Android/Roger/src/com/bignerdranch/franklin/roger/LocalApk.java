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
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.WindowManager;

public abstract class LocalApk {
    private class LocalApkContext extends ContextThemeWrapper {
        private LocalApkContext(Context c, int themeRes) {
            super(c, themeRes);
        }

        @Override
        public Object getSystemService(String name) {
            try {
                return super.getSystemService(name);
            } catch (RuntimeException e) {
                throw e;
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
                Object value = Rxn.getFieldValue(containingInfo, "nativeLibraryDir");
                Rxn.setFieldValue(info, "nativeLibraryDir", value);
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
            Log.i(TAG, "creating AssetManager");
        	Constructor<AssetManager> con = amClass.getConstructor();
            
            Method m = Rxn.getMethod(amClass, "addAssetPath", String.class);
            AssetManager am = con.newInstance();

            Log.i(TAG, "adding asset path to AssetManager");
            m.invoke(am, getFile().getPath());

            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager wm = (WindowManager)context
                .getSystemService(Context.WINDOW_SERVICE);
            wm.getDefaultDisplay().getMetrics(metrics);
            
            Configuration configuration = context.getResources()
                .getConfiguration();

            Log.i(TAG, "creating resources");
            resources = new Resources(am, metrics, configuration);
        } catch (Exception e) {
            Log.i(TAG, "failure to do something or another", e);
        }
        
        return resources;
    }

    private Context findBaseContext(Context c) {
        if (c instanceof ContextWrapper) {
            Field f = Rxn.getField(ContextWrapper.class, "mBase");
            try {
                return findBaseContext((Context)f.get(c));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            return c;
        }
    }

    public LayoutInflater getLayoutInflaterSettingPrivateContext(LayoutInflater original, Context c) {
        LayoutInflater inflater = getLayoutInflaterUsingCloneInContext(original, c);

        Rxn.setFieldValue(inflater, "mContext", c);

        return inflater;
    }

    public LayoutInflater getLayoutInflaterUsingCloneInContext(LayoutInflater original, Context c) {
        Log.i(TAG, "and calling getApplicationInfo");

        LayoutInflater inflater = original.cloneInContext(c);
        Rxn.showAllFields(inflater);

        return inflater;
    }

    public LayoutInflater getLayoutInflater(LayoutInflater original) {
        Resources r = getResources();
        Context c = createPackageContext(r);
        return getLayoutInflaterSettingPrivateContext(original, c);
    }

    private Context createPackageContextICS(Resources resources) {
        try {
            String contextImplName = "android.app.ContextImpl";
            String activityThreadName = "android.app.ActivityThread";
            String loadedApkName = "android.app.LoadedApk";
            String compatibilityInfoName = "android.content.res.CompatibilityInfo";

            ClassLoader cl = ClassLoader.getSystemClassLoader();
            Class<?> contextImplClass = (Class<?>)cl.loadClass(contextImplName);
            Class<?> activityThreadClass = (Class<?>)cl.loadClass(activityThreadName);

            Log.i(TAG, "getting constructor and method");
            Constructor<?> contextImplConstructor = Rxn.getConstructorThrows(contextImplClass);
            Method init = Rxn.getMethodThrows(contextImplClass, "init", Resources.class, activityThreadClass);

            Log.i(TAG, "getting the activity thread");
            Context baseContext = findBaseContext(context);
            Field field = Rxn.getField(baseContext.getClass(), "mMainThread");
            Object activityThread = field.get(baseContext);

            Log.i(TAG, "got everything, it looks like");
            Context c = (Context)contextImplConstructor.newInstance();
            init.invoke(c, resources, activityThread);

            // get resId
            Method getThemeResId = Rxn.getMethod(activity.getClass(), "getThemeResId");
            int themeResId = (int)(Integer)getThemeResId.invoke(activity);

            Context finalFakeContext = new LocalApkContext(c, themeResId);
            
            // more setup to create a fake LoadedApk for mPackageInfo
            Class<?> loadedApkClass = (Class<?>)cl.loadClass(loadedApkName);
            Class<?> compatInfoClass = (Class<?>)cl.loadClass(compatibilityInfoName);
            Constructor<?> loadedApkConstructor = Rxn.getConstructorThrows(loadedApkClass, 
                    activityThreadClass, String.class, Context.class, ApplicationInfo.class, compatInfoClass);

            // find some compat info to use
            Object rogerPackageInfo = Rxn.getFieldValue(baseContext, "mPackageInfo");
            Object compatInfoHolder = Rxn.getFieldValue(rogerPackageInfo, "mCompatibilityInfo");
            Object compatInfo = Rxn.invoke(compatInfoHolder, "get");

            // construct fake LoadedApk
            Object fakeLoadedApk = loadedApkConstructor.newInstance(
                    activityThread, packageName, c, finalFakeContext.getApplicationInfo(), compatInfo);

            // then set it up on our fake system context to make it *not* a system
            // context? ooh, this is skeevy.
            Rxn.setFieldValue(c, "mPackageInfo", fakeLoadedApk);

            return finalFakeContext;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private Context createPackageContextHoneycomb(Resources resources) {
        try {
            String contextImplName = "android.app.ContextImpl";
            String activityThreadName = "android.app.ActivityThread";
            String loadedApkName = "android.app.LoadedApk";

            ClassLoader cl = ClassLoader.getSystemClassLoader();
            Class<?> contextImplClass = (Class<?>)cl.loadClass(contextImplName);
            Class<?> activityThreadClass = (Class<?>)cl.loadClass(activityThreadName);

            Log.i(TAG, "getting constructor and method");
            Constructor<?> contextImplConstructor = Rxn.getConstructorThrows(contextImplClass);
            Method init = Rxn.getMethodThrows(contextImplClass, "init", Resources.class, activityThreadClass);

            Log.i(TAG, "getting the activity thread");
            Context baseContext = findBaseContext(context);
            Field field = Rxn.getField(baseContext.getClass(), "mMainThread");
            Object activityThread = field.get(baseContext);

            Log.i(TAG, "got everything, it looks like");
            Context c = (Context)contextImplConstructor.newInstance();
            init.invoke(c, resources, activityThread);

            // get resId
            Method getThemeResId = Rxn.getMethod(activity.getClass(), "getThemeResId");
            int themeResId = (int)(Integer)getThemeResId.invoke(activity);

            Context finalFakeContext = new LocalApkContext(c, themeResId);
            
            // more setup to create a fake LoadedApk for mPackageInfo
            Class<?> loadedApkClass = (Class<?>)cl.loadClass(loadedApkName);
            Constructor<?> loadedApkConstructor = Rxn.getConstructor(loadedApkClass, 
                    activityThreadClass, String.class, Context.class, ApplicationInfo.class);
            Object fakeLoadedApk = loadedApkConstructor.newInstance(
                    activityThread, packageName, c, finalFakeContext.getApplicationInfo());

            // then set it up on our fake system context to make it *not* a system
            // context? ooh, this is skeevy.
            Rxn.setFieldValue(c, "mPackageInfo", fakeLoadedApk);

            return finalFakeContext;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private Context createPackageContextFroyo(Resources resources) {
        try {
            Log.i(TAG, "creating froyo package context");
            String contextImplName = "android.app.ContextImpl";
            String activityThreadName = "android.app.ActivityThread";

            ClassLoader cl = ClassLoader.getSystemClassLoader();
            Class<?> contextImplClass = (Class<?>)cl.loadClass(contextImplName);
            Class<?> activityThreadClass = (Class<?>)cl.loadClass(activityThreadName);

            Log.i(TAG, "getting constructor and method");
            Constructor<?> contextImplConstructor = Rxn.getConstructorThrows(contextImplClass);
            Method init = Rxn.getMethodThrows(contextImplClass, "init", Resources.class, activityThreadClass);

            Log.i(TAG, "getting the activity thread");
            Context baseContext = findBaseContext(context);
            Field field = Rxn.getField(baseContext.getClass(), "mMainThread");
            Object activityThread = field.get(baseContext);

            Log.i(TAG, "got everything, it looks like");
            Context c = (Context)contextImplConstructor.newInstance();
            init.invoke(c, resources, activityThread);

            // get resId
            int themeResId = (int)(Integer)Rxn.getFieldValue(activity, "mThemeResource");

            Context finalFakeContext = new LocalApkContext(c, themeResId);
            
            // force our fake system context to have a layout inflater
            LayoutInflater originalInflater = activity.getLayoutInflater();
            LayoutInflater inflater = getLayoutInflaterSettingPrivateContext(originalInflater, finalFakeContext);
            Rxn.setFieldValue(c, "mLayoutInflater", inflater);

            return finalFakeContext;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private Context createPackageContextGingerbread(Resources resources) {
        try {
            String contextImplName = "android.app.ContextImpl";
            String activityThreadName = "android.app.ActivityThread";
            String loadedApkName = "android.app.LoadedApk";

            ClassLoader cl = ClassLoader.getSystemClassLoader();
            Class<?> contextImplClass = (Class<?>)cl.loadClass(contextImplName);
            Class<?> activityThreadClass = (Class<?>)cl.loadClass(activityThreadName);

            Log.i(TAG, "getting constructor and method");
            Constructor<?> contextImplConstructor = Rxn.getConstructorThrows(contextImplClass);
            Method init = Rxn.getMethodThrows(contextImplClass, "init", Resources.class, activityThreadClass);

            Log.i(TAG, "getting the activity thread");
            Context baseContext = findBaseContext(context);
            Field field = Rxn.getField(baseContext.getClass(), "mMainThread");
            Object activityThread = field.get(baseContext);

            Log.i(TAG, "got everything, it looks like");
            Context c = (Context)contextImplConstructor.newInstance();
            init.invoke(c, resources, activityThread);

            // get resId
            int themeResId = (int)(Integer)Rxn.getFieldValue(activity, "mThemeResource");

            Context finalFakeContext = new LocalApkContext(c, themeResId);
            
            // more setup to create a fake LoadedApk for mPackageInfo
            Class<?> loadedApkClass = (Class<?>)cl.loadClass(loadedApkName);
            Constructor<?> loadedApkConstructor = Rxn.getConstructor(loadedApkClass, 
                    activityThreadClass, String.class, Context.class, ApplicationInfo.class);
            Object fakeLoadedApk = loadedApkConstructor.newInstance(
                    activityThread, packageName, c, finalFakeContext.getApplicationInfo());

            // then set it up on our fake system context to make it *not* a system
            // context? ooh, this is skeevy.
            Rxn.setFieldValue(c, "mPackageInfo", fakeLoadedApk);

            return finalFakeContext;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Create a package context off of an apk that is not actually installed. 
     *
     * The following code is extremely skeevy.
     */
    public Context createPackageContext(Resources resources) {
        switch (Build.VERSION.SDK_INT) {
            case Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1:
            case Build.VERSION_CODES.ICE_CREAM_SANDWICH:
            default:
                return createPackageContextICS(resources);
            case Build.VERSION_CODES.HONEYCOMB_MR2:
            case Build.VERSION_CODES.HONEYCOMB_MR1:
            case Build.VERSION_CODES.HONEYCOMB:
                return createPackageContextHoneycomb(resources);
            case Build.VERSION_CODES.GINGERBREAD_MR1:
            case Build.VERSION_CODES.GINGERBREAD:
                return createPackageContextGingerbread(resources);
            case Build.VERSION_CODES.FROYO:
            case Build.VERSION_CODES.ECLAIR_MR1:
            case Build.VERSION_CODES.ECLAIR_0_1:
            case Build.VERSION_CODES.ECLAIR:
            case Build.VERSION_CODES.DONUT:
            case Build.VERSION_CODES.CUPCAKE:
            case Build.VERSION_CODES.BASE_1_1:
            case Build.VERSION_CODES.BASE:
                return createPackageContextFroyo(resources);
        }
    }
}
