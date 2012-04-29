package com.bignerdranch.franklin.roger;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bignerdranch.franklin.roger.model.RogerParams;
import com.bignerdranch.franklin.roger.pair.ConnectionHelper;
import com.bignerdranch.franklin.roger.pair.DiscoveryHelper;
import com.bignerdranch.franklin.roger.Constants;
import com.bignerdranch.franklin.roger.pair.SelectServerDialog;
import com.bignerdranch.franklin.roger.pair.ServerDescription;
import com.bignerdranch.franklin.roger.util.ViewUtils;

public class RogerActivity extends FragmentActivity {
    public static final String TAG = "RogerActivity";

    private static String SERVER_SELECT = "SelectServer";
    private static String THE_MANAGEMENT = "Management";
    private static final String LAYOUT_PARAM_DIALOG_TAG = "RogerActivity.layoutParamsDialog";

    private TheManagement management;
    
    private TextView serverNameTextView;
    private TextView connectionStatusTextView;
    private FrameLayout container;
    private ViewGroup containerBorder;
    private ProgressBar discoveryProgressBar;

    private static class TheManagement extends Fragment {
        public LayoutDescription layoutDescription;
        public RogerParams rogerParams;
        public boolean textFillSet;
        public boolean textFillEnabled;
        public boolean isListView;

        @Override
        public void onCreate(Bundle sharedInstanceState) {
            super.onCreate(sharedInstanceState);
            setRetainInstance(true);
            Log.i(TAG, Constants.ACTION_NEW_LAYOUT);
            Log.i(TAG, Constants.EXTRA_LAYOUT_DESCRIPTION);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            ConnectionHelper.getInstance(getActivity())
                .connectToServer(null);
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.main);

        serverNameTextView = (TextView)findViewById(R.id.serverNameTextView);
        connectionStatusTextView = (TextView)findViewById(R.id.connectionStatusTextView);
        
        container = (FrameLayout)findViewById(R.id.container);
        containerBorder = (ViewGroup)findViewById(R.id.main_container_border);
        containerBorder.setVisibility(View.GONE);
        discoveryProgressBar = (ProgressBar)findViewById(R.id.discoveryProgressBar);

        DiscoveryHelper.getInstance(this)
            .addListener(discoveryListener);

        management = (TheManagement)getSupportFragmentManager()
            .findFragmentByTag(THE_MANAGEMENT);

        if (management == null) {
            management = new TheManagement();
            getSupportFragmentManager().beginTransaction()
                .add(management, THE_MANAGEMENT)
                .commit();
        }

        if (management.layoutDescription != null) {
            loadResource(management.layoutDescription);
        }

        if (management.rogerParams == null) {
        	float density = getResources().getDisplayMetrics().density;
    		management.rogerParams = new RogerParams(density, new ViewGroup.LayoutParams(
    				ViewGroup.LayoutParams.WRAP_CONTENT, 
    				ViewGroup.LayoutParams.WRAP_CONTENT));
    	}
        
        ConnectionHelper connector = ConnectionHelper.getInstance(this);
        if (connector.getState() == ConnectionHelper.STATE_DISCONNECTED || connector.getState() == ConnectionHelper.STATE_FAILED) {
            refreshServers();
        }

        connector.addListener(connectionStateListener);

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
    
    public void setRogerParams(RogerParams params) {
    	management.rogerParams = params;
    	updateLayoutParams(params);
        loadResource();
    }
    
