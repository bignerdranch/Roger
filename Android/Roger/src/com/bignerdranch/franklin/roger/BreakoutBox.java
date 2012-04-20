package com.bignerdranch.franklin.roger;

import android.util.SparseArray;
import android.view.View;

public class BreakoutBox {
    protected SparseArray<Object> tags = new SparseArray<Object>();
    protected Object tag;
    protected View view;

    public BreakoutBox(View view) {
        this.view = view;
    }

    public void setTag(Object o) {
        tag = o;
    }

    public Object getTag() {
        return tag;
    }

    public synchronized void setTag(int resId, Object o) {
        tags.put(resId, o);
    }

    public synchronized Object getTag(int resId) {
        return tags.get(resId);
    }
}

