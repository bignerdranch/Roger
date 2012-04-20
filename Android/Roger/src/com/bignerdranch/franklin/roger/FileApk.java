package com.bignerdranch.franklin.roger;

import java.io.File;

import android.app.Activity;

public class FileApk extends LocalApk {
    protected String filePath;

    public FileApk(Activity a, String packageName, String filePath) {
        super(a, packageName);
        this.filePath = filePath;
    }

    @Override
    protected File getFile() {

        return new File(filePath);
    }
}
