package com.bignerdranch.franklin.roger;

import java.io.File;

import android.content.Context;

public class AssetsApk extends LocalApk {
    public static final String TAG = "AssetsApk";

    private String assetPath;

    @Override
    protected File getFile() {

        return new File(assetPath);
    }

    public AssetsApk(Context c, String packageName, String assetPath) {
        super(c, packageName);
        this.assetPath = assetPath;
    }
}
