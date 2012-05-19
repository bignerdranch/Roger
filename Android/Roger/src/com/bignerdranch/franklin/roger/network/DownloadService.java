package com.bignerdranch.franklin.roger.network;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.bignerdranch.franklin.roger.Constants;
import com.bignerdranch.franklin.roger.IntentUtils;
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

		try {
            synchronized (data) {
                data.desc = (ServerDescription)intent.getSerializableExtra(Constants.EXTRA_SERVER_DESCRIPTION);
            }
            Log.i(TAG, "server description for download: " + data.desc + "");
            LayoutDescription description = IntentUtils.getLayoutDescription(intent);
            connector.setDownloading(data.desc);

            if (description != null && lastAdbTxnId == null || description.getTxnId() != lastAdbTxnId) {
                // indicates that we didn't get pinged by ADB; free to download

                String remotePath = description.getApkPath();
                String localPath = downloadFile(remotePath);
                description.setApkPath(localPath);
            } else if (description != null && lastAdbTxnId != null) {
                Log.i(TAG, "aborted download for txnId: " + lastAdbTxnId + "");
            }
            connector.setFinishDownload(data.desc);

            if (description.getApkPath() != null) {
                IntentUtils.setLayoutDescription(intent, description);
                broadcastChange(intent);
            }
        } catch (ServerChangedException ex) {
            // should start up again soon
		} catch (IOException e) {
			Log.e(TAG, "Unable to download file", e);
            connector.setConnectionError(data.desc, e);
		}
	}

	private String downloadFile(String remotePath) throws IOException {
		URL remoteUrl = new URL(remotePath);
		Log.d(TAG, "Connecting to " + remotePath);

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

	private void broadcastChange(Intent intent) {
		manager.onDownloadComplete(intent);
	}

    private BroadcastReceiver incomingAdbTxnListener = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "received intent " + intent + "");
            if (intent.hasExtra(Constants.EXTRA_LAYOUT_TXN_ID)) {
                lastAdbTxnId = intent.getIntExtra(Constants.EXTRA_LAYOUT_TXN_ID, 0);
                Log.i(TAG, "just heard about an ADB transaction: " + lastAdbTxnId + "");
            }
        }
    };
}
