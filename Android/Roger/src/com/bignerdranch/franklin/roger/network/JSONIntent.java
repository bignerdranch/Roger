package com.bignerdranch.franklin.roger.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bignerdranch.franklin.roger.IntentUtils;

import android.content.Context;
import android.content.Intent;

import android.net.Uri;

import android.util.Log;

public class JSONIntent {
    private static final String TAG = "JSONIntent";
    JSONObject source;
    public JSONIntent(JSONObject source) {
        this.source = source;
    }

    static String getString(JSONObject object, String key)  throws JSONException {
        if (object.isNull(key)) {
            return null;
        } else {
            return object.getString(key);
        }
    }

    String getAction() throws JSONException {
        return getString(source, "action");
    }

    Uri getData() throws JSONException {
        String data = getString(source, "data");
        if (data != null) {
            return Uri.parse(data);
        } else {
            return null;
        }
    }

    ArrayList<String> getCategories() throws JSONException {
        JSONArray cats = source.getJSONArray("categories");
        ArrayList<String> results = new ArrayList<String>();

        for (int i = 0; i < cats.length(); i++) {
            results.add(cats.getString(i));
        }

        return results;
    }

    String getType() throws JSONException {
        return getString(source, "type");
    }

    HashMap<String,Object> getExtras() throws JSONException {
        HashMap<String,Object> results = new HashMap<String,Object>();

        JSONObject extras = source.getJSONObject("extras");

        Iterator<?> iter = extras.keys();
        while (iter.hasNext()) {
            String key = (String)iter.next();
            results.put(key, extras.get(key));
        }

        return results;
    }

    public Intent getIntent() throws JSONException {
        Intent i = new Intent(getAction());
        i.setData(getData());

        for (String category : getCategories()) {
            i.addCategory(category);
        }

        HashMap<String,Object> extras = getExtras();
        for (String key : extras.keySet()) {
            Object value = extras.get(key);
            if (value instanceof Integer) {
                i.putExtra(key, (int)(Integer)value);
            } else if (value instanceof Double) {
                i.putExtra(key, (double)(Double)value);
            } else if (value instanceof Float) {
                i.putExtra(key, (float)(Float)value);
            } else if (value instanceof String) {
                i.putExtra(key, (String)value);
            } else {
                Log.i(TAG, "I have no idea what this is. key: " + key + " value: " + value + "");
            }
        }

        return i;
    }

    public void fire(Context context, Intent i) {
        try {
            String type = getType();
            if (type.equals("broadcast")) {
                Log.i(TAG, "firing " + type + " intent: " + IntentUtils.getDescription(i) + "");
                context.sendBroadcast(i);
            } else if (type.equals("service")) {
                Log.i(TAG, "firing " + type + " intent: " + IntentUtils.getDescription(i) + "");
                context.startService(i);
            } else if (type.equals("activity")) {
                Log.i(TAG, "firing " + type + " intent: " + IntentUtils.getDescription(i) + "");
                context.startActivity(i);
            } else {
                Log.i(TAG, "not firing " + type + " intent b/c i don't know what it is");
            }
        } catch (JSONException e) {
            Log.e(TAG, "failed to fire intent", e);
        }
    }

    public void fire(Context context) {
        try {
            Intent i = getIntent();
            fire(context, i);
        } catch (JSONException e) {
            Log.e(TAG, "failed to fire intent", e);
        }
    }
}
