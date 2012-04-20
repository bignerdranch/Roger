package com.bignerdranch.franklin.roger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import android.util.Pair;

public class DownloadService extends IntentService {
	private static final String TAG = "DownloadService";

    //private static final String PACKAGE = "com.bignerdranch.franklin.roger.dummypackage";
	//private static final String PACKAGE = "com.bignerdranch.franklin.roger";
//	private static final String PACKAGE = "com.att.labs.uversetv.android.tablet";
	//private static final String PACKAGE = "com.bignerdranch.Franklin.RogerTest";

	private static final char INFO_PREFIX = '-';

    private static final int CHUNK_SIZE = 32768;
    private static final int BUFFER_SIZE = CHUNK_SIZE;

    public static final String ACTION_DISCONNECT = 
        DownloadService.class.getPackage() + ".ACTION_CONNECT";
    public static final String ACTION_CONNECT = 
        DownloadService.class.getPackage() + ".ACTION_CONNECT";
    public static final String EXTRA_SERVER_DESCRIPTION = 
        DownloadService.class.getPackage() + ".EXTRA_SERVER_DESCRIPTION";

    private static final String HOSTNAME = "http://10.1.10.108";
	
	private static final String SERVER_ADDRESS = HOSTNAME + ":8082/";
	private static final String SERVER_APK_ADDRESS = HOSTNAME + ":8081/get?hash=%1$s";
	private DownloadManager manager;
    private HttpURLConnection connection;
	
	public DownloadService() {
		super("DownloadService");
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_CONNECT.equals(intent.getAction())) {
            synchronized (this) {
                if (connection != null) {
                    connection.disconnect();
                    connection = null;
                }
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

	@Override
	protected void onHandleIntent(Intent intent) {
		manager = DownloadManager.getInstance();
		
		try {
			FileDescriptor descriptor = getDescriptor();
			String filePath = getApk(descriptor.identifier);
			broadcastChange(filePath, descriptor.layout, descriptor.pack);
		} catch (IOException e) {
			Log.e(TAG, "Unable to download file", e);
		}
		
		startService(new Intent(this, DownloadService.class));
	}

	private FileDescriptor getDescriptor() throws IOException {

		URL remoteUrl = new URL(SERVER_ADDRESS);
		Log.d(TAG, "Connecting to " + SERVER_ADDRESS);

        InputStream input;
        synchronized (this) {
            connection = (HttpURLConnection) remoteUrl.openConnection();
            connection.connect();

            input = connection.getInputStream();
        }

		byte[] buffer = new byte[BUFFER_SIZE];
		int bytesRead = 0;
		String layoutFile = "main";
		String identifier = "";
		String pack = "";
		
		while ((bytesRead = input.read(buffer)) > 0) {
			String data = new String(buffer);
			
			String response = data.substring(0, data.indexOf("--"));
			Log.d(TAG, "Got response: " + response);
			
			String[] values = response.split("\n");
			layoutFile = values[0];
            layoutFile = layoutFile.split("\\.")[0];
			identifier = values[1];
			pack = values[2];
			
			Log.d(TAG, "Layout file: " + layoutFile + " identifier " + identifier + " package: " + pack);
		}
		
		return new FileDescriptor(identifier, pack, layoutFile);
	}
	
	private String getApk(String identifier) throws IOException {
		String address = String.format(SERVER_APK_ADDRESS, identifier);
		URL remoteUrl = new URL(address);
		Log.d(TAG, "Connecting to " + address);

        long startTime = System.currentTimeMillis();

		String filePath = getPath();
		FileOutputStream output = getOutputStream(filePath);
        InputStream input;
        synchronized (this) {
            connection = (HttpURLConnection) remoteUrl.openConnection();
            connection.setChunkedStreamingMode(CHUNK_SIZE);
            connection.connect();
            input = connection.getInputStream();
            Log.d(TAG, "Content length " + connection.getContentLength());
            input = connection.getInputStream();
        }

		byte[] buffer = new byte[BUFFER_SIZE];
		int bytesRead = 0;
		int bytesWritten = 0;
		
		while ((bytesRead = input.read(buffer)) > 0) {
			output.write(buffer, 0, bytesRead);
			bytesWritten += bytesRead;
            Log.i(TAG, "read " + bytesRead + " bytes " + bytesWritten + " total");
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

	private void broadcastChange(String apkPath, String layoutName, String pack) {
		manager.onDownloadComplete(apkPath, layoutName, pack);
	}
	
	private class FileDescriptor {
		public String identifier;
		public String pack;
		public String layout;
		
		public FileDescriptor(String identifier, String pack, String layout) {
			this.identifier = identifier;
			this.pack = pack;
			this.layout = layout;
		}
	}
}
