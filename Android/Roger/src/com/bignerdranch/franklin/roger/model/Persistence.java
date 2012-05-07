package com.bignerdranch.franklin.roger.model;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

import com.bignerdranch.franklin.roger.LayoutDescription;

public class Persistence implements Serializable {
    public static final long serialVersionUID = 0l;

    private static final String TAG = "Persistence";

    private static Persistence instance;

    protected transient Context context;
    protected transient ArrayList<Listener> listeners = new ArrayList<Listener>();


    private RogerParams params;
    private LayoutDescription layoutDescription;
    private boolean isListView = false;
    private Boolean isTextFillEnabled = null;

    public interface Listener {
        public void update();
    }

    public Boolean getIsTextFillEnabled() {
		return isTextFillEnabled;
	}

	public void setIsTextFillEnabled(Boolean isTextFillEnabled) {
		this.isTextFillEnabled = isTextFillEnabled;
	}

	private static final String EVERYTHING = "everything.thefile";

    private Persistence(Context c) {
        context = c.getApplicationContext();
    }

    public void addListener(Listener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void updateListeners() {
        for (Listener listener: listeners) {
            listener.update();
        }
    }

    private void save() {
        try {
            Log.i(TAG, "context? " + context + "");
            FileOutputStream out = context.openFileOutput(EVERYTHING, Context.MODE_PRIVATE);
            ObjectOutputStream oOut = new ObjectOutputStream(out);
            oOut.writeObject(this);
            updateListeners();
            oOut.close();
        } catch (Exception ex) {
            Log.i(TAG, "unable to save persistent stuff to file", ex);
        }
    }

    private static boolean load(Context context) {
        try {
            FileInputStream in = context.openFileInput(EVERYTHING);
            ObjectInputStream oIn = new ObjectInputStream(in);
            Persistence old = instance;
            instance = (Persistence)oIn.readObject();
            if (old != null) {
                instance.listeners = old.listeners;
            }
            if (instance.listeners == null) {
                instance.listeners = new ArrayList<Listener>();
                instance.context = context.getApplicationContext();
            }
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
        if (params == this.params || params != null && params.equals(this.params)) return;
        this.params = params;
        this.save();
    }

    public RogerParams getRogerParams() {
        return params;
    }

    public void setLayoutDescription(LayoutDescription desc) {
        if (desc == this.layoutDescription || desc != null && desc.equals(this.layoutDescription)) return;
        this.layoutDescription = desc;
        this.save();
    }

    public LayoutDescription getLayoutDescription() {
        return layoutDescription;
    }

    public boolean isListView() {
        return isListView;
    }

    public void setListView(boolean isListView) {
        boolean changed = isListView != this.isListView;
        this.isListView = isListView;
        if (changed) { save(); }
    }

    public void forceUpdate() {
        save();
    }
}
