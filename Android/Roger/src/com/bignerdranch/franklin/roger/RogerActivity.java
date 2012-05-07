package com.bignerdranch.franklin.roger;

import java.util.ArrayList;

import com.bignerdranch.franklin.roger.model.Persistence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
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

public class RogerActivity extends AutoFragmentActivity {
    public static final String TAG = "RogerActivity";

    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
    }

    public static class Auto extends Fragment {
        private static String SERVER_SELECT = "SelectServer";
        private static final String LAYOUT_PARAM_DIALOG_TAG = "RogerActivity.layoutParamsDialog";

        private TextView serverNameTextView;
        private TextView connectionStatusTextView;
        private FrameLayout container;
        private ViewGroup containerBorder;
        private ProgressBar discoveryProgressBar;

        private Persistence persistence;
        private Persistence.Listener persistenceListener = new Persistence.Listener() { public void update() {
            updateLayoutParams();
            loadResource();
        }};

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
            persistence = Persistence.getInstance(getActivity());
            persistence.addListener(persistenceListener);
            if (persistence.getRogerParams() == null) {
                float density = getResources().getDisplayMetrics().density;
                persistence.setRogerParams(new RogerParams(density, new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, 
                        ViewGroup.LayoutParams.WRAP_CONTENT)));
            }
        }

        /** Called when the activity is first created. */
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.main, parent, false);

            serverNameTextView = (TextView)v.findViewById(R.id.serverNameTextView);
            connectionStatusTextView = (TextView)v.findViewById(R.id.connectionStatusTextView);
            
            container = (FrameLayout)v.findViewById(R.id.container);
            containerBorder = (ViewGroup)v.findViewById(R.id.main_container_border);
            containerBorder.setVisibility(View.GONE);
            discoveryProgressBar = (ProgressBar)v.findViewById(R.id.discoveryProgressBar);

            DiscoveryHelper.getInstance(getActivity())
                .addListener(discoveryListener);

            ConnectionHelper connector = ConnectionHelper.getInstance(getActivity());
            if (connector.getState() == ConnectionHelper.STATE_DISCONNECTED || connector.getState() == ConnectionHelper.STATE_FAILED) {
                refreshServers();
            }

            connector.addListener(connectionStateListener);

            updateServerStatus();
            updateDiscovery();

            return v;
        }

        @Override
        public void onViewCreated(View v, Bundle savedInstanceState) {
            if (persistence.getLayoutDescription() != null) {
                loadResource(persistence.getLayoutDescription());
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            persistence.removeListener(persistenceListener);
            ConnectionHelper.getInstance(getActivity())
                .removeListener(connectionStateListener);
            DiscoveryHelper.getInstance(getActivity())
                .removeListener(discoveryListener);
        }

        private void updateDiscovery() {
            DiscoveryHelper discover = DiscoveryHelper.getInstance(getActivity());

            if (discover.getState() == DiscoveryHelper.STATE_DISCOVERING) {
                discoveryProgressBar.setVisibility(View.VISIBLE);
            } else {
                discoveryProgressBar.setVisibility(View.GONE);
            }
        }
        
        private void updateLayoutParams() {
            Log.i(TAG, "updated");
            if (container == null) {
                Log.i(TAG, "container null");
                return;
            }

            RogerParams params = persistence.getRogerParams();
            
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
            ConnectionHelper connector = ConnectionHelper.getInstance(getActivity());

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

                FragmentManager fm = getActivity().getSupportFragmentManager();
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
            LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(foundServersReceiver, filter);
        }
        
        @Override
        public void onPause() {
            super.onPause();
            LocalBroadcastManager.getInstance(getActivity())
                .unregisterReceiver(foundServersReceiver);
        }

        private void loadResource() {
            if (persistence.getLayoutDescription() != null) {
                loadResource(persistence.getLayoutDescription());
            }
        }

        private boolean isInvalidAndroidVersion(LayoutDescription description) {
            if (description.getMinVersion() != 0 && description.getMinVersion() > Build.VERSION.SDK_INT) {
                return true;
            } else {
                return false;
            }
        }
        
        private void loadResource(LayoutDescription description) {
            persistence.setLayoutDescription(description);
            
            if (description.getLayoutType().equals("layout")) {
                loadLayout(description);
            } else if (description.getLayoutType().equals("drawable")) {
                Log.i(TAG, "loading drawable");
                loadDrawable(description);
            }
        }

        private void loadDrawable(LayoutDescription description) {
            final int id = description.getResId(getActivity());
            if (id == 0) {
                Log.e(TAG, "ID is 0. Not displaying drawable.");
                Log.e(TAG, "    description was: " + description + "");
                String layoutError = getString(R.string.error_zero_layout_id);
                ErrorManager.show(getActivity().getApplicationContext(), layoutError);
                containerBorder.setVisibility(View.GONE);
                return;
            }

            container.removeAllViews();
            updateLayoutParams();

            try {
                Drawable drawable = description.getApk(getActivity()).getResources().getDrawable(id);
                Log.i(TAG, "drawable is: " + drawable + "");
                Log.i(TAG, "   dimens: (" + drawable.getIntrinsicWidth() + "," + drawable.getIntrinsicHeight() + ")");
                ImageView imageView = new ImageView(getActivity());
                imageView.setImageDrawable(drawable);
                imageView.setClickable(true);
                container.addView(imageView);

                ViewGroup.LayoutParams params = imageView.getLayoutParams();
                if (drawable.getIntrinsicWidth() == -1) {
                    params.width = ViewGroup.LayoutParams.FILL_PARENT;
                }
                if (drawable.getIntrinsicHeight() == -1) {
                    params.height = ViewGroup.LayoutParams.FILL_PARENT;
                }
                imageView.setLayoutParams(params);

                containerBorder.setVisibility(View.VISIBLE);
            } catch (RuntimeException ex) {
                Log.e(TAG, "failed to inflate and set drawable", ex);

                if (isInvalidAndroidVersion(description)) {
                    // maybe that's why?
                    Log.e(TAG, "invalid version of Android");
                    
                    String versionFormat = getString(R.string.error_old_android_version_format);
                    ErrorManager.show(getActivity().getApplicationContext(), String.format(versionFormat, description.getMinVersion()));
                    containerBorder.setVisibility(View.GONE);
                    return;
                } else {
                    Throwable cause = ex;
                    while (cause.getCause() != null) {
                        cause = cause.getCause();
                    }

                    Log.i(TAG, "root cause", cause);
                    ErrorManager.show(getActivity().getApplicationContext(), cause.getMessage());
                }
            }
        }

        private void loadLayout(LayoutDescription description) {
            container.removeAllViews();
            updateLayoutParams();

            Log.i(TAG, "loading layout: " + description + "");
            final int id = description.getResId(getActivity());
            
            if (id == 0) {
                Log.e(TAG, "ID is 0. Not inflating.");
                Log.e(TAG, "    description was: " + description + "");
                String layoutError = getString(R.string.error_zero_layout_id);
                ErrorManager.show(getActivity().getApplicationContext(), layoutError);
                containerBorder.setVisibility(View.GONE);
                return;
            }
            
            Log.i(TAG, "getting a layout inflater...");
            final LayoutInflater inflater = description.getApk(getActivity())
                .getLayoutInflater(getActivity().getLayoutInflater());
            Log.i(TAG, "inflating???");
            try {
                if (persistence.isListView()) {
                    FrameLayout.LayoutParams params = 
                        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);

                    ListView listView = new ListView(getActivity());

                    ArrayList<String> items = new ArrayList<String>();
                    for (int i = 0; i < 100; i++) {
                        items.add("" + i);
                    }

                    listView.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, items) {
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
                if (isInvalidAndroidVersion(description)) {
                    // maybe that's why?
                    Log.e(TAG, "invalid version of Android");
                    
                    String versionFormat = getString(R.string.error_old_android_version_format);
                    ErrorManager.show(getActivity().getApplicationContext(), String.format(versionFormat, description.getMinVersion()));
                    containerBorder.setVisibility(View.GONE);
                    return;
                } else {
                    Throwable cause = ex;
                    while (cause.getCause() != null) {
                        cause = cause.getCause();
                    }

                    Log.i(TAG, "root cause", cause);
                    ErrorManager.show(getActivity().getApplicationContext(), cause.getMessage());
                }
            }
        }

        private void addTextFill() {
            addTextFill(container);
        }
        
        private void addTextFill(View view) {
            if (persistence.getIsTextFillEnabled() == null) {
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
                
                if (persistence.getIsTextFillEnabled()) {
                    textView.setText(dummyText);
                } else {
                    textView.setText(oldText);
                }
            }
        }

        protected void showLayoutParamsDialog() {
            LayoutDialogFragment dialog = LayoutDialogFragment.newInstance(persistence.getRogerParams());
            dialog.show(getActivity().getSupportFragmentManager(), LAYOUT_PARAM_DIALOG_TAG);
        }
        
        protected void refreshServers() {
            DiscoveryHelper.getInstance(getActivity())
                .startDiscovery();
        }

        private void updateTextFill() {
            if (persistence.getIsTextFillEnabled() == null || !persistence.getIsTextFillEnabled()) {
                persistence.setIsTextFillEnabled(true);
            } else {
                persistence.setIsTextFillEnabled(false);
            }
        }

        private void toggleListView() {
            persistence.setListView(!persistence.isListView());
        }
        
        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.menu, menu);
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
        public void onStart() {
            super.onStart();
            IntentFilter filter = new IntentFilter(Constants.ACTION_NEW_LAYOUT);
            Log.i(TAG, "registering reciever for " + Constants.ACTION_NEW_LAYOUT + "");
            getActivity().registerReceiver(downloadReceiver, filter);

        }
        
        @Override
        public void onStop() {
            super.onStop();
            Log.i(TAG, "unregistering reciever");
            getActivity().unregisterReceiver(downloadReceiver);
        }

        private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
            public void onReceive(Context c, Intent i) {
                Log.i(TAG, "received intent: " + i + "");

                if (!Constants.ACTION_NEW_LAYOUT.equals(i.getAction())) return;

                Log.i(TAG, "    it's a layout...");
                LayoutDescription desc = (LayoutDescription)i
                    .getSerializableExtra(Constants.EXTRA_LAYOUT_DESCRIPTION);

                if (desc == null) {
                    Log.i(TAG, "    building from parts");
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
                Log.i(TAG, "    and it is: " + desc + "");

                if (desc != null) {
                    LayoutDescription old = persistence.getLayoutDescription();
                    if (old != null && old.getTxnId() == desc.getTxnId()) {
                        Log.i(TAG, "already seen this txnId: " + desc.getTxnId() + " path: " + desc.getApkPath() + "");
                    } else {
                        loadResource(desc);
                    }
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
}
