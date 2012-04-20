package com.bignerdranch.franklin.roger;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
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
import android.widget.ProgressBar;
import android.widget.TextView;

public class RogerActivity extends FragmentActivity {
    public static final String TAG = "RogerActivity";

    private static String SERVER_SELECT = "SelectServer";
    private static String THE_MANAGEMENT = "Management";

    private DownloadManager manager;

    private TheManagement management;
    
    private TextView serverNameTextView;
    private TextView connectionStatusTextView;
    private FrameLayout container;
    private ViewGroup rootContainer;
    private ViewGroup containerBorder;
    private ProgressBar discoveryProgressBar;

    private static class TheManagement extends Fragment {
        public LayoutDescription layoutDescription;

        @Override
        public void onCreate(Bundle sharedInstanceState) {
            super.onCreate(sharedInstanceState);
            setRetainInstance(true);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            ConnectionHelper.getInstance(getActivity())
                .connectToServer(null);
        }
    }

    private DiscoveryHelper.Listener discoveryListener = new DiscoveryHelper.Listener() {
        public void onStateChanged(DiscoveryHelper discover) {
            if (discoveryProgressBar != null) {
                discoveryProgressBar.post(new Runnable() { public void run() {
                    updateDiscovery();
                }});
            }
        }
    };
    
    private ConnectionHelper.Listener connectionStateListener = new ConnectionHelper.Listener() {
        public void onStateChanged(ConnectionHelper connector) {
            if (connectionStatusTextView != null) {
                connectionStatusTextView.post(new Runnable() { public void run() {
                    updateServerStatus();
                }});
            }
        }
    };
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        manager = DownloadManager.getInstance();
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.main);

        serverNameTextView = (TextView)findViewById(R.id.serverNameTextView);
        connectionStatusTextView = (TextView)findViewById(R.id.connectionStatusTextView);
        
        rootContainer = (ViewGroup)findViewById(R.id.main_root);
        container = (FrameLayout)findViewById(R.id.container);
        containerBorder = (ViewGroup)findViewById(R.id.main_container_border);
        containerBorder.setVisibility(View.GONE);
        discoveryProgressBar = (ProgressBar)findViewById(R.id.discoveryProgressBar);

        DiscoveryHelper.getInstance(this)
            .addListener(discoveryListener);

        ConnectionHelper connector = ConnectionHelper.getInstance(this);
        if (connector.getState() == ConnectionHelper.STATE_DISCONNECTED || connector.getState() == ConnectionHelper.STATE_FAILED) {
            refreshServers();
        }

        connector.addListener(connectionStateListener);

        management = (TheManagement)getSupportFragmentManager()
            .findFragmentByTag(THE_MANAGEMENT);

        if (management == null) {
            management = new TheManagement();
            getSupportFragmentManager().beginTransaction()
                .add(management, THE_MANAGEMENT)
                .commit();
        }

        if (management.layoutDescription != null) {
            loadLayout(management.layoutDescription);
        }

        updateServerStatus();
        updateDiscovery();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ConnectionHelper.getInstance(this)
            .removeListener(connectionStateListener);
        DiscoveryHelper.getInstance(this)
            .removeListener(discoveryListener);
    }

    private void updateDiscovery() {
        DiscoveryHelper discover = DiscoveryHelper.getInstance(this);

        if (discover.getState() == DiscoveryHelper.STATE_DISCOVERING) {
            discoveryProgressBar.setVisibility(View.VISIBLE);
        } else {
            discoveryProgressBar.setVisibility(View.GONE);
        }
    }

    private void updateServerStatus() {
        ConnectionHelper connector = ConnectionHelper.getInstance(this);

        ServerDescription desc = connector.getConnectedServer();

        if (desc != null) {
            serverNameTextView.setText(desc.getName());
            serverNameTextView.setVisibility(View.VISIBLE);
            connectionStatusTextView.setVisibility(View.VISIBLE);
        } else {
            serverNameTextView.setText("");
            serverNameTextView.setVisibility(View.GONE);
            connectionStatusTextView.setVisibility(View.GONE);
        }

        String state = null;

        switch (connector.getState()) {
            case ConnectionHelper.STATE_CONNECTING:
                state = "Connecting...";
                break;
            case ConnectionHelper.STATE_CONNECTED:
                state = "";
                break;
            case ConnectionHelper.STATE_FAILED:
                state = "Connection failed";
                break;
            case ConnectionHelper.STATE_DISCONNECTED:
                state = "Disconnected";
                break;
            default:
                break;
        }

        connectionStatusTextView.setText(state);
    }

    private BroadcastReceiver foundServersReceiver = new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
            ArrayList<?> addresses = (ArrayList<?>)i.getSerializableExtra(FindServerService.EXTRA_IP_ADDRESSES);
            if (addresses == null || addresses.size() == 0) return;

            ConnectionHelper connector = ConnectionHelper.getInstance(c);
            int state = connector.getState();
            if (addresses.size() == 1 && 
                    (state == ConnectionHelper.STATE_DISCONNECTED || state == ConnectionHelper.STATE_FAILED)) {
                // auto connect
                connector.connectToServer((ServerDescription)addresses.get(0));
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
    
    private void loadLayout(LayoutDescription description) {
        management.layoutDescription = description;

    	container.removeAllViews();

        int id = description.getResId(this);
    	
        if (id == 0) {
        	Log.e(TAG, "ID is 0. Not inflating.");
        	ErrorManager.show(getApplicationContext(), rootContainer, "Unable to load view");
        	containerBorder.setVisibility(View.GONE);
        	return;
        }
        
        Log.i(TAG, "getting a layout inflater...");
        LayoutInflater inflater = description.getApk(this).getLayoutInflater(getLayoutInflater());
        Log.i(TAG, "inflating???");
        View v = inflater.inflate(id, container, false);

        container.addView(v);
        containerBorder.setVisibility(View.VISIBLE);
    }

    protected void refreshServers() {
        DiscoveryHelper.getInstance(this)
            .startDiscovery();
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
		public void onApkDownloaded(final LayoutDescription description) {
			Log.d(TAG, "New apk with path: " + description.getApkPath());
			container.post(new Runnable() {
				public void run() {
					loadLayout(description);
				}
			});
		}
    	
    };
}
