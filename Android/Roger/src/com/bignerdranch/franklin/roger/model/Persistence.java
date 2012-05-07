package com.bignerdranch.franklin.roger.model;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import android.content.Context;
import android.util.Log;

import com.bignerdranch.franklin.roger.LayoutDescription;

public class Persistence implements Serializable {
    public static final long serialVersionUID = 0l;

    private static final String TAG = "Persistence";

    private static Persistence instance;
    private transient Context context;

    private RogerParams params;
    private LayoutDescription layoutDescription;

    private static final String EVERYTHING = "everything.thefile";

    private Persistence(Context c) {
        context = c.getApplicationContext();
    }

    private void save() {
        try {
            FileOutputStream out = context.openFileOutput(EVERYTHING, Context.MODE_PRIVATE);
            ObjectOutputStream oOut = new ObjectOutputStream(out);
            oOut.writeObject(this);
        } catch (Exception ex) {
            Log.i(TAG, "unable to save persistent stuff to file", ex);
        }
    }

    private static boolean load(Context context) {
        try {
            FileInputStream in = context.openFileInput(EVERYTHING);
            ObjectInputStream oIn = new ObjectInputStream(in);
            instance = (Persistence)oIn.readObject();
            return true;
        } catch (Exception ex) {
            Log.i(TAG, "unable to read persistent stuff from file", ex);
            return false;
        }
    }

    public static Persistence getInstance(Context c) {
        if (instance == null && !load(c)) {
            instance = new Persistence(c);
        } 

        return instance;
    }

    public void setRogerParams(RogerParams params) {
        this.params = params;
        this.save();
    }

    public RogerParams getRogerParams() {
        return params;
    }

    public void setLayoutDescription(LayoutDescription desc) {
        this.layoutDescription = desc;
        this.save();
    }

    public LayoutDescription getLayoutDescription() {
        return layoutDescription;
    }
}
