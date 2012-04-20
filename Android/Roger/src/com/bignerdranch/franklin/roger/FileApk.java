package com.bignerdranch.franklin.roger;

import java.io.File;

import android.content.Context;

public class FileApk extends LocalApk {
    protected String filePath;

    public FileApk(Context c, String packageName, String filePath) {
        super(c, packageName);
        this.filePath = filePath;
    }

    @Override
    protected File getFile() {

        return new File(filePath);
    }
}
