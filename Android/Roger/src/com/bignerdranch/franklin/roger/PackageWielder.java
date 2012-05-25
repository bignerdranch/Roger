package com.bignerdranch.franklin.roger;

import java.io.File;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import android.content.Context;

import android.content.pm.PackageManager;

import android.net.Uri;

import android.util.Log;

public class PackageWielder {
    private static final String TAG = "PackageWielder";

    // magic code words:
    //import android.content.pm.IPackageInstallObserver;
    //    public static final int INSTALL_REPLACE_EXISTING = 0x00000002;
    //    public abstract void installPackage(
    //            Uri packageURI, IPackageInstallObserver observer, int flags,
    //            String installerPackageName);
    //            

    public static class ProxyIPackageInstallObserver implements InvocationHandler {
        public Object invoke(Object proxy, Method method, Object[] args) {
            // void packageInstalled(in String packageName, int returnCode);
            if (!"packageInstalled".equals(method)) {
                Log.i(TAG, "wtf is this: " + method + "");
            } else {
                String packageName = (String)args[0];
                int returnCode = (int)(Integer)args[1];
                Log.i(TAG, "installed packageName: " + packageName + ", returnCode: " + returnCode + "");
            }
            return null;
        }
    }

    public static void tryInstallPackage(Context c, File apkFile) {
        PackageManager pm = c.getPackageManager();
        Uri uri = Uri.fromFile(apkFile);
        Class<?> installObserverClass = Rxn.getReflectedClass("android.content.pm.IPackageInstallObserver");
        Log.i(TAG, "ooh i got an installObserver");
        int INSTALL_REPLACE_EXISTING = (int)(Integer)Rxn.getFieldValue(pm, "INSTALL_REPLACE_EXISTING");
        Log.i(TAG, "aaand here's my constant: " + INSTALL_REPLACE_EXISTING + "");

        Rxn.scan(pm.getClass());
        Log.i(TAG, "getting the method");
        Method m = Rxn.getMethod(pm.getClass(), "installPackage", 
                Uri.class, installObserverClass, int.class, String.class);

        Log.i(TAG, "creating a proxy object...");
        Class<?>[] interfaces = new Class<?>[] { installObserverClass };
        Object installObserverProxy = Proxy.newProxyInstance(
                installObserverClass.getClassLoader(), interfaces, new ProxyIPackageInstallObserver());

        try {
            Log.i(TAG, "invoking package install...............!");
            m.invoke(pm, uri, installObserverProxy, INSTALL_REPLACE_EXISTING, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
