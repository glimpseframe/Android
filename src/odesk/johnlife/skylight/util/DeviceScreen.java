package odesk.johnlife.skylight.util;

import android.content.Context;
import android.util.DisplayMetrics;

public class DeviceScreen {

	private int width;
	private int height;
	private int widthDpScaled;
	private int heightDpScaled;
	private float widthIn;
	private float heightIn;
	private int widthDp; 
	private int heightDp;
	private int densityDpi;

	public DeviceScreen(Context context) {
		DisplayMetrics m = context.getResources().getDisplayMetrics();
		width = m.widthPixels;
		height = m.heightPixels;
		float density = m.density;
		widthDp = (int)Math.ceil(((float)width)/density);
		heightDp = (int)Math.ceil(((float)height)/density);
		density = m.scaledDensity;
		widthDpScaled = (int)Math.ceil(((float)width)/density);
		heightDpScaled = (int)Math.ceil(((float)height)/density);
		widthIn = width / m.xdpi;
		heightIn = height / m.ydpi;
		densityDpi = m.densityDpi;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getSmallestWidth() {
		return Math.min(width, height);
	}
	
	public int getLargestWidth() {
		return Math.max(width, height);
	}

	public int getDensityDpi() { return densityDpi; }

	public int getWidthDpScaled() {
		return widthDpScaled;
	}

	public int getHeightDpScaled() {
		return heightDpScaled;
	}

	public int getWidthDp() {
		return widthDp;
	}

	public int getHeightDp() {
		return heightDp;
	}

	public float getWidthIn() {
		return widthIn;
	}

	public float getHeightIn() {
		return heightIn;
	}
	
	
}
