package com.bignerdranch.franklin.roger.model;

import java.io.Serializable;

import android.view.ViewGroup.LayoutParams;

public class RogerParams implements Serializable{
	private static final long serialVersionUID = 1L;
	
	private int widthParam;
	private int heightParam;
	private float displayDensity;
	
	public RogerParams(float displayDensity, LayoutParams params) {
		this(displayDensity, params.width, params.height);
	}
	
	public RogerParams(float displayDensity, int widthParams, int heightParams) {
		this.displayDensity = displayDensity;
		this.widthParam = widthParams;
		this.heightParam = heightParams;
	}

	public int getWidthParam() {
		if (widthParam == LayoutParams.WRAP_CONTENT ||
				widthParam == LayoutParams.FILL_PARENT) {
			return widthParam;
		}

		return (int) convertPxToDp(widthParam);
	}

	public void setWidthParam(int widthParam) {
		this.widthParam = widthParam;
	}

	public int getHeightParam() {
		if (heightParam == LayoutParams.WRAP_CONTENT ||
				heightParam == LayoutParams.FILL_PARENT) {
			return heightParam;
		}
		
		return (int) convertPxToDp(heightParam);
	}

	public void setHeightParam(int heightParam) {
		this.heightParam = heightParam;
	}

	private float convertPxToDp(int dimen) {
		return dimen / displayDensity;
	}
	
	public int getPixelHeight() {
		return heightParam;
	}
	
	public int getPixelWidth() {
		return widthParam;
	}
}
