package ifer.android.wifiselector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.List;

public class WifiScanResultsReceiver extends BroadcastReceiver {

    private WifiManager wifiManager;
    private Context context;
    private WifiSelector wifiSelector = new WifiSelector();


    @Override
    public void onReceive(Context context, Intent intent) {
        wifiManager =  (WifiManager) context.getSystemService(this.context.WIFI_SERVICE);
        this.context = GlobalApplication.getAppContext();
        List<ScanResult> results = wifiManager.getScanResults();
        context.unregisterReceiver(this);

Log.d(MainActivity.TAG, "WifiScanResultsReceiver ressults: " + results.size());

        wifiSelector.processScanResults(results);
    }
}