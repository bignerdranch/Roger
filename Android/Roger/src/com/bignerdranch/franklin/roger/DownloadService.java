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

    private static final String PACKAGE = "com.bignerdranch.franklin.roger.dummypackage";
	//private static final String PACKAGE = "com.bignerdranch.franklin.roger";
	//private static final String PACKAGE = "com.att.labs.uversetv.android.tablet";
	//private static final String PACKAGE = "com.bignerdranch.Franklin.RogerTest";

	private static final char INFO_PREFIX = '-';

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
			Pair<String, String> descriptors = getDescriptor();
			String filePath = getApk(descriptors.second);
			broadcastChange(filePath, descriptors.first);
		} catch (IOException e) {
			Log.e(TAG, "Unable to download file", e);
		}
		
		startService(new Intent(this, DownloadService.class));
	}

	private Pair<String, String> getDescriptor() throws IOException {

		URL remoteUrl = new URL(SERVER_ADDRESS);
		Log.d(TAG, "Connecting to " + SERVER_ADDRESS);

        InputStream input;
        synchronized (this) {
            connection = (HttpURLConnection) remoteUrl.openConnection();
            connection.connect();

            input = connection.getInputStream();
        }

		byte[] buffer = new byte[1024];
		int bytesRead = 0;
		String layoutFile = "main";
		String identifier = "";
		
		while ((bytesRead = input.read(buffer)) > 0) {
			String data = new String(buffer);
			
			String response = data.substring(0, data.indexOf("--"));
			Log.d(TAG, "Got response: " + response);
			
			String[] values = response.split("\n");
			layoutFile = values[0];
            layoutFile = layoutFile.split("\\.")[0];
			identifier = values[1];
			
			Log.d(TAG, "Layout file: " + layoutFile + " identifier " + identifier);
		}
		
		return new Pair<String, String>(layoutFile, identifier);
//			int byteOffset = 0;
//			if (firstPass && buffer[0] == INFO_PREFIX) {
//				
//				int lastIndex;
//				for (lastIndex = 1; lastIndex < buffer.length; lastIndex++) {
//					if (buffer[lastIndex] == INFO_PREFIX) {
//						break;
//					}
//				}
//				
//				byte[] layoutBuffer = new byte[lastIndex - 1];
//				for (int i = 0; i < lastIndex - 1; i++) {
//					layoutBuffer[i] = buffer[i + 1];
//				}
//				
//				layoutFile = new String(layoutBuffer);
//				Log.d(TAG, "Got layout file: " + layoutFile + " length " + layoutFile.length());
//				byteOffset = 2 + layoutFile.length();
//				bytesRead -= byteOffset;
//				
//				if (layoutFile.endsWith(".xml")) {
//					layoutFile = layoutFile.substring(0, layoutFile.length() - 4);
//				}
//			}
//			
//			output.write(buffer, byteOffset, bytesRead);
//			
//			if (input.available() <= 0) {
//				// The file is done
//				broadcastChange(filePath, layoutFile);
//				filePath = getPath();
//				output = getOutputStream(filePath);
//				buffer = new byte[1024];
//				bytesRead = 0;
//				firstPass = true;
//			} else {
//				firstPass = false;
//			}
//		}
//		long elapsed = System.currentTimeMillis() - start;
//		Log.d(TAG, "GET complete, " + elapsed + "ms");
	}
	
	private String getApk(String identifier) throws IOException {
		String address = String.format(SERVER_APK_ADDRESS, identifier);
		URL remoteUrl = new URL(address);
		Log.d(TAG, "Connecting to " + address);

		String filePath = getPath();
		FileOutputStream output = getOutputStream(filePath);
        InputStream input;
        synchronized (this) {
            connection = (HttpURLConnection) remoteUrl.openConnection();
            connection.connect();
            input = connection.getInputStream();
            Log.d(TAG, "Content length " + connection.getContentLength());
            input = connection.getInputStream();
        }

		byte[] buffer = new byte[1024];
		int bytesRead = 0;
		int bytesWritten = 0;
		
		while ((bytesRead = input.read(buffer)) > 0) {
			output.write(buffer, 0, bytesRead);
			bytesWritten += bytesRead;
		}
		
		Log.d(TAG, "Wrote " + bytesWritten + " bytes");
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

	private void broadcastChange(String apkPath, String layoutName) {
		manager.onDownloadComplete(apkPath, layoutName, PACKAGE);
	}
}
