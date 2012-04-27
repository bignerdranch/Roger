package com.bignerdranch.franklin.roger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.util.Log;

public class LayoutReceiver extends BroadcastReceiver {
    private static final String TAG = "LayoutReceiver";

    public void onReceive(Context c, Intent i) {
        Log.i(TAG, "received intent: " + i + "; repackaging");
        Log.i(TAG, "keySet:");
        for (String s : i.getExtras().keySet()) {
            Log.i(TAG, "    s: " + s + " v: " + i.getExtras().get(s) + "");
        }

        LayoutDescription desc = new LayoutDescription();

        String apkPath = i.getStringExtra(Constants.EXTRA_LAYOUT_APK_PATH);
        String layoutName = i.getStringExtra(Constants.EXTRA_LAYOUT_LAYOUT_NAME);
        String packageName = i.getStringExtra(Constants.EXTRA_LAYOUT_PACKAGE_NAME);
        int minimumVersion = i.getIntExtra(Constants.EXTRA_LAYOUT_MIN_VERSION, 1);
        int txnId = i.getIntExtra(Constants.EXTRA_LAYOUT_TXN_ID, 1);

        desc.setApkPath(apkPath);
        desc.setLayoutName(layoutName);
        desc.setPackageName(packageName);
        desc.setMinVersion(minimumVersion);
        desc.setTxnId(txnId);

        Intent redirect = new Intent(Constants.ACTION_NEW_LAYOUT);
        redirect.putExtra(Constants.EXTRA_LAYOUT_DESCRIPTION, desc);
        Log.i(TAG, "about to redirect using " + redirect + "...");
        c.sendBroadcast(redirect);
        Log.i(TAG, "done.");
    }
}
