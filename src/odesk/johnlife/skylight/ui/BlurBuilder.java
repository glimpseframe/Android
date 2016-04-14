package odesk.johnlife.skylight.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.view.View;

public class BlurBuilder {
	private static final float BLUR_RADIUS = 10f;//3.5f;

	public static Bitmap blur(View v) {
		return blur(v.getContext(), getScreenshot(v));
	}

	public static Bitmap blur(Context ctx, Bitmap image) {
		int width = (image.getWidth()) >> 2 << 1;
		int height = (image.getHeight() >> 2) << 1;

		Bitmap inputBitmap = Bitmap.createScaledBitmap(image, width, height, true);
		Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

		RenderScript rs = RenderScript.create(ctx);
		ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
		Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
		Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);
		theIntrinsic.setRadius(BLUR_RADIUS);
		theIntrinsic.setInput(tmpIn);
		theIntrinsic.forEach(tmpOut);
		tmpOut.copyTo(outputBitmap);

		return outputBitmap;
	}

	private static Bitmap getScreenshot(View v) {
		Bitmap b = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(b);
		v.draw(c);
		return b;
	}
}