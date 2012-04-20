package com.bignerdranch.franklin.roger;

import java.io.Serializable;

import com.bignerdranch.franklin.roger.LocalApk;

import android.content.Context;

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

    public Resources getResources(Context c) {
        if (resources != null) return resources;

        resources = getApk(c).getResources();
        Log.i(TAG, "Did it work? " + getApk(c).getFile().exists() + "");

        return resources;
    }

    public int getResId(Context c) {
        Log.i(TAG, "getting identifier for layoutName " + layoutName + ", packageName " + packageName + "");
        int id = getResources(c).getIdentifier(layoutName, "layout", packageName);
        Log.i(TAG, "here's what we got: " + id + "");
        return id;
    }

    public LocalApk getApk(Context context) {
        if (apk != null) return apk;

    	Log.d(TAG, "Loading apk with path: " + apkPath + " layout: " + layoutName + " package: " + packageName);
        apk = new FileApk(context, packageName, apkPath);

        return apk;
    }
}
