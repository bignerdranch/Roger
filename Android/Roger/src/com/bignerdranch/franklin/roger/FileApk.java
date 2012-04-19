package com.bignerdranch.franklin.roger;

import java.io.File;

import android.content.Context;

public class FileApk extends LocalApk {
    protected File file;

    public FileApk(Context c, String packageName, File file) {
        super(c, packageName);
        this.file = file;
    }

    @Override
    public File getFile() {
        return file;
    }
}
