package ifer.android.wifiselector;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.net.wifi.ScanResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class WifiService extends Service {
    private static String TAG = "WifiSelector";
    private IBinder mBinder = new ServiceBinder();

//    public static final String SETTINGS_NAME = "wifi_prefs";
//    public static final String OPTION_SSIDS = "opt_ssids";
//    public static final String OPTION_BACKGRND = "opt_backgrnd";
//    public static final String OPTION_INTERVAL = "opt_interval";
//    public static final String OPTION_AUTOCONNECT = "opt_autoconnect";

    private SharedPreferences settings;

    private WifiManager wifiManager;
    private String curSSID;
    private List<ScanResult> results;
    private ArrayList<WifiEntry> wifiArrayList = new ArrayList<WifiEntry>();
    private ArrayList<String> registeredSSIDList = new ArrayList<String>();
    private UserOptions userOptions;
    private Handler handler = new Handler();
    private Runnable periodicUpdate;

    private boolean boundOnly = true;

    @Override
    public void onCreate() {
        super.onCreate();
//Log.d(TAG, "service onCreate");
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        settings = getApplicationContext().getSharedPreferences(UserOptionsHelper.SETTINGS_NAME, 0);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//Log.d(TAG, "service onStartCommand");

        boundOnly = false;


        if (! boundOnly){
            if (intent != null && intent.getSerializableExtra("UserOptions") != null) {
                userOptions = (UserOptions) intent.getSerializableExtra("UserOptions");
            }
            else{
                userOptions = UserOptionsHelper.loadUserOptions();
            }

            mainWork();
        }

        // If we get killed, after returning from here, restart with the last intent that was delivered to the service
        return START_REDELIVER_INTENT;
    }


    private void mainWork (){
        scanWifi();


        if (periodicUpdate != null) {
            handler.removeCallbacks(periodicUpdate);
        }


        periodicUpdate = new Runnable() {
            public void run() {
                scanWifi();
                handler.postDelayed(this, 1000 * 60 * userOptions.getAlarmInterval());
            }
        };
        handler.postDelayed(periodicUpdate, 1000  * 60 * userOptions.getAlarmInterval());

    }


    public void scanWifi() {
Log.d(MainActivity.TAG, "scanWifi!");

        curSSID = getWifiSSID(getApplicationContext());
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

             if (userOptions.isAutoConnectToStrongest() && wifiArrayList.size() > 0){
                connectToWifiSSID(context, wifiArrayList.get(0).getSsid());
            }

            sendBroadcast(new Intent(MainActivity.ACTION_DATA_REFRESH));
        }
    };



    public static String getWifiSSID(Context context) {
        if (context == null) {
            return "";
        }
        final Intent intent = context.registerReceiver(null, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
        if (intent != null) {
            final WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            if (wifiInfo != null) {
                final String ssid = wifiInfo.getSSID().replaceAll("\"", "");
//Log.d(TAG, "ssid=" + ssid);
                if (ssid != null) {
                    return ssid;
                }
            }
        }
        return "";
    }


    private void connectToWifiSSID(Context context, String ssid) {

        if (curSSID != null && curSSID.equals(ssid)){
            return;
        }
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        if (list == null)
            return;
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
        if (list == null)
            return;
        for (WifiConfiguration i : list) {
            if (i.SSID != null ) {
                registeredSSIDList.add(i.SSID.replaceAll("\"", ""));
            }
        }
    }





    public String getCurSSID() {
        return curSSID;
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
//Log.d(TAG, "service onBind");

        if ( boundOnly){
            if (intent != null && intent.getSerializableExtra("UserOptions") != null) {
                userOptions = (UserOptions) intent.getSerializableExtra("UserOptions");
            }
            else{
                userOptions = UserOptionsHelper.loadUserOptions();
            }

            mainWork();

        }

        return mBinder;
    }


    public class ServiceBinder extends Binder {
        WifiService getService() {
            return WifiService.this;
        }
    }

    @Override
    public void onRebind(Intent intent) {
//Log.d(TAG, "service onRebind");
        super.onRebind(intent);
    }
    @Override
    public boolean onUnbind(Intent intent) {
//Log.d(TAG, "service onUnbind");
        if (boundOnly) {
//Log.d(TAG, "Stopping periodicUpdate") ;
            handler.removeCallbacks(periodicUpdate);
        }
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//Log.d(TAG, "service onDestroy");
        if (! boundOnly) {
//Log.d(TAG, "Stopping periodicUpdate") ;
            handler.removeCallbacks(periodicUpdate);
            boundOnly = false;
        }

    }



}
