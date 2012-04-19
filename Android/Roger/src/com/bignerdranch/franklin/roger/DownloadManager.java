package com.bignerdranch.franklin.roger;

import android.content.Context;

public class DownloadManager {
	private static final int APK_COUNT = 2;

	private int apkIndex;
	
	private DownloadListener listener;
	public interface DownloadListener {
		public void onApkDownloaded(final String path, final String layoutName, final String packageName);
	}
	
	private static DownloadManager manager;
	public static DownloadManager getInstance() {
		if (manager == null) {
			manager = new DownloadManager();
		}
		return manager;
	}
	
	private DownloadManager() { 
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
	
	public void setDownloadListener(DownloadListener listener) {
		this.listener = listener;
	}
	
	public void onDownloadComplete(String apkPath, String layoutName, String packageName) {
		if (listener != null) {
			listener.onApkDownloaded(apkPath, layoutName, packageName);
		}
	}
}
