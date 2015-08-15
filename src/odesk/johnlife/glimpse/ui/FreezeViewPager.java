package odesk.johnlife.glimpse.ui;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class FreezeViewPager extends ViewPager {
	public static interface SwipeValidator {
		public boolean isSwipeBlocked();
	}
	
	private SwipeValidator validator; 

	public FreezeViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public FreezeViewPager(Context context) {
		super(context);
	}

	public void setSwipeValidator(SwipeValidator validator) {
		this.validator = validator;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (validator != null && validator.isSwipeBlocked()) {
			return true;
		}
		return super.onTouchEvent(ev);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (validator != null && validator.isSwipeBlocked()) {
			return false;
		}
		return super.onInterceptTouchEvent(ev);
	}

}