    private void updateLayoutParams(RogerParams params) {
    	if (container == null) {
    		return;
    	}
    	
    	int width = params.getWidthParam();
    	int height = params.getHeightParam();
    	
    	FrameLayout.LayoutParams actualParams = new FrameLayout.LayoutParams(width, height);
    	container.setLayoutParams(actualParams);
    	
    	int containerWidth = ViewGroup.LayoutParams.WRAP_CONTENT;
    	int containerHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
    	if (width == ViewGroup.LayoutParams.FILL_PARENT) {
    		containerWidth = width;
    	}
    	
    	if (height == ViewGroup.LayoutParams.FILL_PARENT) {
    		containerHeight = height;
    	}
    	
    	FrameLayout.LayoutParams containerParams = (FrameLayout.LayoutParams) containerBorder.getLayoutParams();
    	containerParams.width = containerWidth;
    	containerParams.height = containerHeight;
    	containerBorder.setLayoutParams(containerParams);
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

        int stateResId = 0;
        switch (connector.getState()) {
            case ConnectionHelper.STATE_CONNECTING:
                stateResId = R.string.connection_state_connecting;
                break;
            case ConnectionHelper.STATE_CONNECTED:
                stateResId = R.string.connection_state_connected;
                break;
            case ConnectionHelper.STATE_FAILED:
                stateResId = R.string.connection_state_failed;
                break;
            case ConnectionHelper.STATE_DISCONNECTED:
                stateResId = R.string.connection_state_disconnected;
                break;
            case ConnectionHelper.STATE_DOWNLOADING:
                stateResId = R.string.connection_state_downloading;
                break;
            default:
                break;
        }

        connectionStatusTextView.setText(stateResId);
    }

    private BroadcastReceiver foundServersReceiver = new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
            ArrayList<?> addresses = (ArrayList<?>)i.getSerializableExtra(Constants.EXTRA_IP_ADDRESSES);
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
            args.putSerializable(Constants.EXTRA_IP_ADDRESSES, addresses);

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
        IntentFilter filter = new IntentFilter(Constants.ACTION_FOUND_SERVERS);
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(foundServersReceiver, filter);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(foundServersReceiver);
    }

    private void loadResource() {
        if (management.layoutDescription != null) {
            loadResource(management.layoutDescription);
        }
    }
    
    private void loadResource(LayoutDescription description) {
        management.layoutDescription = description;

        if (description.getMinVersion() != 0 && description.getMinVersion() > Build.VERSION.SDK_INT) {
        	Log.e(TAG, "invalid version of Android");
        	
        	String versionFormat = getString(R.string.error_old_android_version_format);
        	ErrorManager.show(getApplicationContext(), String.format(versionFormat, description.getMinVersion()));
        	containerBorder.setVisibility(View.GONE);
        	return;
        }
        
        if (description.getLayoutType().equals("layout")) {
            loadLayout(description);
        } else if (description.getLayoutType().equals("drawable")) {
            loadDrawable(description);
        }
    }

    private void loadDrawable(LayoutDescription description) {
        final int id = description.getResId(this);
        if (id == 0) {
            Log.e(TAG, "ID is 0. Not displaying drawable.");
            Log.e(TAG, "    description was: " + description + "");
        	String layoutError = getString(R.string.error_zero_layout_id);
        	ErrorManager.show(getApplicationContext(), layoutError);
        	containerBorder.setVisibility(View.GONE);
            return;
        }
    }

