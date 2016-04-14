package odesk.johnlife.skylight.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;

import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import odesk.johnlife.skylight.Constants;
import odesk.johnlife.skylight.R;
import odesk.johnlife.skylight.adapter.ImagePagerAdapter;
import odesk.johnlife.skylight.adapter.ImagesGalleryAdapter;
import odesk.johnlife.skylight.app.SkylightApp;
import odesk.johnlife.skylight.data.DatabaseHelper;
import odesk.johnlife.skylight.data.FileHandler;
import odesk.johnlife.skylight.data.PictureData;
import odesk.johnlife.skylight.dialog.BlurDialog;
import odesk.johnlife.skylight.dialog.DeletingDialog;
import odesk.johnlife.skylight.dialog.EmailChangeDialog;
import odesk.johnlife.skylight.dialog.HelpDialog;
import odesk.johnlife.skylight.dialog.RecognizeDialog;
import odesk.johnlife.skylight.dialog.WifiDialog;
import odesk.johnlife.skylight.task.ResetCheckTask;
import odesk.johnlife.skylight.ui.BlurActionBar;
import odesk.johnlife.skylight.ui.BlurActionBar.OnActionClick;
import odesk.johnlife.skylight.ui.BlurListView;
import odesk.johnlife.skylight.ui.BlurTextView;
import odesk.johnlife.skylight.ui.FreezeViewPager;
import odesk.johnlife.skylight.ui.HintTextView;
import odesk.johnlife.skylight.util.MailConnector;
import odesk.johnlife.skylight.util.MailConnector.OnItemDownloadListener;
import odesk.johnlife.skylight.util.WifiConnectionListener;
import odesk.johnlife.skylight.util.WifiReceiver;
import ru.johnlife.lifetools.reporter.UpmobileExceptionReporter;

public class PhotoActivity extends Activity implements Constants, WifiConnectionListener, RecognizeDialog.OnCodeAssociatingListener {

	private Runnable swipeRunnable = new Runnable() {
		@Override
		public void run() {
			if (SkylightApp.getFileHandler().size() < SCREEN_PAGE_LIMIT) return;
			boolean blocked = actionBar.isFreeze() || getActionBar().isShowing() || isBlocked(true);
			if (blocked || SkylightApp.getFileHandler().isLocked()) {
				pager.postDelayed(swipeRunnable, 50);
			} else {
				int idx = pager.getCurrentItem() + 1;
				if (idx == SkylightApp.getFileHandler().size()) {
					idx = 0;
				}
				pager.setCurrentItem(idx);
				rescheduleImageSwipe();
			}
		}
	};

