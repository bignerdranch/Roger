package com.bignerdranch.franklin.roger;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;

import android.support.v4.app.DialogFragment;

import android.util.Log;

import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

public class SelectServerDialog extends DialogFragment {
    public static final String TAG = "SelectServerDialog";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)  {
        final ArrayList<ServerDescription> addresses = (ArrayList<ServerDescription>)getArguments()
            .getSerializable(FindServerService.EXTRA_IP_ADDRESSES);

        ListAdapter adapter = new ArrayAdapter<ServerDescription>(getActivity(), R.layout.server_item, addresses);

        return new AlertDialog.Builder(getActivity())
            .setAdapter(adapter, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Log.i(TAG, "selected server: " + addresses.get(which) + "");
                    Intent i = new Intent(getActivity(), DownloadService.class);
                    i.setAction(DownloadService.ACTION_CONNECT);
                    i.putExtra(DownloadService.EXTRA_SERVER_DESCRIPTION, addresses.get(which));
                    getActivity().startService(i);
                }
            })
            .create();
    }
}
