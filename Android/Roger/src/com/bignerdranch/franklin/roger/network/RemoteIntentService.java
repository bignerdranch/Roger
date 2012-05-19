package com.bignerdranch.franklin.roger.network;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.bignerdranch.franklin.roger.Constants;
import com.bignerdranch.franklin.roger.IntentUtils;

import com.bignerdranch.franklin.roger.pair.ConnectionHelper;
import com.bignerdranch.franklin.roger.pair.ServerDescription;

import android.app.IntentService;

import android.content.Intent;

import android.util.Log;

public class RemoteIntentService extends IntentService {
    private static final String TAG = "RemoteIntentService";

    private static class ConnectionData {
        ServerDescription desc;
        HttpURLConnection conn;
    }

    protected ConnectionData data = new ConnectionData(); 

	public RemoteIntentService() {
		super("RemoteIntentService");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (Constants.ACTION_CONNECT.equals(intent.getAction()) || Constants.ACTION_DISCONNECT.equals(intent.getAction())) {
			synchronized (data) {
				if (data.conn != null) {
					data.conn.disconnect();
					data.conn = null;
				}
			}
		}

		return super.onStartCommand(intent, flags, startId);
	}

    private static class ServerChangedException extends RuntimeException {
        public static final long serialVersionUID = 0l;
        public ServerChangedException() {
            super();
        }
    }

    private void validateData() {
        synchronized (data) {
            ConnectionHelper helper = ConnectionHelper.getInstance(this);
            if (!data.desc.equals(helper.getConnectedServer())) {
                throw new ServerChangedException();
            }
        }
    }

	@Override
	protected void onHandleIntent(Intent intent) {
        ConnectionHelper connector = ConnectionHelper.getInstance(this);
        Log.i(TAG, "started up with intent: " + intent + "");

        if (Constants.ACTION_DISCONNECT.equals(intent.getAction())) {
            Log.i(TAG, "disconnecting.");
            // do nothing
            return;
        }

		try {
            synchronized (data) {
                data.desc = (ServerDescription)intent.getSerializableExtra(Constants.EXTRA_SERVER_DESCRIPTION);
            }
            JSONIntent remoteIntent = getIntent();
            if (remoteIntent != null) {
                Log.i(TAG, "got a remote intent. firing it.");
                try {
                    Intent i = remoteIntent.getIntent();
                    i.putExtra(Constants.EXTRA_SERVER_DESCRIPTION, data.desc);
                    Log.i(TAG, "   intent: " + IntentUtils.getDescription(i) + "");

                    remoteIntent.fire(this, i);
                } catch (JSONException je) {
                    Log.e(TAG, "failed to fire remote intent", je);
                }

            } else {
                Log.e(TAG, "got no remote intent");
            }

            // still connected, presumably
            Intent i = new Intent(this, this.getClass());
            i.setAction(Constants.ACTION_CONNECT);
            i.putExtra(Constants.EXTRA_SERVER_DESCRIPTION, data.desc);
            startService(i);
        } catch (ServerChangedException ex) {
            Log.i(TAG, "server changed");
            // should start up again soon
		} catch (IOException e) {
			Log.e(TAG, "Unable to download remote intent", e);
            connector.setConnectionError(data.desc, e);
		}
	}

    private JSONIntent getIntent() throws IOException {
        String serverAddress = data.desc.getServerAddress();
		URL remoteUrl = new URL(serverAddress);
		Log.d(TAG, "Connecting to " + serverAddress);

		InputStream input;
		synchronized (data) {
            validateData();
			data.conn = (HttpURLConnection) remoteUrl.openConnection();
			data.conn.connect();
            ConnectionHelper.getInstance(this)
                .setConnectionSuccess(data.desc);

			input = data.conn.getInputStream();
		}

        StringBuilder data = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        String line;
        while ((line = reader.readLine()) != null) {
            data.append(line);
        }

        try {
            JSONObject object = (JSONObject)new JSONTokener(data.toString()).nextValue();

            return new JSONIntent(object);
        } catch (JSONException e) {
            Log.i(TAG, "failed to parse incoming data. data: " + data + "", e);
            return null;
        }
    }
}
