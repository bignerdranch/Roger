package com.bignerdranch.franklin.roger.pair;

import java.util.ArrayList;


import android.content.Context;
import android.content.Intent;

public class DiscoveryHelper {
    private Context context;

    private DiscoveryHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    private static DiscoveryHelper instance;

    public static DiscoveryHelper getInstance(Context c) {
        if (instance == null) {
            instance = new DiscoveryHelper(c);
        }

        return instance;
    }

    public interface Listener {
        public void onStateChanged(DiscoveryHelper helper);
    }

    private ArrayList<Listener> listeners = new ArrayList<Listener>();

    public void addListener(Listener listener) {
        if (!listeners.contains(listener)) 
            listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public static final int STATE_IDLE = 0;
    public static final int STATE_DISCOVERING = 1;

    private int state = STATE_IDLE;

    private ArrayList<ServerDescription> servers = new ArrayList<ServerDescription>(); 

    protected void notifyStateChanged(int newState) {
        this.state = newState;

        for (Listener listener : listeners) {
            listener.onStateChanged(this);
        }
    }

    public int getState() {
        return state;
    }

    public ArrayList<ServerDescription> getDiscoveredServers() {
        return new ArrayList<ServerDescription>(servers);
    }

    protected void startService() {
        Intent i = new Intent(context, FindServerService.class);
        context.startService(i);
        notifyStateChanged(STATE_DISCOVERING);
    }

    public void startDiscovery() {
        if (state == STATE_IDLE) {
            startService();
        }
    }

    public void finishDiscovery() {
        notifyStateChanged(STATE_IDLE);
    }
}
