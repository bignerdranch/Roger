package com.bignerdranch.franklin.roger;

import java.io.Serializable;

import com.bignerdranch.franklin.roger.LocalApk;

import android.app.Activity;

import android.content.res.Resources;

import android.util.Log;

public class LayoutDescription implements Serializable {
    public static final String TAG = "LayoutDescription";

    public static final long serialVersionUID = 0l;

    String identifier;
    String apkPath;
    String layoutName;
    String layoutType;
    String packageName;
    int minVersion;
    int txnId;

    transient LocalApk apk;
    transient Resources resources;

    public LayoutDescription() {
    }

    public LayoutDescription(String identifier, String apkPath, String layoutName, String layoutType, String packageName, int minVersion, int txnId) {
        this.identifier = identifier;
        this.apkPath = apkPath;
        this.layoutName = layoutName;
        this.layoutType = layoutType;
        this.packageName = packageName;
        this.minVersion = minVersion;
        this.txnId = txnId;
    }

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

    public String getLayoutType() {
        return this.layoutType;
    }

    public void setLayoutType(String layoutType) {
        this.layoutType = layoutType;
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
        Log.i(TAG, "getting identifier for layoutName " + layoutName + ", type " + layoutType + ", packageName " + packageName + "");
        int id = getResources(a).getIdentifier(layoutName, layoutType, packageName);
        Log.i(TAG, "here's what we got: " + id + "");
        return id;
    }

    public LocalApk getApk(Activity activity) {
        if (apk != null) return apk;

        Log.d(TAG, "Loading apk with path: " + apkPath + " layout: " + layoutName + " package: " + packageName);
        apk = new FileApk(activity, packageName, apkPath);

        return apk;
    }

    public int getMinVersion() {
        return minVersion;
    }

    public void setMinVersion(int minVersion) {
        this.minVersion = minVersion;
    }

    public int getTxnId() {
        return this.txnId;
    }

    public void setTxnId(int txnId) {
        this.txnId = txnId;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String toString() {
        return "apkPath:" + apkPath + " layoutName:" + layoutName + " packageName:" + packageName + 
                " minVersion:" + minVersion + " txnId:" + txnId;
    }
}
