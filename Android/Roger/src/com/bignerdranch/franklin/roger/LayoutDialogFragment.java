package com.bignerdranch.franklin.roger;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;

import com.bignerdranch.franklin.roger.model.RogerParams;

public class LayoutDialogFragment extends DialogFragment {
	private static final String PARAM_PARAMS = "LayoutDialogFragment.RogerParam";
	private static final String TAG_EDIT_DIALOG = "LayoutDialogFragment.EditDialog";
	
	private boolean lastValueIsHeight;
	private RogerParams params;
	
	private Button widthButton;
	private Button heightButton;
	
	public static LayoutDialogFragment newInstance(RogerParams params) {
		LayoutDialogFragment fragment = new LayoutDialogFragment();
		Bundle args = new Bundle();
		args.putSerializable(PARAM_PARAMS, params);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Dialog);
		Bundle args = getArguments();
		params = (RogerParams) args.getSerializable(PARAM_PARAMS);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.layout_dialog_fragment, null);
		
		widthButton = (Button) view.findViewById(R.id.layout_width_button);
		widthButton.setOnClickListener(widthButtonClick);
		
		heightButton = (Button) view.findViewById(R.id.layout_height_button);
		heightButton.setOnClickListener(heightButtonClick);
		
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		updateButtons();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		Activity activity = getActivity();
		if (activity instanceof RogerActivity) {
			((RogerActivity) activity).setRogerParams(params);
		}
	}
	
	private void updateValue(int value) {
		if (lastValueIsHeight) {
			params.setHeightParam(value);
		} else {
			params.setWidthParam(value);
		}
	}
	
	private void showEditDialog(String title, int layoutValue) {
		LayoutEditDialogFragment fragment = LayoutEditDialogFragment.newInstance(title, layoutValue);
		
		// FIXME: I think there is a memory leak here
		fragment.setOnValueChangeListener(editListener);
		fragment.show(getFragmentManager(), TAG_EDIT_DIALOG);
	}
	
	private void updateButtons() {
		widthButton.setText(paramsToString(params.getWidthParam()));
		heightButton.setText(paramsToString(params.getHeightParam()));
	}
	
	private String paramsToString(int value) {
		if (value == LayoutParams.FILL_PARENT) {
			return "Fill Parent";
		} else if (value == LayoutParams.WRAP_CONTENT) {
			return "Wrap Content";
		}
		
		else return value + " dip";
	}
	
	private LayoutEditDialogFragment.ValueChangeListener editListener = new LayoutEditDialogFragment.ValueChangeListener() {

		@Override
		public void onValueChanged(int value) {
			updateValue(value);
			updateButtons();
		}
		
	};
	
	private View.OnClickListener widthButtonClick = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			lastValueIsHeight = false;
			showEditDialog("Width", params.getPixelWidth());
		}
	};
	
	
	private View.OnClickListener heightButtonClick = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			lastValueIsHeight = true;
			showEditDialog("Height", params.getPixelHeight());
		}
	};
}
