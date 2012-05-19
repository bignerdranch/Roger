package com.bignerdranch.franklin.roger.network;

import java.io.File;

import com.bignerdranch.franklin.roger.Constants;

import android.content.Context;
import android.content.Intent;

import android.os.Environment;

import android.util.Log;

public class DownloadManager {
    private static final String TAG = "DownloadManager";

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
        getExternalFilesDirPreAPIv8();
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

    public void onDownloadComplete(Intent remoteIntent) {
        Intent localIntent = new Intent(remoteIntent);
        // replace categories with local
        for (String category : localIntent.getCategories()) {
            localIntent.removeCategory(category);
        }
        localIntent.addCategory(Constants.CATEGORY_LOCAL);
        context.sendBroadcast(localIntent);
    }
	
    public File getExternalFilesDirPreAPIv8() {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return null;
        }

        File externalStorageDir = Environment.getExternalStorageDirectory();
        String ourExternalStoragePath = externalStorageDir.getPath() + 
                "/Android/data/" + context.getPackageName() + "/files";
        File ourExternalStorageDir = new File(ourExternalStoragePath);
        if (!ourExternalStorageDir.exists()) {
            if (!ourExternalStorageDir.mkdirs())
                Log.e(TAG, "Could not create external storage dir at " + ourExternalStoragePath);
        }

        return ourExternalStorageDir;
    }
}
