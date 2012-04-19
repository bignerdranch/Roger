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

public class DownloadService extends IntentService {
	private static final String TAG = "DownloadService";
	
	private static final String PACKAGE = "com.bignerdranch.franklin.roger.dummypackage";
	private static final char INFO_PREFIX = '-';
	
	private static final String SERVER_ADDRESS = "http://10.1.10.57:8082/";
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

		InputStream input = conn.getInputStream();
		byte[] buffer = new byte[1024];
		int bytesRead = 0;
		String layoutFile = "";
		boolean firstPass = true;
		
		while ((bytesRead = input.read(buffer)) > 0) {
			int byteOffset = 0;
			if (firstPass && buffer[0] == INFO_PREFIX) {
				
				int lastIndex;
				for (lastIndex = 1; lastIndex < buffer.length; lastIndex++) {
					if (buffer[lastIndex] == INFO_PREFIX) {
						break;
					}
				}
				
				byte[] layoutBuffer = new byte[lastIndex - 1];
				for (int i = 0; i < lastIndex - 1; i++) {
					layoutBuffer[i] = buffer[i + 1];
				}
				
				layoutFile = new String(layoutBuffer);
				Log.d(TAG, "Got layout file: " + layoutFile + " length " + layoutFile.length());
				byteOffset = 2 + layoutFile.length();
				bytesRead -= byteOffset;
				
				if (layoutFile.endsWith(".xml")) {
					layoutFile = layoutFile.substring(0, layoutFile.length() - 4);
				}
			}
			
			output.write(buffer, byteOffset, bytesRead);
			
			if (input.available() <= 0) {
				// The file is done
				broadcastChange(filePath, layoutFile);
				filePath = getPath();
				output = getOutputStream(filePath);
				buffer = new byte[1024];
				bytesRead = 0;
				firstPass = true;
			} else {
				firstPass = false;
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

	private void broadcastChange(String apkPath, String layoutName) {
		manager.onDownloadComplete(apkPath, layoutName, PACKAGE);
	}
}
