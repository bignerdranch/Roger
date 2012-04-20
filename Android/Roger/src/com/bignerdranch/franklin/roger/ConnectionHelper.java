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
    
    public ConnectionHelper getInstance(Context context) {
        if (instance == null) {
            instance = new ConnectionHelper(context);
        }

        return instance;
    }


    ServerDescription connectedServer;

    protected void disconnect() {
        Intent i = new Intent(DownloadService.ACTION_DISCONNECT);
        context.startService(i);
    }

    protected void connect(ServerDescription server) {
        connectedServer = server;
        Intent i = new Intent(DownloadService.ACTION_CONNECT);
        context.startService(i);
    }

    public void connectToServer(ServerDescription server) {
        if (server == null) {
            disconnect();
        } else if (connectedServer != null && server.getHostAddress().equals(connectedServer.getHostAddress())) {
            Log.i(TAG, "connecting to " + connectedServer.getHostAddress() + ", but we're already connected to it");
            // do nothing
        } else {
            connect(server);
        }
    }
}
