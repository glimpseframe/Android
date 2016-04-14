package odesk.johnlife.skylight.util;

import android.net.wifi.ScanResult;

import java.util.List;

public interface WifiConnectionListener {
    void onConnecting();
    void onConnected();
    void onDisconnected(WifiReceiver.WifiError error);
    void onScanning();
    void onScansResultReceive(List<ScanResult> scanResults);
}
