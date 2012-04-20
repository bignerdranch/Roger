package com.bignerdranch.franklin.roger;

import android.content.Context;
import android.content.Intent;

import android.util.Log;

public class ConnectionHelper {
    public static final String TAG = "ConnectionHelper";

    protected Context context;

    private ConnectionHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    protected static ConnectionHelper instance;
    
    public static ConnectionHelper getInstance(Context context) {
        if (instance == null) {
            instance = new ConnectionHelper(context);
        }

        return instance;
    }

    public static final int STATE_CONNECTING = 0;
    public static final int STATE_CONNECTED = 1;
    public static final int STATE_FAILED = 2;
    public static final int STATE_DISCONNECTED = 3;

    ServerDescription connectedServer;
    int connectionState;
    Exception connectionError;

    public int getConnectionState() {
        return connectionState;
    }

    public ServerDescription getConnectedServer() {
        return connectedServer;
    }

    public void setConnectionSuccess(ServerDescription server) {
        if (!server.getHostAddress().equals(connectedServer.getHostAddress())) return;

        notifyConnectionStateChange(STATE_CONNECTED);
    }

    public void setConnectionError(ServerDescription server, Exception error) {
        if (!server.getHostAddress().equals(connectedServer.getHostAddress())) return;

        notifyConnectionStateChange(STATE_FAILED);
    }

    protected void notifyConnectionStateChange(int newState) {
        this.connectionState = newState;
    }

    protected void notifyDisconnect() {
        connectedServer = null;

        notifyConnectionStateChange(STATE_DISCONNECTED);
    }

    protected void notifyStartConnect(ServerDescription server) {
        connectedServer = server;

        notifyConnectionStateChange(STATE_CONNECTING);
    }

    protected void serviceDisconnect() {
        Intent i = new Intent(context, DownloadService.class);
        i.setAction(DownloadService.ACTION_DISCONNECT);
        context.startService(i);
        notifyDisconnect();
    }

    protected void connect(ServerDescription server) {
        connectedServer = server;
        Intent i = new Intent(context, DownloadService.class);
        i.setAction(DownloadService.ACTION_CONNECT);
        i.putExtra(DownloadService.EXTRA_SERVER_DESCRIPTION, server);
        context.startService(i);
        notifyStartConnect(server);
    }

    public void connectToServer(ServerDescription server) {
        if (server == null) {
            serviceDisconnect();
        } else if (connectedServer != null && server.getHostAddress().equals(connectedServer.getHostAddress())) {
            Log.i(TAG, "connecting to " + connectedServer.getHostAddress() + ", but we're already connected to it");
            // do nothing
        } else {
            connect(server);
        }
    }
}
