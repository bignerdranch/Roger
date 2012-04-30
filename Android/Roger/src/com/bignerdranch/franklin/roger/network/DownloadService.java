package com.bignerdranch.franklin.roger.network;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.bignerdranch.franklin.roger.Constants;
import com.bignerdranch.franklin.roger.LayoutDescription;

import com.bignerdranch.franklin.roger.pair.ConnectionHelper;
import com.bignerdranch.franklin.roger.pair.ServerDescription;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class DownloadService extends IntentService {
	private static final String TAG = "DownloadService";

    private static final int CHUNK_SIZE = 64 * 1024;
    private static final int BUFFER_SIZE = CHUNK_SIZE;

	private DownloadManager manager;

    private static class ConnectionData {
        ServerDescription desc;
        HttpURLConnection conn;
    }

    protected ConnectionData data = new ConnectionData(); 
    private Integer lastAdbTxnId = null;

	public DownloadService() {
		super("DownloadService");
	}

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter filter = new IntentFilter(Constants.ACTION_INCOMING_ADB_TXN);
        registerReceiver(incomingAdbTxnListener, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(incomingAdbTxnListener);
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
		manager = DownloadManager.getInstance(this);
        ConnectionHelper connector = ConnectionHelper.getInstance(this);

        if (Constants.ACTION_DISCONNECT.equals(intent.getAction())) {
            // do nothing
            return;
        }

		try {
            synchronized (data) {
                data.desc = (ServerDescription)intent.getSerializableExtra(Constants.EXTRA_SERVER_DESCRIPTION);
            }
			LayoutDescription description = getLayoutDescription();
            connector.setDownloading(data.desc);

            if (description.getTxnId() != lastAdbTxnId) {
                // indicates that we didn't get pinged by ADB
                getApk(description.getIdentifier());
            }
            connector.setFinishDownload(data.desc);
			broadcastChange(description);

            // still connected, presumably
            Intent i = new Intent(this, this.getClass());
            i.setAction(Constants.ACTION_CONNECT);
            i.putExtra(Constants.EXTRA_SERVER_DESCRIPTION, data.desc);
            startService(i);
        } catch (ServerChangedException ex) {
            // should start up again soon
		} catch (IOException e) {
			Log.e(TAG, "Unable to download file", e);
            connector.setConnectionError(data.desc, e);
		}

	}

	private LayoutDescription getLayoutDescription() throws IOException {
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

		byte[] buffer = new byte[BUFFER_SIZE];
		String layoutName = "";
		String layoutType = "";
		String identifier = "";
		String pack = "";
		String minVersionText = "";
		String txnIdText = "";
		
		while ((input.read(buffer)) > 0) {
			String data = new String(buffer);

			String response = data.substring(0, data.indexOf("--"));
			Log.d(TAG, "Got response: " + response);

            int i = 0;
			String[] values = response.split("\n");
			layoutName = values[i++];
			layoutName = layoutName.split("\\.")[0];
			layoutType = values[i++];
			identifier = values[i++];
			pack = values[i++];
			minVersionText = values[i++];
			txnIdText = values[i++];

			Log.d(TAG, "Layout file: " + layoutName + " identifier " + identifier + " package: " + pack);
		}

        synchronized (data) {
            data.conn.disconnect();
            data.conn = null;
        }
        
        int minVersion = 0;
        try {
        	minVersion = Integer.parseInt(minVersionText);
        } catch (NumberFormatException e) {
        	Log.e(TAG, "Unable to parse min version from " + minVersionText, e);
        }
        

        int txnId = 0;
        try {
        	txnId = Integer.parseInt(txnIdText);
        } catch (NumberFormatException e) {
        	Log.e(TAG, "Unable to parse min version from " + minVersionText, e);
        }
        

		return new LayoutDescription(identifier, null, pack, layoutName, layoutType, minVersion, txnId);
	}

	private String getApk(String identifier) throws IOException {
		String address = String.format(data.desc.getApkAddress(), identifier);
		URL remoteUrl = new URL(address);
		Log.d(TAG, "Connecting to " + address);

        long startTime = System.currentTimeMillis();

		String filePath = getPath();

        new File(filePath).delete();
		FileOutputStream output = getOutputStream(filePath);
        InputStream input;
        synchronized (data) {
            validateData();

            data.conn = (HttpURLConnection) remoteUrl.openConnection();
            data.conn.setChunkedStreamingMode(CHUNK_SIZE);
            data.conn.connect();
            input = data.conn.getInputStream();
            Log.d(TAG, "Content length " + data.conn.getContentLength());
            input = data.conn.getInputStream();
        }

		byte[] buffer = new byte[BUFFER_SIZE];
		int bytesRead = 0;
		int bytesWritten = 0;

		while ((bytesRead = input.read(buffer)) > 0) {
			output.write(buffer, 0, bytesRead);
			bytesWritten += bytesRead;
            Log.i(TAG, "read " + bytesRead + " " + bytesWritten + " total");
		}

        synchronized (data) {
            data.conn.disconnect();
            data.conn = null;
        }
		
		Log.d(TAG, "Wrote " + bytesWritten + " bytes in " + (System.currentTimeMillis() - startTime) + " ms");
		return filePath;
	}

	private String getPath() {
		String path = manager.getNextPath(this);
		Log.d(TAG, "Downloading file with path: " + path);
		return path;
	}

	private FileOutputStream getOutputStream(String path) throws IOException {
		File filePath = new File(path);
		filePath.createNewFile();
		return new FileOutputStream(filePath);
	}

	private void broadcastChange(LayoutDescription description) {
		manager.onDownloadComplete(description);
	}

    private BroadcastReceiver incomingAdbTxnListener = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(Constants.EXTRA_LAYOUT_TXN_ID)) {
                lastAdbTxnId = intent.getIntExtra(Constants.EXTRA_LAYOUT_TXN_ID, 0);
                Log.i(TAG, "just heard about an ADB transaction: " + lastAdbTxnId + "");
            }
        }
    };
}
