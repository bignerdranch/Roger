package com.bignerdranch.franklin.roger;

import java.util.Iterator;

import android.content.Intent;
import android.content.IntentFilter;

public class IntentUtils {
    public static LayoutDescription getLayoutDescription(Intent i) {
        LayoutDescription desc = new LayoutDescription();

        String apkPath = i.getStringExtra(Constants.EXTRA_LAYOUT_APK_PATH);
        String layoutName = i.getStringExtra(Constants.EXTRA_LAYOUT_LAYOUT_NAME).split("\\.")[0];
        String layoutType = i.getStringExtra(Constants.EXTRA_LAYOUT_LAYOUT_TYPE).split("\\.")[0];
        String packageName = i.getStringExtra(Constants.EXTRA_LAYOUT_PACKAGE_NAME);
        int minimumVersion = i.getIntExtra(Constants.EXTRA_LAYOUT_MIN_VERSION, 1);
        int txnId = i.getIntExtra(Constants.EXTRA_LAYOUT_TXN_ID, 1);

        desc.setApkPath(apkPath);
        desc.setLayoutName(layoutName);
        desc.setLayoutType(layoutType);
        desc.setPackageName(packageName);
        desc.setMinVersion(minimumVersion);
        desc.setTxnId(txnId);

        return desc;
    }

    public static void setLayoutDescription(Intent i, LayoutDescription desc) {
        String apkPath = desc.getApkPath();
        String layoutName = desc.getLayoutName();
        String layoutType = desc.getLayoutType();
        String packageName = desc.getPackageName();
        int minimumVersion = desc.getMinVersion();
        int txnId = desc.getTxnId();

        desc.setApkPath(apkPath);
        desc.setLayoutName(layoutName);
        desc.setLayoutType(layoutType);
        desc.setPackageName(packageName);
        desc.setMinVersion(minimumVersion);
        desc.setTxnId(txnId);

        i.putExtra(Constants.EXTRA_LAYOUT_APK_PATH, apkPath);
        i.putExtra(Constants.EXTRA_LAYOUT_LAYOUT_NAME, layoutName);
        i.putExtra(Constants.EXTRA_LAYOUT_LAYOUT_TYPE, layoutType);
        i.putExtra(Constants.EXTRA_LAYOUT_PACKAGE_NAME, packageName);
        i.putExtra(Constants.EXTRA_LAYOUT_MIN_VERSION, minimumVersion);
        i.putExtra(Constants.EXTRA_LAYOUT_TXN_ID, txnId);
    }

    private static void iterateStrings(StringBuilder s, Iterator<String> iter) {
        int i = 0;
        while (iter.hasNext()) {
            String next = iter.next();
            if (i++ != 0) {
                s.append(", ");
            }
            s.append(next);
        }
    }

    public static String getDescription(IntentFilter f) {
        StringBuilder s = new StringBuilder("{");

        s.append("ACTION: ");
        iterateStrings(s, f.actionsIterator());
        s.append("; ");
        s.append("CATEGORIES: ");
        iterateStrings(s, f.categoriesIterator());
        s.append("}");
        return s.toString();
    }

    static String abbv(String s) {
        if (s == null) {
            return null;
        }

        String[] parts = s.split("\\.");
        return parts[parts.length - 1];
    }

    public static String getDescription(Intent intent) {
        StringBuilder s = new StringBuilder("{");

        s.append("ACTION: " + abbv(intent.getAction()) + "; ");
        s.append("CATEGORIES: ");
        int i = 0;
        if (intent.getCategories() != null) {
            for (String c : intent.getCategories()) {
                if (i++ > 0) s.append(", ");
                s.append(abbv(c));
            }
        }
        s.append("; ");
        s.append("EXTRAS: ");
        i = 0;
        for (String k : intent.getExtras().keySet()) {
            if (intent.getExtras().get(k) != null) {
                if (i++ > 0) s.append(", ");
                s.append(abbv(k));
            }
        }
        s.append("; ");

        s.append("DATA: " + abbv(intent.getDataString()) + "; ");

        s.append("}");
        return s.toString();
    }
}
