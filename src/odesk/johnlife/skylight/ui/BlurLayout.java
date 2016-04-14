package odesk.johnlife.skylight.ui;

import odesk.johnlife.skylight.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class BlurLayout extends FrameLayout {
	private boolean renderingBlur = false;
	private Bitmap bmp;
	private Rect dst;
	private Rect src;

	public BlurLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setBackgroundColor(Color.argb(1, 0, 0, 0));
	}

	public BlurLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		setBackgroundColor(Color.argb(1, 0, 0, 0));
	}

	public BlurLayout(Context context) {
		super(context);
		setBackgroundColor(Color.argb(1, 0, 0, 0));
	}

	@Override
	public void onDraw(Canvas canvas) {
		if (isInEditMode()) {
			super.onDraw(canvas);
			return;
		}
		if (null == bmp) {
			renderBlur();
		}
		canvas.drawBitmap(bmp, src, dst, null);
		super.onDraw(canvas);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return true;
	}
	
	private void renderBlur() {
		renderingBlur = true;
		View parentView = null;
		if (getId() == R.id.custom_popup_menu) {
			parentView = (View)getParent();
		} else if (getId() == R.id.custom_bar) {
			parentView = (View)getRootView().findViewById(R.id.pager).getParent();
		} else {
			parentView = (View) getParent().getParent();
		}
		bmp = Bitmap.createBitmap(parentView.getWidth(), parentView.getHeight(), Config.ARGB_8888);
		Canvas cnv = new Canvas(bmp);
		parentView.draw(cnv);
		renderingBlur = false;
		bmp = BlurBuilder.blur(getContext(), bmp);
		dst = new Rect(0, 0, getWidth(), getHeight());
		src = new Rect(dst);
		src.offset(getLeft(), getTop());
		src.set(src.left/2, src.top/2, src.right/2, src.bottom/2);
	}

	@Override
	public void draw(Canvas canvas) {
		if (renderingBlur) return;
		super.draw(canvas);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		bmp = null;
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		bmp = null;
	}

	
}
