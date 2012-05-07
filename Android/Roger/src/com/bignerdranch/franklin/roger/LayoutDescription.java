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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((apkPath == null) ? 0 : apkPath.hashCode());
		result = prime * result
				+ ((identifier == null) ? 0 : identifier.hashCode());
		result = prime * result
				+ ((layoutName == null) ? 0 : layoutName.hashCode());
		result = prime * result
				+ ((layoutType == null) ? 0 : layoutType.hashCode());
		result = prime * result + minVersion;
		result = prime * result
				+ ((packageName == null) ? 0 : packageName.hashCode());
		result = prime * result + txnId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LayoutDescription other = (LayoutDescription) obj;
		if (apkPath == null) {
			if (other.apkPath != null)
				return false;
		} else if (!apkPath.equals(other.apkPath))
			return false;
		if (identifier == null) {
			if (other.identifier != null)
				return false;
		} else if (!identifier.equals(other.identifier))
			return false;
		if (layoutName == null) {
			if (other.layoutName != null)
				return false;
		} else if (!layoutName.equals(other.layoutName))
			return false;
		if (layoutType == null) {
			if (other.layoutType != null)
				return false;
		} else if (!layoutType.equals(other.layoutType))
			return false;
		if (minVersion != other.minVersion)
			return false;
		if (packageName == null) {
			if (other.packageName != null)
				return false;
		} else if (!packageName.equals(other.packageName))
			return false;
		if (txnId != other.txnId)
			return false;
		return true;
	}
}
