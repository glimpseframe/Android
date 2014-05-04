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
			actionBar.hide();
			listener.onClick(v);
		}
	}

	private ActionBar actionBar;
	private View customActionBar;
	private boolean isFreeze = false;
	private OnActionClick listener;
	
	public BlurActionBar(Activity activity) {
		this.actionBar = activity.getActionBar();
		customActionBar = activity.getLayoutInflater().inflate(R.layout.custom_bar, null);
		actionBar.hide();
		actionBar.setCustomView(customActionBar);
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		createActionButtons();
	}
	
	private void createActionButtons() {
		View deleteActionView = customActionBar.findViewById(R.id.action_delete);
		View freezeActionView = customActionBar.findViewById(R.id.action_freeze);
		View resetActionView = customActionBar.findViewById(R.id.action_reset_wifi);
		View emailActionView = customActionBar.findViewById(R.id.action_new_email);
		
		ActionClickListener simpleClickListener = new ActionClickListener();
		deleteActionView.setOnClickListener(simpleClickListener);
		resetActionView.setOnClickListener(simpleClickListener);
		emailActionView.setOnClickListener(simpleClickListener);
		freezeActionView.setOnClickListener(new ActionClickListener() {
			@Override
			public void onClick(View v) {
				chageFreezeFrame(!isFreeze, v);
				super.onClick(v);
			}
		});
	}
	
	public void setOnActionClickListener(OnActionClick listener) {
		this.listener = listener;
	}
	
	private void chageFreezeFrame(boolean isFreeze, View view) {
		this.isFreeze = isFreeze;
		TextView action = (TextView) view.findViewById(R.id.action_freeze);
		action.setText(isFreeze ? R.string.action_unfreeze : R.string.action_freeze);
		action.setCompoundDrawablesWithIntrinsicBounds((isFreeze ? android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause), 0,0,0);
	}
	
	public boolean isFreeze() {
		return isFreeze;
	}

	public void unFreeze() {
		chageFreezeFrame(false, customActionBar);
	}

}