package odesk.johnlife.skylight.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import odesk.johnlife.skylight.Constants;
import odesk.johnlife.skylight.task.WifiRedirectionTask;

public class WifiReceiver implements Constants {

    public enum WifiError { NEED_PASSWORD, CONNECT_ERROR, DISCONNECTED, UNKNOWN_ERROR, NONE }

    private class OpenConnector {

        protected WifiConfiguration configure() {
            WifiConfiguration config = new WifiConfiguration();
            config.SSID = addQuotes(selectedNetwork.SSID);
            config.status = WifiConfiguration.Status.ENABLED;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            return config;
        }

        protected void connect() {
            selectedNetworkId = wifi.addNetwork(configure());
            wifi.disconnect();
            wifi.enableNetwork(selectedNetworkId, true);
            wifi.reconnect();
        }

        protected final String addQuotes(String password) {
            return String.format("\"%s\"", password);
        }
    }

    private class WepConnector extends OpenConnector {
        @Override
        protected WifiConfiguration configure() {
            WifiConfiguration config = new WifiConfiguration();
            config.SSID = addQuotes(selectedNetwork.SSID);
            config.status = WifiConfiguration.Status.DISABLED;
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.priority = 40;
            if (isHexString(selectedNetworkPass)) {
                config.wepKeys[0] = selectedNetworkPass;
            } else {
                config.wepKeys[0] = addQuotes(selectedNetworkPass);
            }
            config.wepTxKeyIndex = 0;
            return config;
        }

