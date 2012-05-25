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
                // well what if we don't use a theme for our own app?
                // call to populate mThemeResource
                activity.getTheme();
                Object v = Rxn.getFieldValue(activity, "mThemeResource");
                int resId = (int)(Integer)v;
                Log.i(TAG, "getDefaultThemeResource: " + resId + "");
                return resId;
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
                Method selectDefaultTheme = Rxn.getMethod(Resources.class, 
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

        //public Resources.Theme getTheme() {
        //    if (Build.VERSION.SDK_INT < 11) {
        //        return getThemeGingerbread();
        //    } else {
        //        return getThemePostAPI10();
        //    }
        //}

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
        	Constructor<AssetManager> con = amClass.getConstructor();
            
            Method m = Rxn.getMethod(amClass, "addAssetPath", String.class);
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

    public LayoutInflater getLayoutInflaterHackingAroundCrap(LayoutInflater original, Context c) {
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
        return getLayoutInflaterHackingAroundCrap(original, c);
    }

    //public LoadedApk(ActivityThread activityThread, String name,
    //        Context systemContext, ApplicationInfo info, CompatibilityInfo compatInfo) {

    public Context createPackageContext(Resources resources) {
        try {
            String contextImplName = "android.app.ContextImpl";
            String activityThreadName = "android.app.ActivityThread";
            String loadedApkName = "android.app.LoadedApk";
            String compatibilityInfoName = "android.content.res.CompatibilityInfo";

            ClassLoader cl = ClassLoader.getSystemClassLoader();
            Class<?> contextImplClass = (Class<?>)cl.loadClass(contextImplName);
            Class<?> activityThreadClass = (Class<?>)cl.loadClass(activityThreadName);

            Log.i(TAG, "getting constructor and method");
            Constructor<?> contextImplConstructor = Rxn.getConstructor(contextImplClass);
            Method init = Rxn.getMethod(contextImplClass, "init", Resources.class, activityThreadClass);

            Log.i(TAG, "getting the activity thread");
            Context baseContext = findBaseContext(context);
            Field field = Rxn.getField(baseContext.getClass(), "mMainThread");
            Object activityThread = field.get(baseContext);

            Log.i(TAG, "got everything, it looks like");
            Context c = (Context)contextImplConstructor.newInstance();
            init.invoke(c, resources, activityThread);

            // more setup to create a fake LoadedApk for mPackageInfo
            Class<?> loadedApkClass = (Class<?>)cl.loadClass(loadedApkName);
            Class<?> compatInfoClass = (Class<?>)cl.loadClass(compatibilityInfoName);
            Constructor<?> loadedApkConstructor = Rxn.getConstructor(loadedApkClass, 
                    activityThreadClass, String.class, Context.class, ApplicationInfo.class, compatInfoClass);

            // find some compat info to use
            Object rogerPackageInfo = Rxn.getFieldValue(baseContext, "mPackageInfo");
            Object compatInfoHolder = Rxn.getFieldValue(rogerPackageInfo, "mCompatibilityInfo");
            Object compatInfo = Rxn.invoke(compatInfoHolder, "get");

            // get resId
            Method getThemeResId = Rxn.getMethod(activity.getClass(), "getThemeResId");
            int themeResId = (int)(Integer)getThemeResId.invoke(activity);

            Context finalFakeContext = new LocalApkContext(c, themeResId);
            
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
}
