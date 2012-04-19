package com.bignerdranch.franklin.roger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class DownloadService extends IntentService {
	private static final String TAG = "DownloadService";
	
	private static final String SERVER_ADDRESS = "http://10.1.10.108:8082/";
	private DownloadManager manager;
	
	public DownloadService() {
		super("DownloadService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		manager = DownloadManager.getInstance();
		
		try {
			downloadApk();
		} catch (IOException e) {
			Log.e(TAG, "Unable to download file", e);
		}
	}

	private void downloadApk() throws IOException {

		URL remoteUrl = new URL(SERVER_ADDRESS);
		Log.d(TAG, "Connecting to " + SERVER_ADDRESS);

		long start = System.currentTimeMillis();

		String filePath = getPath();
		FileOutputStream output = getOutputStream(filePath);
		HttpURLConnection conn = (HttpURLConnection) remoteUrl.openConnection();
		conn.connect();

//		if (conn.getResponseCode() != 200) {
//			Log.d(TAG, "Detected bad response code: " + conn.getResponseCode());
//			throw new HttpResponseException(conn.getResponseCode(), "Invalid response: " + conn.getResponseCode() + "");
//		}

		InputStream input = conn.getInputStream();
		byte[] buffer = new byte[1024];
		int bytesRead = 0;

		while ((bytesRead = input.read(buffer)) > 0) {
			output.write(buffer, 0, bytesRead);
			
			if (input.available() <= 0) {
				// The file is done
				broadcastChange(filePath);
				filePath = getPath();
				output = getOutputStream(filePath);
				buffer = new byte[1024];
				bytesRead = 0;
			}
		}
		long elapsed = System.currentTimeMillis() - start;
		Log.d(TAG, "GET complete, " + elapsed + "ms");
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

	private void broadcastChange(String apkPath) {
		// FIXME: Need to get these values from elsewhere
		String layoutName = "main";
		String packageName = "com.bignerdranch.Franklin.RogerTest";
		manager.onDownloadComplete(apkPath, layoutName, packageName);
	}
}
