package com.bignerdranch.franklin.roger;

import android.app.Activity;
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

import com.bignerdranch.franklin.roger.model.RogerParams;

public class LayoutEditDialogFragment extends DialogFragment {
	private static final String TAG = "LayoutEditDialogFragment";
	
	private static final String ARG_PARAM_TYPE = "LayoutEditDialogFragment.paramType";
	private static final String ARG_ROGER_PARAMS = "LayoutEditDialogFragment.rogerParams";
	
	private TextView titleText;
	private RadioGroup radioGroup;
	private RadioButton wrapButton;
	private RadioButton fillButton;
	private RadioButton pixelButton;
	private EditText pixelText;
	
	private ParamType type;
	private RogerParams params;
	
	public enum ParamType {
		WIDTH, HEIGHT
	}
	
	public static LayoutEditDialogFragment newInstance(ParamType type, RogerParams params) {
		
		LayoutEditDialogFragment fragment = new LayoutEditDialogFragment();
		
		Bundle args = new Bundle();
		args.putSerializable(ARG_PARAM_TYPE, type);
		args.putSerializable(ARG_ROGER_PARAMS, params);
		
		fragment.setArguments(args);
		
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Dialog);
		
		Bundle args = getArguments();
		type = (ParamType) args.getSerializable(ARG_PARAM_TYPE);
		params = (RogerParams) args.getSerializable(ARG_ROGER_PARAMS);
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
	
	private boolean updateParam(int value) {
		if (type == ParamType.HEIGHT && value != params.getHeightParam()) {
			params.setHeightParam(value);
		} else if (type == ParamType.WIDTH && value != params.getWidthParam()) {
			params.setWidthParam(value);
		} else {
            return false;
        }

        return true;
	}
	
	private int getLayoutValue() {
		int value = 0;
		
		if (type == ParamType.HEIGHT) {
			value = params.getHeightParam();
		} else if (type == ParamType.WIDTH) {
			value = params.getWidthParam();
		}
		
		return value;
	}
	
	private int getLayoutPixelValue() {
		int value = 0;
		
		if (type == ParamType.HEIGHT) {
			value = params.getPixelHeight();
		} else if (type == ParamType.WIDTH) {
			value = params.getPixelWidth();
		}
		
		return value;
	}
	
	private void updateView() {
		
		String title = "";
		if (type == ParamType.HEIGHT) {
			title = "Height";
		} else if (type == ParamType.WIDTH) {
			title = "Width";
		}
		titleText.setText(title);
		
		int layoutValue = getLayoutValue();
		if (layoutValue == ViewGroup.LayoutParams.FILL_PARENT) {
			radioGroup.check(fillButton.getId());
		} else if (layoutValue == ViewGroup.LayoutParams.WRAP_CONTENT) {
			radioGroup.check(wrapButton.getId());
		} else {
			radioGroup.check(pixelButton.getId());
			pixelText.setText(getLayoutPixelValue() + "");
		}
	}
	
	private void parsePixelValue() {
		try {
			String text = pixelText.getText().toString();
			int value = Integer.parseInt(text);
			if (updateParam(value)) {
                valueChanged();
            }
		} catch (NumberFormatException e) {
			Log.e(TAG, "Unable to parse number ", e);
		}
	}
	
	private void valueChanged() {
		Activity activity = getActivity();
		if (activity instanceof RogerActivity) {
			((RogerActivity) activity).setRogerParams(params);
		}
	}
	
	private OnCheckedChangeListener checkedChagned = new OnCheckedChangeListener() {
		
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			
            boolean valueChanged = false;
			if (checkedId == fillButton.getId()) {
				valueChanged |= updateParam(ViewGroup.LayoutParams.FILL_PARENT);
			} else if (checkedId == wrapButton.getId()) {
				valueChanged |= updateParam(ViewGroup.LayoutParams.WRAP_CONTENT);
			} else {
				parsePixelValue();
			}
			
            if (valueChanged) {
                valueChanged();
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
