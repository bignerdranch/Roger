package com.bignerdranch.franklin.roger;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

public class RogerActivity extends FragmentActivity {
    public static final String TAG = "RogerActivity";

    private static String SERVER_SELECT = "SelectServer";

    private DownloadManager manager;
    
    private FrameLayout container;
    private ViewGroup rootContainer;
    private ViewGroup containerBorder;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        manager = DownloadManager.getInstance();
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.main);
        
        rootContainer = (ViewGroup)findViewById(R.id.main_root);
        container = (FrameLayout)findViewById(R.id.container);
        containerBorder = (ViewGroup)findViewById(R.id.main_container_border);
        containerBorder.setVisibility(View.GONE);

        ConnectionHelper helper = ConnectionHelper.getInstance(this);
        if (helper.getState() == ConnectionHelper.STATE_DISCONNECTED || helper.getState() == ConnectionHelper.STATE_FAILED) {
            refreshServers();
        }
    }

    private BroadcastReceiver foundServersReceiver = new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
            ArrayList<?> addresses = (ArrayList<?>)i.getSerializableExtra(FindServerService.EXTRA_IP_ADDRESSES);
            if (addresses == null || addresses.size() == 0) return;

            ConnectionHelper helper = ConnectionHelper.getInstance(c);
            int state = helper.getState();
            if (addresses.size() == 1 && 
                    (state == ConnectionHelper.STATE_DISCONNECTED || state == ConnectionHelper.STATE_FAILED)) {
                // auto connect
                helper.connectToServer((ServerDescription)addresses.get(0));
                return;
            }

            Bundle args = new Bundle();
            args.putSerializable(FindServerService.EXTRA_IP_ADDRESSES, addresses);

            FragmentManager fm = getSupportFragmentManager();
            if (fm.findFragmentByTag(SERVER_SELECT) == null) {
                DialogFragment f = new SelectServerDialog();
                f.setArguments(args);
                f.show(fm, SERVER_SELECT);
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(FindServerService.ACTION_FOUND_SERVERS);
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(foundServersReceiver, filter);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(foundServersReceiver);
    }
    
    private void loadApk(String apkPath, String layoutName, String packageName) {
    	container.removeAllViews();
    	
    	Log.d(TAG, "Loading apk with path: " + apkPath + " layout: " + layoutName + " package: " + packageName);
        AssetsApk apk = new AssetsApk(this, packageName, apkPath);
        Log.i(TAG, "Did it work? " + apk.getFile().exists() + "");
        Resources r = apk.getResources();
        Log.i(TAG, "hmm, still alive");

        Log.i(TAG, "getting identifier for layoutName " + layoutName + ", packageName " + packageName + "");
        int id = r.getIdentifier(layoutName, "layout", packageName);
        Log.i(TAG, "here's what we got: " + id + "");

        if (id == 0) {
        	Log.e(TAG, "ID is 0. Not inflating.");
        	ErrorManager.show(getApplicationContext(), rootContainer, "Unable to load view");
        	containerBorder.setVisibility(View.GONE);
        	return;
        }
        
        Log.i(TAG, "getting a layout inflater...");
        LayoutInflater inflater = apk.getLayoutInflater(getLayoutInflater());
        Log.i(TAG, "inflating???");
        View v = inflater.inflate(id, container, false);

        container.addView(v);
        containerBorder.setVisibility(View.VISIBLE);
    }

    protected void refreshServers() {
        Intent i = new Intent(this, FindServerService.class);
        startService(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            refreshServers();
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
