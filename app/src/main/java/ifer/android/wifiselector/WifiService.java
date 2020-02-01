package ifer.android.wifiselector;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.net.wifi.ScanResult;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WifiService extends Service {
    private static String TAG = "WifiSelector";
    private IBinder mBinder = new ServiceBinder();

    private WifiManager wifiManager;
    private String curSSID;
    private List<ScanResult> results;
    private ArrayList<WifiEntry> wifiArrayList = new ArrayList<WifiEntry>();
    private ArrayList<String> registeredSSIDList = new ArrayList<String>();
    private UserOptions userOptions;

    Handler handler = new Handler();


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "in onCreate");
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "in onStartCommand");

        userOptions =  (UserOptions) intent.getSerializableExtra("UserOptions");
//        Log.d(TAG, "user option alarmInterval=" + userOptions.getAlarmInterval());


        mainWork();

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }


    private void mainWork (){
        scanWifi();

        Runnable periodicUpdate = new Runnable() {
            public void run() {
                scanWifi();
                handler.postDelayed(this, 1000 * 60 * userOptions.getAlarmInterval());
            }
        };
        handler.postDelayed(periodicUpdate, 1000  * 60 * userOptions.getAlarmInterval());

    }


    private void scanWifi() {
        Log.d(MainActivity.TAG, "scanWifi!");

        getRegisteredSSIDs();

        wifiArrayList.clear();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();

    }

    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            results = wifiManager.getScanResults();
            unregisterReceiver(this);

            for (ScanResult scanResult : results) {
                int level = WifiManager.calculateSignalLevel(scanResult.level, 10);
                int percentage = (int) ((level / 10.0) * 100);

                WifiEntry wfe = new WifiEntry();

                if (scanResult.SSID == null || scanResult.SSID.trim().equals("")) {
                    scanResult.SSID = "<SSID undefined>";
                }
                wfe.setSsid(scanResult.SSID);
                wfe.setSignalLevel(String.valueOf(scanResult.level));
                wfe.setSignalPercentage(percentage);
                wfe.setSignalLabel(String.valueOf(percentage) + "%");

                if(registeredSSIDList.contains(scanResult.SSID)){
                    wfe.setRegistered(true);
                }
                if (userOptions.getSelectedSSIDs().contains(scanResult.SSID)){
//                    Log.d(MainActivity.TAG, "selected: " + scanResult.SSID);
                    wfe.setSelected(true);
                }

                wifiArrayList.add(wfe);

//                scanAdapter.notifyDataSetChanged();
            }
            Collections.sort(wifiArrayList);

//             if (userOptions.isAutoConnectToStrongest() && wifiArrayList.size() > 0){
//                connectToWifiSSID(context, wifiArrayList.get(0).getSsid());
//            }

            sendBroadcast(new Intent(MainActivity.ACTION_DATA_REFRESH));
        };
    };

//    public  String getWifiSSID(Context context) {
//        final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
//        if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
//            String ssid = connectionInfo.getSSID();
//            return ssid;
//        }
//        return "??";
//    }

    public  String getWifiSSID(Context context) {
Log.d(TAG, "Requesting " );
        if (context == null) {
            return "";
        }
        final Intent intent = context.registerReceiver(null, new IntentFilter(WifiManager.NETWORK_IDS_CHANGED_ACTION));
        if (intent != null) {
            final WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            if (wifiInfo != null) {
                final String ssid = wifiInfo.getSSID().replaceAll("\"", "");
//Log.d(TAG, "ssid=" + ssid);
                if (ssid != null) {
                    curSSID = ssid;
Log.d(TAG, "Returning " + ssid);
                    return ssid;
                }
            }
        }
Log.d(TAG, "Returning empty" );
        return "";
    }

    private void connectToWifiSSID(Context context, String ssid) {

        if (curSSID != null && curSSID.equals(ssid)){
            return;
        }
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
//            Log.d(MainActivity.TAG, "i.SSID=" + i.SSID + " ssid=" + ssid);
            if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                wifiManager.reconnect();

                curSSID = ssid.replaceAll("\"", "");


            }
        }
    }

    private void getRegisteredSSIDs (){
        registeredSSIDList.clear();
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
            if (i.SSID != null ) {
                registeredSSIDList.add(i.SSID.replaceAll("\"", ""));
            }
        }
    }

    public ArrayList<WifiEntry> getWifiArrayList() {
        return wifiArrayList;
    }

    public ArrayList<String> getRegisteredSSIDList() {
        return registeredSSIDList;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service bound");

        return mBinder;
    }


    public class ServiceBinder extends Binder {
        WifiService getService() {
            return WifiService.this;
        }
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "in onRebind");
        super.onRebind(intent);
    }
    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "in onUnbind");
        return true;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "in onDestroy");
//        mChronometer.stop();
    }



}
