package odesk.johnlife.glimpse.ui;

import odesk.johnlife.glimpse.R;
import android.app.ActionBar;
import android.app.Activity;
import android.view.View;
import android.widget.TextView;

public class BlurActionBar {
	public static interface OnActionClick {
		void onClick(View v);
	}
	
	private class ActionClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (v.getId() != R.id.action_setting)
				actionBar.hide();
			listener.onClick(v);
		}
	}

	private ActionBar actionBar;
	private View customActionBar;
	private boolean isFreeze = false;
	private OnActionClick listener;
	
	public BlurActionBar(Activity activity, boolean isFreeze, boolean isBlocked) {
		this.actionBar = activity.getActionBar();
		customActionBar = activity.getLayoutInflater().inflate(R.layout.custom_bar, null);
		actionBar.setCustomView(customActionBar);
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		this.isFreeze = isFreeze;
		createActionButtons(isBlocked);
	}
	
	private void createActionButtons(boolean isBlocked) {
		View deleteActionView = customActionBar.findViewById(R.id.action_delete);
		View freezeActionView = customActionBar.findViewById(R.id.action_freeze);
		View settingActionView = customActionBar.findViewById(R.id.action_setting);
		View galleryActionView = customActionBar.findViewById(R.id.action_gallery);
		if (isBlocked) {
			deleteActionView.setEnabled(false);
			freezeActionView.setEnabled(false);
			galleryActionView.setEnabled(false);
		}
		chageFreezeState(isFreeze, freezeActionView);
		ActionClickListener simpleClickListener = new ActionClickListener();
		deleteActionView.setOnClickListener(simpleClickListener);
		settingActionView.setOnClickListener(simpleClickListener);
		galleryActionView.setOnClickListener(simpleClickListener);
		freezeActionView.setOnClickListener(new ActionClickListener() {
			@Override
			public void onClick(View v) {
				chageFreezeState(!isFreeze, v);
				super.onClick(v);
			}
		});
	}
	
	public void setOnActionClickListener(OnActionClick listener) {
		this.listener = listener;
	}
	
	private void chageFreezeState(boolean isFreeze, View view) {
		this.isFreeze = isFreeze;
		TextView action = (TextView) view.findViewById(R.id.action_freeze);
		action.setText(isFreeze ? R.string.resume : R.string.pause);
		action.setCompoundDrawablesWithIntrinsicBounds((isFreeze ? android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause), 0,0,0);
	}
	
	public boolean isFreeze() {
		return isFreeze;
	}

	public void unFreeze() {
		chageFreezeState(false, customActionBar);
	}
	
	public void freeze() {
		chageFreezeState(true, customActionBar);
	}

}
