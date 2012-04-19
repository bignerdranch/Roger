package com.bignerdranch.franklin.roger;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;

import android.os.Bundle;

import android.support.v4.app.DialogFragment;

import android.util.Log;

import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

public class SelectServerDialog extends DialogFragment {
    public static final String TAG = "SelectServerDialog";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)  {
        final ArrayList<String> addresses = (ArrayList<String>)getArguments()
            .getSerializable(FindServerService.EXTRA_IP_ADDRESSES);

        ListAdapter adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, addresses);

        return new AlertDialog.Builder(getActivity())
            .setAdapter(adapter, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Log.i(TAG, "selected server: " + addresses.get(which) + "");
                }
            })
            .create();
    }
}
