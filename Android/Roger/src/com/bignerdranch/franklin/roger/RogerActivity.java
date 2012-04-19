package com.bignerdranch.franklin.roger;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

public class RogerActivity extends Activity {
    public static final String TAG = "RogerActivity";

    private DownloadManager manager;
    
    private FrameLayout container;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        manager = DownloadManager.getInstance();
        
        setContentView(R.layout.main);
        container = (FrameLayout)findViewById(R.id.container);
        
        startService();
    }
    
    private void loadApk(String apkPath, String layoutName, String packageName) {
    	container.removeAllViews();
    	
//        String apkName = "YellowBook.apk";
//        String packageName = "com.serco.yellowbook";
        AssetsApk apk = new AssetsApk(this, packageName, apkPath);
        Log.i(TAG, "Did it work? " + apk.getFile().exists() + "");
        Resources r = apk.getResources();
        Log.i(TAG, "hmm, still alive");

        Log.i(TAG, "let's try to get a resource id");
        int id = r.getIdentifier(layoutName, "layout", packageName);
        Log.i(TAG, "here's what we got: " + id + "");

        if (id == 0) {
        	Log.e(TAG, "ID is 0. Not inflating.");
        	return;
        }
        
        Log.i(TAG, "getting a layout inflater...");
        LayoutInflater inflater = apk.getLayoutInflater(getLayoutInflater());
        Log.i(TAG, "inflating???");
        View v = inflater.inflate(id, container, false);

        container.addView(v);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            Intent i = new Intent(this, FindServerService.class);
            startService(i);
        }

        return true;
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	manager.setDownloadListener(downloadListener);
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	manager.setDownloadListener(null);
    }
    
    private void startService() {
    	Intent service = new Intent(RogerActivity.this, DownloadService.class);
    	startService(service);
    }
    
    private DownloadManager.DownloadListener downloadListener = new DownloadManager.DownloadListener() {

		@Override
		public void onApkDownloaded(final String path, final String layoutName, final String packageName) {
			Log.d(TAG, "New apk with path: " + path);
			container.post(new Runnable() {
				public void run() {
					loadApk(path, layoutName, packageName);
				}
			});
		}
    	
    };
}
