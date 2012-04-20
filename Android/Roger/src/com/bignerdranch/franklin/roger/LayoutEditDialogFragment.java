package com.bignerdranch.franklin.roger;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

public class LayoutEditDialogFragment extends DialogFragment {
	private static final String TAG = "LayoutEditDialogFragment";
	
	private static final String ARG_TITLE = "LayoutEditDialogFragment.title";
	private static final String ARG_LAYOUT_VALUE = "LayoutEditDialogFragment.LayoutValue";
	
	private TextView titleText;
	private RadioGroup radioGroup;
	private RadioButton wrapButton;
	private RadioButton fillButton;
	private RadioButton pixelButton;
	private EditText pixelText;
	
	private String title;
	private int layoutValue;
	
	private ValueChangeListener listener;
	public interface ValueChangeListener {
		public void onValueChanged(int value);
	}
	
	public static LayoutEditDialogFragment newInstance(String title, int layoutValue) {
		
		LayoutEditDialogFragment fragment = new LayoutEditDialogFragment();
		
		Bundle args = new Bundle();
		args.putString(ARG_TITLE, title);
		args.putInt(ARG_LAYOUT_VALUE, layoutValue);
		fragment.setArguments(args);
		
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Dialog);
		
		Bundle args = getArguments();
		layoutValue = args.getInt(ARG_LAYOUT_VALUE);
		title = args.getString(ARG_TITLE);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.layout_edit_dialog_fragment, null);

		titleText = (TextView) view.findViewById(R.id.layout_edit_title_text);
		radioGroup = (RadioGroup) view.findViewById(R.id.layout_edit_radio_group);
		radioGroup.setOnCheckedChangeListener(checkedChagned);
		
		wrapButton = (RadioButton) view.findViewById(R.id.layout_edit_wrap_content);
		fillButton = (RadioButton) view.findViewById(R.id.layout_edit_fill_parent);
		pixelButton = (RadioButton) view.findViewById(R.id.layout_edit_pixels);
		
		pixelText = (EditText) view.findViewById(R.id.layout_edit_pixels_text);
		pixelText.setOnTouchListener(pixelValueClick);
		pixelText.addTextChangedListener(textWatcher);
		
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		updateView();
	}
	
	public void setOnValueChangeListener(ValueChangeListener listener) {
		this.listener = listener;
	}
	
	private void updateView() {
		titleText.setText(title);
		
		if (layoutValue == ViewGroup.LayoutParams.FILL_PARENT) {
			radioGroup.check(fillButton.getId());
		} else if (layoutValue == ViewGroup.LayoutParams.WRAP_CONTENT) {
			radioGroup.check(wrapButton.getId());
		} else {
			radioGroup.check(pixelButton.getId());
			pixelText.setText(layoutValue + "");
		}
	}
	
	private void parsePixelValue() {
		try {
			String text = pixelText.getText().toString();
			int value = Integer.parseInt(text);
			layoutValue = value;			
			
			if (listener != null) {
				listener.onValueChanged(layoutValue);
			}
		} catch (NumberFormatException e) {
			Log.e(TAG, "Unable to parse number ", e);
		}
	}
	
	private OnCheckedChangeListener checkedChagned = new OnCheckedChangeListener() {
		
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			
			if (checkedId == fillButton.getId()) {
				layoutValue = ViewGroup.LayoutParams.FILL_PARENT;
			} else if (checkedId == wrapButton.getId()) {
				layoutValue = ViewGroup.LayoutParams.WRAP_CONTENT;
			} else {
				parsePixelValue();
			}
			
			if (listener != null) {
				listener.onValueChanged(layoutValue);
			}
		}
	};
	
	private TextWatcher textWatcher = new TextWatcher() {
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			parsePixelValue();
		}
		
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
		
		@Override
		public void afterTextChanged(Editable s) { }
	};
	
	private View.OnTouchListener pixelValueClick = new View.OnTouchListener() {
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			radioGroup.check(pixelButton.getId());
			return false;
		}
	};

}
