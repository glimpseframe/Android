package odesk.johnlife.glimpse.activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import odesk.johnlife.glimpse.R;
import odesk.johnlife.glimpse.adapter.ImagePagerAdapter;
import odesk.johnlife.glimpse.app.GlimpseApp;
import odesk.johnlife.glimpse.data.DatabaseHelper;
import odesk.johnlife.glimpse.ui.BlurActionBar;
import odesk.johnlife.glimpse.ui.BlurActionBar.OnActionClick;
import odesk.johnlife.glimpse.util.MailConnector;
import odesk.johnlife.glimpse.util.WifiConnector;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class PhotoActivity extends Activity {

	// private Bitmap activeImage = null;

	private ProgressBar progressBar;
	private View listPane;
	private View errorPane;
	private TextView errorText;
	private WifiManager wifi;
	private WifiConnectionHandler wifiConnectionHandler = new WifiConnectionHandler();
	private Context context;
	private DatabaseHelper databaseHelper;
	private ViewPager pager;
	private ImagePagerAdapter pagerAdapter;
	private BlurActionBar actionBar;
	private boolean isConnectErrorVisible = false;
	private Timer mailTimer = new Timer();

	public interface ConnectedListener {
		public void onConnected();
	}

	private class WifiConnectionHandler {
		private ListView list;
		private View wifiDialog;
		private TextView password;
		private TextView networkName;
		private ScanResult activeNetwork;

		private final Runnable focusRunnable = new Runnable() {
			@Override
			public void run() {
				password.requestFocus();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(password, InputMethodManager.SHOW_IMPLICIT);
			}
		};

		private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context c, Intent intent) {
				String action = intent.getAction();
				if (action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
					if (isConnectedOrConnecting() || isConnectErrorVisible == true) return;
					TreeSet<ScanResult> sortedResults = new TreeSet<ScanResult>(
							new Comparator<ScanResult>() {
								@Override
								public int compare(ScanResult lhs,
										ScanResult rhs) {
									return -WifiManager.compareSignalLevel(
											lhs.level, rhs.level);
								}
							});
					sortedResults.addAll(wifi.getScanResults());
					ArrayList<ScanResult> scanResults = new ArrayList<ScanResult>(
							sortedResults.size());
					TreeSet<String> nameLans = new TreeSet<String>();
					for (ScanResult net : sortedResults) {
						if (!net.SSID.trim().isEmpty()
								&& nameLans.add(net.SSID)) {
							scanResults.add(net);
						}
					}
					final ArrayAdapter<ScanResult> adapter = new ArrayAdapter<ScanResult>(
							context, R.layout.wifi_list_item,
							scanResults) {
						@Override
						public View getView(int position, View convertView,
								ViewGroup parent) {
							TextView view = (TextView) super.getView(position,
									convertView, parent);
							view.setText(getItem(position).SSID);
							return view;
						}
					};
					list.setAdapter(adapter);
					list.setOnItemClickListener(new OnItemClickListener() {
						@Override
						public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
							activeNetwork = adapter.getItem(position);
							if (activeNetwork.capabilities.startsWith("[ESS")) {
								hideListPane();
								progressBar.setVisibility(View.VISIBLE);
								new WifiConnector(PhotoActivity.this).connectTo(activeNetwork);
							} else {
								wifiDialog.setVisibility(View.VISIBLE);
								password.setText("");
								password.postDelayed(focusRunnable, 150);
								password.requestFocus();
								networkName.setText(activeNetwork.SSID);
							}
						}
					});
					if (isConnectedOrConnecting()) return;
					progressBar.setVisibility(View.INVISIBLE);
					errorPane.setVisibility(View.INVISIBLE);
					listPane.setVisibility(View.VISIBLE);
					listPane.setAlpha(0);
					listPane.setTranslationX(listPane.getWidth());
					listPane.animate().translationX(0).alpha(1).start();
				} else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
					NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
					boolean connected = info.getState() == NetworkInfo.State.CONNECTED;
					boolean connecting = info.getState() == NetworkInfo.State.CONNECTING;
					boolean visible = listPane.getVisibility() == View.VISIBLE;
					if (connected) {
						connectedListener.onConnected();
					}
					if (!visible && !connected && !connecting) {
						scanWifi();
					}
				}
			}
		};

		private void hideListPane() {
			listPane.animate()
			.translationX(listPane.getWidth()).alpha(0)
			.setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					listPane.setVisibility(View.INVISIBLE);
					listPane.setTranslationX(0);
					listPane.setAlpha(1);
					listPane.animate().setListener(null).start();
				}
			}).start();
		}
		
		public void createUi(Bundle savedInstanceState) {
			list = (ListView) findViewById(R.id.list);
			listPane = findViewById(R.id.list_container);
			wifiDialog = findViewById(R.id.wifi_pane);
			password = (TextView) wifiDialog.findViewById(R.id.password);
			password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_DONE) {
						connectToNetwork();
						return true;
					}
					return false;
				}
			});
			networkName = (TextView) wifiDialog.findViewById(R.id.title);
			wifiDialog.findViewById(R.id.connect).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						connectToNetwork();
					}
				});
			if (null == savedInstanceState) {
				wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
				if (!wifi.isWifiEnabled()) {
					wifi.setWifiEnabled(true);
					scanWifi();
				}
			}
			if (isConnectedOrConnecting()) {
				listPane.setVisibility(View.INVISIBLE);
			} else {
				scanWifi();
			}
			registerReceiver(wifiScanReceiver, new IntentFilter(
					WifiManager.NETWORK_STATE_CHANGED_ACTION));
		}

		public void connectToNetwork() {
			hideConnectionDialog();
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(password.getWindowToken(), 0);
			if (activeNetwork != null) {
				hideListPane();
				progressBar.setVisibility(View.VISIBLE);
				WifiConnector wifiConnector = new WifiConnector(PhotoActivity.this);
				wifiConnector.connectTo(activeNetwork, password.getText().toString());
				if (wifiConnector.getConnectionResult() == -1) {
					progressBar.setVisibility(View.INVISIBLE);
					errorText.setText(R.string.error_not_connected);
					errorPane.setVisibility(View.VISIBLE);
					isConnectErrorVisible = true;
					errorPane.postDelayed(hideErrorPane, 5000);
				}
			}
		}

		public void scanWifi() {
			wifi.startScan();
			registerReceiver(wifiScanReceiver, new IntentFilter(
					WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		}

		public View getView() {
			return listPane;
		}

		public boolean isConnectionDialogVisible() {
			if (wifiDialog == null) {
				return false;
			} else {
				return wifiDialog.getVisibility() == View.VISIBLE;
			}
		}

		public void hideConnectionDialog() {
			wifiDialog.setVisibility(View.INVISIBLE);
		}
	}

	private Runnable swipeRunnable = new Runnable() {
		@Override
		public void run() {
			View[] swipeBlockers = {
				wifiConnectionHandler.getView(),
				errorPane, 
				progressBar 
			};
			boolean blocked = getActionBar().isShowing() || actionBar.isFreeze()|| !isConnectedOrConnecting();
			for (View blocker : swipeBlockers) {
				blocked |= blocker.getVisibility() == View.VISIBLE;
			}
			if (blocked) {
				pager.postDelayed(swipeRunnable, 50);
			} else {
//				pagerAdapter.notifyDataSetChanged();
				int idx = pager.getCurrentItem()+1;
				if (idx == pagerAdapter.getCount()) {
					idx = 0;
				}
				pager.setCurrentItem(idx);
				rescheduleImageSwipe();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_photo);
		createActionBar();
		context = this;
		databaseHelper = DatabaseHelper.getInstance(getApplicationContext());
		pager = (ViewPager) findViewById(R.id.pager);
		pagerAdapter = new ImagePagerAdapter(this, databaseHelper, new OnClickListener() {
			@Override
			public void onClick(View v) {
				ActionBar actionBar = getActionBar();
				if (actionBar.isShowing()) {
					actionBar.hide();
				} else {
					actionBar.show();
				}
			}
		});
		pager.setAdapter(pagerAdapter);
		pager.setOffscreenPageLimit(2);
		pager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				rescheduleImageSwipe();
				pagerAdapter.setImageShown(position);
				if (actionBar.isFreeze()) {
					actionBar.unFreeze();
				}
			}
		});
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		errorPane = findViewById(R.id.error_pane);
		errorText = ((TextView) errorPane.findViewById(R.id.error_text));
		wifiConnectionHandler.createUi(savedInstanceState);
		progressBar = (ProgressBar) findViewById(R.id.progressLoading);
		final String user = getUser();
		if (user == null) {
			errorText.setText(R.string.error_no_user_data);
			errorPane.setVisibility(View.VISIBLE);
			return;
		}
		rescheduleImageSwipe();
		mailTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (isConnected()) {
					MailConnector mailer = new MailConnector(user, "HPgqL2658P", context);
					mailer.connect();
					if (!GlimpseApp.getFileHandler().isEmpty()) {
						hideErrorPane();
					}
				}
			}
		}, 0, 120000);
	}
	
	
	
	@Override
	protected void onDestroy() {
		mailTimer.cancel();
		super.onDestroy();
	}



	private void createActionBar() {
		actionBar = new BlurActionBar(this);
		actionBar.setOnActionClickListener(new OnActionClick() {
			@Override
			public void onClick(View v) {
				if (v.getId() == R.id.action_delete) {
					pagerAdapter.deleteCurrentItem(pager);
				} else if (v.getId() == R.id.action_freeze) {
//					pager.setSwipeable(!actionBar.isFreeze());
				} else if (v.getId() == R.id.action_reset_wifi) {
					if (isConnected()) {
						new WifiConnector(context).forgetCurrent();
					}
				}
			}
		});
	}

	private String getUser() {
		String user = null;
		try {
			File dataFile = new File(
				Environment.getExternalStorageDirectory(),
				context.getString(R.string.data_file)
			);
			if (!dataFile.exists()) return null;
			BufferedReader br = new BufferedReader(new FileReader(dataFile));
			String line = br.readLine();
			if (line != null) {
				user = line;
			}
			br.close();
		} catch (Exception e) {
			Log.e("UserInfo", e.getMessage(), e);
		}
		return user;
	}

	private void hideErrorPane() {
		if (errorPane.getVisibility() == View.VISIBLE) {
			runOnUiThread(hideErrorPane);
		}
	}

	private Runnable hideErrorPane = new Runnable() {
		@Override
		public void run() {
			errorPane.setVisibility(View.INVISIBLE);
			if (isConnectErrorVisible == true) {
				isConnectErrorVisible = false;
				wifiConnectionHandler.scanWifi();
			}
		}
	};

	private void rescheduleImageSwipe() {
		pager.removeCallbacks(swipeRunnable);
		pager.postDelayed(swipeRunnable, 8000);
	}

	public ConnectedListener connectedListener = new ConnectedListener() {
		@Override
		public void onConnected() {
			progressBar.setVisibility(View.INVISIBLE);
			wifiConnectionHandler.hideListPane();
			if (GlimpseApp.getFileHandler().isEmpty() && getUser() != null) {
				String message = getString(R.string.error_no_foto, getUser());
				errorText.setText(message);
				errorPane.setVisibility(View.VISIBLE);
			}
		}
	};

	private boolean isConnectedOrConnecting() {
		return getNetworkInfo().isConnectedOrConnecting();
	}

	public boolean isConnected() {
		return getNetworkInfo().isConnected();
	}

	private NetworkInfo getNetworkInfo() {
		ConnectivityManager connectionManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		return connectionManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onBackPressed() {
		if (wifiConnectionHandler.isConnectionDialogVisible()) {
			wifiConnectionHandler.hideConnectionDialog();
		} else {
			super.onBackPressed();
		}
	}
}
