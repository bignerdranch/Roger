package com.bignerdranch.franklin.roger.network;

import com.bignerdranch.franklin.roger.Constants;
import com.bignerdranch.franklin.roger.LayoutDescription;

import android.content.Context;
import android.content.Intent;

public class DownloadManager {
	private static final int APK_COUNT = 2;

	private int apkIndex;
    private Context context;
	
	private static DownloadManager manager;
	public static DownloadManager getInstance(Context c) {
		if (manager == null) {
			manager = new DownloadManager(c);
		}
		return manager;
	}
	
	private DownloadManager(Context context) { 
        this.context = context.getApplicationContext();
		apkIndex = 0;
	}

	public String getNextPath(Context context) {
		String path = context.getFilesDir() + "/roger" + apkIndex + ".apk";
		updateIndex();
		return path;
	}

	private void updateIndex() {
		apkIndex++;
		
		if (apkIndex >= APK_COUNT) {
			apkIndex = 0;
		}
	}
	
	public void onDownloadComplete(String apkPath, String layoutName, String packageName, int minimumVersion) {
        LayoutDescription desc = new LayoutDescription();

        desc.setApkPath(apkPath);
        desc.setLayoutName(layoutName);
        desc.setPackageName(packageName);
        desc.setMinVersion(minimumVersion);

        Intent i = new Intent(Constants.ACTION_NEW_LAYOUT);
        i.putExtra(Constants.EXTRA_LAYOUT_DESCRIPTION, desc);
        context.sendBroadcast(i);
	}
}