	private TimerTask mailPollTask = new TimerTask() {
		@Override
		public void run() {
			String user = getUser();
			if (wifi.isConnected() && null != user) {
				String pass = "HPgqL2658P";
				MailConnector mailer = new MailConnector(user, pass, new OnItemDownloadListener() {
					@Override
					public void onItemDownload() {
						pagerAdapter.setHasNewPhotos(true);
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								if (gallery.getVisibility() == View.VISIBLE) {
									galleryAdapter.notifyDataSetChanged();
								}
								error.hide();
								showSeeNewPhoto();
								rescheduleImageSwipe();
							}
						});
					}
				});
				mailer.connect();
				if (!SkylightApp.getFileHandler().isEmpty()) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							error.hide();
						}
					});
				}
			}
		}
	};

	private BlurActionBar actionBar;
	private ViewPager pager;
	private ProgressBar progress;
	private Button seeNewPhoto;
	private BlurTextView error;
	private HintTextView hint;
	private BlurListView wifiList;
	private GridView gallery;
	private DeletingDialog deletingDialog;
	private RecognizeDialog recognizeDialog;
	private EmailChangeDialog emailChangeDialog;
	private HelpDialog helpDialog;
	private WifiDialog wifiDialog;
	private DatabaseHelper databaseHelper;
	private WifiReceiver wifi;
	private ImagePagerAdapter pagerAdapter;
	private ImagesGalleryAdapter galleryAdapter;
	private Timer mailTimer = new Timer();
	private boolean isFreeze = false;
	private boolean isNewPhotosHidden;
	private List<BlurDialog> dialogs = new ArrayList<>();
	private UpmobileExceptionReporter logger;
	private SharedPreferences prefs;
	private Timer resetTimer;

	public void addDialogToList(BlurDialog dialog) {
		if (dialog != null) {
			dialogs.add(dialog);
		}
	}

	@Override
	public void onBackPressed() {
		for (BlurDialog dialog : dialogs) {
			if (dialog.cancel()) return;
		}
		if (null != gallery && gallery.getVisibility() == View.VISIBLE) {
			closeGallery();
			rescheduleImageSwipe();
			return;
		}
		super.onBackPressed();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Crashlytics.start(this);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		setContentView(R.layout.activity_photo);
		logger = UpmobileExceptionReporter.getInstance(this);
		init();
		getActionBar().hide();
		if (!(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && SkylightApp.getPicturesDir().canWrite())) {
			showProgress();
			IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
			filter.addDataScheme("file");
			registerReceiver(new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					restart();
				}
			}, filter);
		} else if (getUser() == null && wifi.isConnected()) {
			recognizeDialog.show();
		}
		wifi.register();
		resetTimer = new Timer();
		resetTimer.scheduleAtFixedRate(new ResetCheckTask(this), 15000, 30000);
	}

	private void init() {
		initViews();
		wifi = WifiReceiver.createInstance(this, this);
		createActionBar();
		mailTimer.scheduleAtFixedRate(mailPollTask, 0, REFRESH_RATE);
		databaseHelper = DatabaseHelper.getInstance(getApplicationContext());
		seeNewPhoto.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				hideSeeNewPhoto();
				actionBar.unFreeze();
				getActionBar().hide();
				FileHandler handler = SkylightApp.getFileHandler();
				handler.setPause(false);
				pagerAdapter.setHasNewPhotos(false);
				handler.resetCurrentPicture();
				rescheduleImageSwipe();
			}
		});
		gallery.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				closeGallery();
				SkylightApp.getFileHandler().rewind((PictureData) gallery.getItemAtPosition(position));
				rescheduleImageSwipe();
			}
		});
		helpDialog.setOnCloseListener(new DeletingDialog.OnCloseListener() {
			@Override
			public void onClose() {
				if (isNewPhotosHidden) showSeeNewPhoto();
			}
		});
		deletingDialog.setOnCloseListener(new DeletingDialog.OnCloseListener() {
			@Override
			public void onClose() {
				if (isNewPhotosHidden) showSeeNewPhoto();
			}
		});
		deletingDialog.setPositiveButtonListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				pagerAdapter.deleteCurrentItem(pager);
				deletingDialog.cancel();
				rescheduleImageSwipe();
				checkForNoPhotos();
			}
		});
		createPager();
	}

	@Override
	protected void onResume() {
		super.onResume();
		wifi.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		wifi.onPause();
	}

	private void initViews() {
		progress = (ProgressBar) findViewById(R.id.progressLoading);
		error = (BlurTextView) findViewById(R.id.error_pane);
		hint = (HintTextView) findViewById(R.id.hint);
		seeNewPhoto = (Button) findViewById(R.id.see_new_photos);
		wifiList = (BlurListView) findViewById(R.id.wifi_list);
		gallery = (GridView) findViewById(R.id.gallery1);
		recognizeDialog = (RecognizeDialog) findViewById(R.id.dialog_recognize);
		deletingDialog = (DeletingDialog) findViewById(R.id.dialog_deleting);
		emailChangeDialog = (EmailChangeDialog) findViewById(R.id.dialog_change_email);
		helpDialog = (HelpDialog) findViewById(R.id.dialog_help);
		wifiDialog = (WifiDialog) findViewById(R.id.dialog_wifi);
	}

	private void createPager() {
		pager = (ViewPager) findViewById(R.id.pager);
		pagerAdapter = new ImagePagerAdapter(this, databaseHelper, createOnClickListener());
		pager.setAdapter(pagerAdapter);
		pager.setOffscreenPageLimit(SCREEN_PAGE_LIMIT);
		pager.setCurrentItem(Integer.MAX_VALUE / 2);
		pager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				rescheduleImageSwipe();
				pagerAdapter.setImageShown(position);
				if (actionBar.isFreeze()) {
					actionBar.unFreeze();
				}
				if (pagerAdapter.hasNewPhotos()) {
					showSeeNewPhoto();
				}
			}

			@Override
			public void onPageScrollStateChanged(int state) {
				if (getActionBar() != null && getActionBar().isShowing() &&
						(state == ViewPager.SCROLL_STATE_SETTLING || state == ViewPager.SCROLL_STATE_DRAGGING)) {
					getActionBar().hide();
				}
			}
		});
		((FreezeViewPager)pager).setSwipeValidator(new FreezeViewPager.SwipeValidator() {
			@Override
			public boolean isSwipeBlocked() {
				return SkylightApp.getFileHandler().size() < SCREEN_PAGE_LIMIT || isBlocked(true);
			}
		});
		pagerAdapter.checkNewPhotos();
		if (pagerAdapter.hasNewPhotos() && wifi.isConnected()) showSeeNewPhoto();
		if (SkylightApp.getFileHandler().size() >= SCREEN_PAGE_LIMIT) rescheduleImageSwipe();
	}

	private void checkForNoPhotos() {
		if (SkylightApp.getFileHandler().isEmpty() && getUser() != null) {
			error.show(getString(R.string.error_no_photo, getUser()));
		}
	}

	private void showSeeNewPhoto() {
		if (gallery != null && gallery.getVisibility() == View.VISIBLE ||
				helpDialog.getVisibility() == View.VISIBLE ||
				deletingDialog.getVisibility() == View.VISIBLE) {
			isNewPhotosHidden = true;
			return;
		}
		isNewPhotosHidden = false;
		seeNewPhoto.setVisibility(View.VISIBLE);
	}

	private void hideSeeNewPhoto() {
		hideSeeNewPhoto(false);
	}

	@Override
	protected void onDestroy() {
		wifi.unregister();
		mailTimer.cancel();
		resetTimer.cancel();
		super.onDestroy();
	}

	private OnClickListener createOnClickListener() {
		return new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isBlocked(false)) return;
				createActionBar();
				ActionBar actionBar = getActionBar();
				if (actionBar.isShowing()) {
					actionBar.hide();
				} else {
					actionBar.show();
				}
			}
		};
	}

	private boolean isBlocked(boolean checkWithError) {
		if (!wifi.isConnectedOrConnecting()) return true;
		if (checkWithError) if (error.getVisibility() == View.VISIBLE) return true;
		View[] swipeBlockers = {
				wifiList,
				/** uncomment if newEmail is needing*/
//			newEmail.dialog,
				helpDialog,
				recognizeDialog,
				deletingDialog,
				wifiDialog,
				progress
		};
		for (View blocker : swipeBlockers) {
			if (blocker.getVisibility() == View.VISIBLE) return true;
		}
		return false;
	}

	private void restart() {
		Activity activity = PhotoActivity.this;
		Intent intent = new Intent(activity, activity.getClass());
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		activity.finish();
		activity.startActivity(intent);
	}

	private void createActionBar() {
		actionBar = new BlurActionBar(this, actionBar == null ? isFreeze : actionBar.isFreeze(), isBlocked(true));
		actionBar.setOnActionClickListener(new OnActionClick() {
			@Override
			public void onClick(View v) {
				switch (v.getId()) {
					case R.id.action_delete:
						deletingDialog.show();
						hideSeeNewPhoto(true);
						break;
					case R.id.action_setting:
						showPopupMenu(v);
						break;
					case R.id.action_freeze:
						FileHandler handler = SkylightApp.getFileHandler();
						if (actionBar.isFreeze()) {
							hideSeeNewPhoto(true);
							handler.setPause(true);
						} else {
							handler.setPause(false);
							if (isNewPhotosHidden) showSeeNewPhoto();
						}
						break;
					case R.id.action_gallery:
						if (View.GONE == gallery.getVisibility()) {
							gallery.setPadding(gallery.getPaddingLeft(), getActionBar().getHeight(),
									gallery.getPaddingRight(), gallery.getPaddingBottom());
							gallery.setVerticalScrollBarEnabled(false);
							gallery.setHorizontalScrollBarEnabled(false);
							galleryAdapter = new ImagesGalleryAdapter(PhotoActivity.this);
							gallery.setAdapter(galleryAdapter);
							gallery.setVisibility(View.VISIBLE);
							pager.setVisibility(View.GONE);
							hideSeeNewPhoto(true);
							actionBar.setGalleryState(true);
							getActionBar().show();
						} else {
							closeGallery();
							rescheduleImageSwipe();
						}
						break;
				}
			}
		});
	}

	private void hideSeeNewPhoto(boolean withCheck) {
		if (withCheck && !isNewPhotosHidden) isNewPhotosHidden = seeNewPhoto.getVisibility() == View.VISIBLE;
		seeNewPhoto.setVisibility(View.GONE);
	}

	private void closeGallery() {
		pager.setVisibility(View.VISIBLE);
		gallery.setVisibility(View.GONE);
		actionBar.setGalleryState(false);
		if (isNewPhotosHidden) showSeeNewPhoto();
		getActionBar().hide();
	}

	private void showPopupMenu(View view) {
		try {
			LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View layout = layoutInflater.inflate(R.layout.popup_menu, null);
			final PopupWindow popupWindow = new PopupWindow(this);
			popupWindow.setContentView(layout);
			popupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
			popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
			popupWindow.setOutsideTouchable(true);
			popupWindow.setTouchable(true);
			popupWindow.setFocusable(true);
			/**
			 * uncomment if the item "change email" is needed */
//			layout.findViewById(R.id.change_email).setOnClickListener(new OnClickListener() {
//				@Override
//				public void onClick(View v) {
//					popupWindow.dismiss();
//					getActionBar().hide();
//					emailChangeDialog.show();
//				}
//			});
			layout.findViewById(R.id.reset_wifi).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					popupWindow.dismiss();
					getActionBar().hide();
					wifi.resetCurrentWifi();
				}
			});
			layout.findViewById(R.id.how_it_works).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (null != gallery && gallery.getVisibility() == View.VISIBLE) {
						closeGallery();
						rescheduleImageSwipe();
					}
					hideSeeNewPhoto(true);
					popupWindow.dismiss();
					getActionBar().hide();
					helpDialog.show();
				}
			});
			popupWindow.showAsDropDown(view, 5, 5);
		} catch (Exception e) {
			Log.e("Show popup menu", e.getMessage(), e);
		}