    private void loadLayout(LayoutDescription description) {
    	container.removeAllViews();
    	updateLayoutParams(management.rogerParams);

        final int id = description.getResId(this);
    	
        if (id == 0) {
        	Log.e(TAG, "ID is 0. Not inflating.");
        	Log.e(TAG, "    description was: " + description + "");
        	String layoutError = getString(R.string.error_zero_layout_id);
        	ErrorManager.show(getApplicationContext(), layoutError);
        	containerBorder.setVisibility(View.GONE);
        	return;
        }
        
        Log.i(TAG, "getting a layout inflater...");
        final LayoutInflater inflater = description.getApk(this).getLayoutInflater(getLayoutInflater());
        Log.i(TAG, "inflating???");
        try {
            if (management.isListView) {
                FrameLayout.LayoutParams params = 
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);

                ListView listView = new ListView(this);

                ArrayList<String> items = new ArrayList<String>();
                for (int i = 0; i < 100; i++) {
                    items.add("" + i);
                }

                listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        if (convertView != null) {
                            addTextFill(convertView);
                            return convertView;
                        } else {
                            View v = inflater.inflate(id, parent, false);
                            addTextFill(v);
                            return v;
                        }
                    }
                });

                container.addView(listView, params);
            } else {
                View v = inflater.inflate(id, container, false);
                container.addView(v);
            }

            addTextFill();
            containerBorder.setVisibility(View.VISIBLE);
        } catch (InflateException ex) {
            Log.i(TAG, "InflateException", ex);
            Throwable cause = ex;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }

            ErrorManager.show(getApplicationContext(), cause.getMessage());
        }
    }

    private void addTextFill() {
        addTextFill(container);
    }
    
    private void addTextFill(View view) {
    	if (!management.textFillSet) {
    		return;
    	}
    	
    	String dummyText = getString(R.string.dummy_text);
    	ArrayList<TextView> views = ViewUtils.findViewsByClass(view, TextView.class);
    	for (TextView textView : views) {
            String oldText = (String)ViewUtils.getTag(textView, R.id.original_text);
            if (oldText == null) {
                oldText = textView.getText() == null ? "" : textView.getText().toString();
                ViewUtils.setTag(textView, R.id.original_text);
            }
    		
    		if (management.textFillEnabled) {
    			textView.setText(dummyText);
    		} else {
    			textView.setText(oldText);
    		}
    	}
    }

    protected void showLayoutParamsDialog() {
    	LayoutDialogFragment dialog = LayoutDialogFragment.newInstance(management.rogerParams);
    	dialog.show(getSupportFragmentManager(), LAYOUT_PARAM_DIALOG_TAG);
    }
    
    protected void refreshServers() {
        DiscoveryHelper.getInstance(this)
            .startDiscovery();
    }

    private void updateTextFill() {
    	management.textFillSet = true;
    	management.textFillEnabled = !management.textFillEnabled;
        loadResource();
    }

    private void toggleListView() {
        management.isListView = !management.isListView;
        loadResource();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
    	switch (item.getItemId()) {
    	
	    	case R.id.menu_refresh:    		
	    		refreshServers();
	    		break;
	    	
	    	case R.id.menu_layout_options:
	    		showLayoutParamsDialog();
	    		break;
	    		
	    	case R.id.menu_layout_fill_text:
	    		updateTextFill();
	    		break;

            case R.id.menu_toggle_list_view:
                toggleListView();
                break;
    	}
    	
        return true;
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
        IntentFilter filter = new IntentFilter(Constants.ACTION_NEW_LAYOUT);
        Log.i(TAG, "registering reciever for " + Constants.ACTION_NEW_LAYOUT + "");
        registerReceiver(downloadReceiver, filter);

    }
    
    @Override
    protected void onStop() {
    	super.onStop();
        Log.i(TAG, "unregistering reciever");
        unregisterReceiver(downloadReceiver);
    }

    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
            Log.i(TAG, "received intent: " + i + "");

            if (!Constants.ACTION_NEW_LAYOUT.equals(i.getAction())) return;

            LayoutDescription desc = (LayoutDescription)i
                .getSerializableExtra(Constants.EXTRA_LAYOUT_DESCRIPTION);

            if (desc == null) {
                desc = new LayoutDescription();

                String apkPath = i.getStringExtra(Constants.EXTRA_LAYOUT_APK_PATH);
                String layoutName = i.getStringExtra(Constants.EXTRA_LAYOUT_LAYOUT_NAME).split("\\.")[0];
                String layoutType = i.getStringExtra(Constants.EXTRA_LAYOUT_LAYOUT_TYPE).split("\\.")[0];
                String packageName = i.getStringExtra(Constants.EXTRA_LAYOUT_PACKAGE_NAME);
                int minimumVersion = i.getIntExtra(Constants.EXTRA_LAYOUT_MIN_VERSION, 1);
                int txnId = i.getIntExtra(Constants.EXTRA_LAYOUT_TXN_ID, 1);

                desc.setApkPath(apkPath);
                desc.setLayoutName(layoutName);
                desc.setLayoutType(layoutType);
                desc.setPackageName(packageName);
                desc.setMinVersion(minimumVersion);
                desc.setTxnId(txnId);
            }

            if (desc != null) {
                loadResource(desc);
            }
        }
    };
    
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
}
