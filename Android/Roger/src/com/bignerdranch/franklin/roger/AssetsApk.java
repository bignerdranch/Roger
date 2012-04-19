package com.bignerdranch.franklin.roger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.content.Context;

import android.util.Log;

public class AssetsApk extends LocalApk {
    public static final String TAG = "AssetsApk";

    private String assetName;

    @Override
    protected File getFile() {
        File filePath = context.getFilesDir();
        String assetPath = filePath.getPath() + "/" + assetName;

        return new File(assetPath);
    }

    public AssetsApk(Context c, String packageName, String assetName) {
        super(c, packageName);
        this.assetName = assetName;

        File assetFile = getFile();
        if (!assetFile.exists()) {
            try {
                copyAsset(assetFile, assetName);
            } catch (Exception ex) {
                Log.i(TAG, "failed to copy over asset", ex);
            }
        }
    }

    private void copyAsset(File assetFile, String assetName) throws IOException {
        InputStream assetInputStream = context.getAssets().open(assetName);
        OutputStream assetOutputStream = new FileOutputStream(assetFile);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = assetInputStream.read(buffer)) > 0) {
            assetOutputStream.write(buffer, 0, length);
        }
        assetOutputStream.flush();
        assetOutputStream.close();
        assetInputStream.close();
    }
}