//		PopupMenu popupMenu = new PopupMenu(this, v);
//		popupMenu.inflate(R.menu.popup_menu);
//		popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
//			@Override
//			public boolean onMenuItemClick(MenuItem item) {
//				newEmail.hide();
//				switch (item.getItemId()) {
//				case R.id.menu1:
//					newEmail.show();
//					return true;
//				case R.id.menu2:
//					if (isConnected()) {
//						new WifiConnector(context).forgetCurrent();
//						hideErrorPane();
//						progress.setVisibility(View.VISIBLE);
//						registerReceiver(wifiConnectionHandler.getReceiver(), new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
//					}
//					return true;
//				case R.id.menu3:
//					getActionBar().hide();
//					howItWorks.show();
//					return true;
//				default:
//					return false;
//				}
//			}
//		});
//		popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
//			@Override
//			public void onDismiss(PopupMenu menu) {
//
//			}
//		});
//		popupMenu.show();
	}

	public String getUser() {
		return prefs.getString(PREF_USER_EMAIL, null);
	}

	@Override
	public void onCodeAssociated(String email) {
		prefs.edit().putString(PREF_USER_EMAIL, email).commit();
		helpDialog.setEmail(email);
		checkForNoPhotos();
		if (pagerAdapter.hasNewPhotos()) {
			showSeeNewPhoto();
		}
	}

	@Override
	public void onCodeRefused(String error) {
		hint.show(error, ERROR_RECOGNIZE_TIME);
	}

	private void rescheduleImageSwipe() {
		pager.removeCallbacks(swipeRunnable);
		pager.postDelayed(swipeRunnable, RESCHEDULE_REFRESH_RATE);
	}

	private void showProgress() {
		progress.setVisibility(View.VISIBLE);
	}

	private void hideProgress() {
		progress.setVisibility(View.GONE);
	}

	private void hideViews() {
		hideProgress();
		getActionBar().hide();
		wifiList.hide(false);
		wifiDialog.hide();
		deletingDialog.hide();
		helpDialog.hide();
		recognizeDialog.hide();
		closeGallery();
		hideSeeNewPhoto();
		error.hide();
	}

	public void showHint(String text) {
		hint.show(text);
	}

	@Override
	public void onConnecting() {
		showProgress();
		wifiList.hide(false);
		wifiDialog.hide();
	}

	@Override
	public void onConnected() {
		hideProgress();
		wifiList.hide(false);
		wifiDialog.hide();
		hint.show(R.string.hint_wifi_connected);
		if (getUser() == null) {
			recognizeDialog.show();
		} else {
			checkForNoPhotos();
			if (pagerAdapter.hasNewPhotos()) {
				showSeeNewPhoto();
			}
		}
	}

	@Override
	public void onDisconnected(WifiReceiver.WifiError wifiError) {
		hideViews();
		if (WifiReceiver.WifiError.NEED_PASSWORD.equals(wifiError)) {
			wifiDialog.show();
		} else {
			wifiList.show();
			wifi.scanWifi();
			if (WifiReceiver.WifiError.CONNECT_ERROR.equals(wifiError)) {
				hint.show(R.string.hint_failed_to_connect);
			} else if (WifiReceiver.WifiError.DISCONNECTED.equals(wifiError)) {
				hint.show(R.string.hint_wifi_disconnected);
			} else if (WifiReceiver.WifiError.UNKNOWN_ERROR.equals(wifiError)) {
				hint.show(R.string.hint_wifi_error);
			}
		}
	}

	@Override
	public void onScanning() {
		if (wifi == null || wifi.isConnected() || (wifiList.getVisibility() == View.VISIBLE && !wifiList.isEmpty())) return;
		showProgress();
	}

	@Override
	public void onScansResultReceive(List<ScanResult> scanResults) {
		wifiList.update(scanResults);
		if (wifi.isConnectedOrConnecting() || wifi.isConnected() || wifiDialog.getVisibility() == View.VISIBLE) return;
		hideProgress();
		wifiList.show();
	}

}