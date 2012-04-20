package com.bignerdranch.franklin.roger;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class ErrorManager {

	public static void show(Context context, ViewGroup root, String message) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View layout = inflater.inflate(R.layout.error_layout, null);

		TextView text = (TextView) layout.findViewById(R.id.error_layout_text);
		text.setText(message);

		Toast toast = new Toast(context);
		toast.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.TOP, 0, 0);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.setView(layout);
		toast.show();
	}
}