        private boolean isHexString(String s) {
            if (s == null) {
                return false;
            }
            int len = s.length();
            if (len != 10 && len != 26 && len != 58) {
                return false;
            }
            for (int i = 0; i < len; ++i) {
                char c = s.charAt(i);
                if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) {
                    continue;
                }
                return false;
            }
            return true;
        }

    }

    private class WpaConnector extends OpenConnector {
        @Override
        protected WifiConfiguration configure() {
            WifiConfiguration config = new WifiConfiguration();
            config.SSID = addQuotes(selectedNetwork.SSID);
            config.BSSID = selectedNetwork.BSSID;
            config.preSharedKey = addQuotes(selectedNetworkPass);
            config.status = WifiConfiguration.Status.ENABLED;
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            return config;
        }
    }

    private final BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
                if (state == SupplicantState.DISCONNECTED) {
                    int errorCode = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
                    if (errorCode == WifiManager.ERROR_AUTHENTICATING) {
                        if (selectedNetwork != null) prefs.edit().remove(selectedNetwork.BSSID).apply();
                        listener.onDisconnected(WifiError.CONNECT_ERROR);
                    }
                } else if (state == SupplicantState.INACTIVE && isConnecting) {
                    isConnecting = false;
                    resetCurrentWifi();
                    listener.onDisconnected(WifiError.CONNECT_ERROR);
                    scanWifi();
                }
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                final NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                NetworkInfo.DetailedState details = info.getDetailedState();
                boolean connected = info.getState() == NetworkInfo.State.CONNECTED;
                boolean connecting = info.getState() == NetworkInfo.State.CONNECTING;
                boolean isSuspended = info.getState() == NetworkInfo.State.SUSPENDED;
                boolean unknown = info.getState() == NetworkInfo.State.UNKNOWN;
                if (isSuspended || unknown) {
                    isConnecting = false;
                    listener.onDisconnected(WifiError.UNKNOWN_ERROR);
                } else if (details == NetworkInfo.DetailedState.DISCONNECTED) {
                    listener.onDisconnected(isConnecting ? WifiError.DISCONNECTED : WifiError.NONE);
                    isConnecting = false;
                    resetCurrentWifi();
                    connectionTimeoutHandler.removeCallbacks(connectionTimeout);
                } else if (connected && details == NetworkInfo.DetailedState.CONNECTED) {
                    isConnecting = true;
                    WifiRedirectionTask redirectionTask = new WifiRedirectionTask() {
                        @Override
                        protected void onPostExecute(Boolean result) {
                            connectionTimeoutHandler.removeCallbacks(connectionTimeout);
                            isConnecting = false;
                            if (result) {
                                unregisterScanReceiver();
                                stopRefresher();
                                listener.onConnected();
                                if (null != selectedNetwork) {
                                    prefs.edit()
                                            .putString(PREF_WIFI_BSSID, selectedNetwork.BSSID)
                                            .putString(selectedNetwork.BSSID, selectedNetworkPass)
                                            .apply();
                                }
                            } else {
                                resetCurrentWifi();
                            }
                        }
                    };
                    redirectionTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else if (!connected && !connecting && details != NetworkInfo.DetailedState.SCANNING) {
                    scanWifi();
                }
            }
        }
    };

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (isConnecting) return;
            if (ACTION_WIFI_SCAN.equals(intent.getAction())) {
                listener.onScanning();
                wifi.startScan();
            } else {
                listener.onScansResultReceive(wifi.getScanResults());
            }
        }
    };

    private int selectedNetworkId;
    private ScanResult selectedNetwork;
    private String selectedNetworkPass;
    private static WifiReceiver instance;
    private WifiManager wifi;
    private Context context;
    private WifiConnectionListener listener;
    private SharedPreferences prefs;
    private AlarmManager wifiRefresher;
    private PendingIntent wifiPendingIntent;
    private boolean isRefresherPaused;
    private boolean isConnecting;
    private Handler connectionTimeoutHandler = new Handler();
    private Runnable connectionTimeout = new Runnable() {
        @Override
        public void run() {
            if (isConnecting) wifi.removeNetwork(selectedNetworkId);
        }
    };

    private WifiReceiver(Context context, WifiConnectionListener listener) {
        this.context = context;
        this.listener = listener;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled()) wifi.setWifiEnabled(true);
    }

    private void startRefresher() {
        stopRefresher();
        wifiRefresher = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ACTION_WIFI_SCAN);
        wifiPendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        wifiRefresher.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), REFRESH_RATE, wifiPendingIntent);
    }

    private void stopRefresher() {
        if (wifiRefresher == null) return;
        try {
            wifiRefresher.cancel(wifiPendingIntent);
        } catch (Exception e) {
            Log.e("Cancel Refresher", e.getMessage(), e);
        }
        wifiRefresher = null;
        wifiPendingIntent = null;
    }

    public static WifiReceiver createInstance(Context context, WifiConnectionListener listener) {
        instance = new WifiReceiver(context, listener);
        return instance;
    }

    public static WifiReceiver getInstance() {
        return instance;
    }

    public void connectToNetwork(ScanResult selectedNetwork) {
        this.selectedNetwork = selectedNetwork;
        connectToSelectedNetwork("");
    }

    public void connectToSelectedNetwork(String pass) {
        listener.onConnecting();
        selectedNetworkPass = pass;
        String cap = selectedNetwork.capabilities;
        if (pass.isEmpty() && !(cap.isEmpty() || cap.startsWith("[ESS"))) {
            listener.onDisconnected(WifiError.NEED_PASSWORD);
        } else {
            connectionTimeoutHandler.removeCallbacks(connectionTimeout);
            connectionTimeoutHandler.postDelayed(connectionTimeout, CONNECTION_TIMEOUT);
            isConnecting = true;
            if (cap.contains("[WPA")) {
                new WpaConnector().connect();
            } else if (cap.contains("[WEP")) {
                new WepConnector().connect();
            } else {
                new OpenConnector().connect();
            }
            checkConnectionResult();
        }
    }

    private void checkConnectionResult() {
        if (selectedNetworkId != -1) {
//            prefs.edit()
//                .putString(PREF_WIFI_BSSID, selectedNetwork.BSSID)
//                .putString(selectedNetwork.BSSID, selectedNetworkPass)
//                .apply();
        } else {
            isConnecting = false;
            listener.onDisconnected(WifiError.UNKNOWN_ERROR);
            resetCurrentWifi();
            scanWifi();
        }
    }

    public void scanWifi() {
        registerScanReceiver();
        startRefresher();
    }

    public boolean isConnectedOrConnecting() {
        return getNetworkInfo().isConnectedOrConnecting();
    }

    public boolean isConnected() {
        return getNetworkInfo().isConnected();
    }

    public void resetCurrentWifi() {
        WifiInfo connection = wifi.getConnectionInfo();
        if (null == connection) return;
        wifi.removeNetwork(connection.getNetworkId());
    }

    private NetworkInfo getNetworkInfo() {
        ConnectivityManager connectionManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectionManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    }

    public ScanResult getSelectedNetwork() {
        return selectedNetwork;
    }

    public void register() {
        IntentFilter filter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        context.registerReceiver(wifiStateReceiver, filter);
    }

    private void registerScanReceiver() {
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(ACTION_WIFI_SCAN);
        context.registerReceiver(wifiScanReceiver, filter);
    }

    public void onResume() {
        if (!isConnectedOrConnecting()) {
            listener.onDisconnected(WifiError.NONE);
        } else {
            if (isRefresherPaused) {
                startRefresher();
                isRefresherPaused = false;
            }
        }
    }

    public void onPause() {
        isRefresherPaused = wifiRefresher != null;
        stopRefresher();
    }

    public void unregister() {
        try {
            context.unregisterReceiver(wifiStateReceiver);
        } catch (Exception e) {}
        unregisterScanReceiver();
    }

    private void unregisterScanReceiver() {
        try {
            context.unregisterReceiver(wifiScanReceiver);
        } catch (Exception e) {}
        stopRefresher();
    }

}