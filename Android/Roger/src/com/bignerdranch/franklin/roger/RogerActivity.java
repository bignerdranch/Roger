package com.bignerdranch.franklin.roger;

import android.app.Activity;

import android.content.res.Resources;
import android.os.Bundle;

import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.FrameLayout;

public class RogerActivity extends Activity {
    public static final String TAG = "RogerActivity";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        FrameLayout container = (FrameLayout)findViewById(R.id.container);

        String apkName = "YellowBook.apk";
        String packageName = "com.serco.yellowbook";
        AssetsApk apk = new AssetsApk(this, packageName, apkName);
        Log.i(TAG, "Did it work? " + apk.getFile().exists() + "");
        Resources r = apk.getResources();
        Log.i(TAG, "hmm, still alive");

        Log.i(TAG, "let's try to get a resource id");
        int id = r.getIdentifier("detail_layout", "layout", packageName);
        Log.i(TAG, "here's what we got: " + id + "");

        Log.i(TAG, "getting a layout inflater...");
        LayoutInflater inflater = apk.getLayoutInflater(getLayoutInflater());
        Log.i(TAG, "inflating???");
        View v = inflater.inflate(id, container, false);

        container.addView(v);
    }
}
