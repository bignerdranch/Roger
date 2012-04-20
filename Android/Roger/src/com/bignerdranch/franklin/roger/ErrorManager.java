package com.bignerdranch.franklin.roger;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.Toast;

public class ErrorManager {

	public static void show(Context context, ViewGroup root, String message) {
//		LayoutInflater inflater = LayoutInflater.from(context);
//		View layout = inflater.inflate(R.layout.error_layout, (ViewGroup) root.findViewById(R.id.error_layout_root));
//
//		TextView text = (TextView) layout.findViewById(R.id.error_layout_text);
//		text.setText(message);
//
//		Toast toast = new Toast(context);
//		toast.setGravity(Gravity.CENTER, 0, 0);
//		toast.setDuration(Toast.LENGTH_SHORT);
//		toast.setView(layout);
//		toast.show();
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}
}
