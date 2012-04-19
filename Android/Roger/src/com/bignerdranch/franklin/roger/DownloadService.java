package com.bignerdranch.franklin.roger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpEntity;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class DownloadService extends IntentService {
	private static final String TAG = "DownloadService";
	
	private static final String SERVER_ADDRESS = "http://10.1.10.108:8082/";
	
	public DownloadService() {
		super("DownloadService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

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

		File filePath = File.createTempFile("roger", "", getFilesDir());
		FileOutputStream output = new FileOutputStream(filePath);
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
			String data = new String(buffer);
			Log.d(TAG, "Got data: " + data);
			output.write(buffer, 0, bytesRead);
			
			if (input.available() <= 0) {
				// The file is done
				broadcastChange(filePath.getAbsolutePath());
			}
		}
		long elapsed = System.currentTimeMillis() - start;
		Log.d(TAG, "GET complete, " + elapsed + "ms");
	}

	private void broadcastChange(String apkPath) {
//		Intent downloadIntent = new Intent(action);
//		sendBroadcast(downloadIntent);
	}
}
