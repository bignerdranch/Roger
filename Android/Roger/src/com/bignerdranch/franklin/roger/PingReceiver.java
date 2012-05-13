package com.bignerdranch.franklin.roger;

import java.io.File;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.os.Build;
import android.os.Environment;

import android.util.Log;

public class PingReceiver extends BroadcastReceiver {
    private static final String TAG = "PingReceiver";

    @Override
    public void onReceive(Context c, Intent i) {
        if (Constants.ACTION_PING.equals(i.getAction())) {
            File externalFilesDir = PingReceiver.getExternalFilesDir(c);

            if (externalFilesDir == null) {
                Log.i(TAG, "no external files dir");
            } else {
                Log.i(TAG, "external files dir ::::= " + externalFilesDir.getPath());
            }
        }
    }

    public static File getExternalFilesDir(Context c) {
        if (Build.VERSION.SDK_INT >= 8) {
            Log.i(TAG, "using Froyo method");
            return c.getExternalFilesDir(null);
        } else {
            Log.i(TAG, "using old method");
            return getExternalFilesDirPreAPIv8(c);
        }
    }

    /*
     * according to docs, this should be equivalent to getExternalFilesDir(null) in api 8+
     */
    public static File getExternalFilesDirPreAPIv8(Context c) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Log.i(TAG, "not mounted");
            return null;
        }

        File externalStorageDir = Environment.getExternalStorageDirectory();
        String ourExternalStoragePath = externalStorageDir.getPath() + 
                "/Android/data/" + c.getPackageName() + "/files";
        File ourExternalStorageDir = new File(ourExternalStoragePath);
        if (!ourExternalStorageDir.exists()) {
            if (!ourExternalStorageDir.mkdirs())
                Log.e(TAG, "Could not create external storage dir at " + ourExternalStoragePath);
            return null;
        } else {
            return ourExternalStorageDir;
        }
    }
}
