package com.bignerdranch.franklin.roger;

import java.io.Serializable;

import com.bignerdranch.franklin.roger.LocalApk;

import android.app.Activity;

import android.content.res.Resources;

import android.util.Log;

public class LayoutDescription implements Serializable {
    public static final String TAG = "LayoutDescription";

    public static final long serialVersionUID = 0l;

    String apkPath;
    String layoutName;
    String packageName;
    transient LocalApk apk;
    transient Resources resources;

    public String getApkPath() {
        return this.apkPath;
    }

    public void setApkPath(String apkPath) {
        this.apkPath = apkPath;
    }

    public String getLayoutName() {
        return this.layoutName;
    }

    public void setLayoutName(String layoutName) {
        this.layoutName = layoutName;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Resources getResources(Activity a) {
        if (resources != null) return resources;

        resources = getApk(a).getResources();
        Log.i(TAG, "Did it work? " + getApk(a).getFile().exists() + "");

        return resources;
    }

    public int getResId(Activity a) {
        Log.i(TAG, "getting identifier for layoutName " + layoutName + ", packageName " + packageName + "");
        int id = getResources(a).getIdentifier(layoutName, "layout", packageName);
        Log.i(TAG, "here's what we got: " + id + "");
        return id;
    }

    public LocalApk getApk(Activity activity) {
        if (apk != null) return apk;

    	Log.d(TAG, "Loading apk with path: " + apkPath + " layout: " + layoutName + " package: " + packageName);
        apk = new FileApk(activity, packageName, apkPath);

        return apk;
    }
}
