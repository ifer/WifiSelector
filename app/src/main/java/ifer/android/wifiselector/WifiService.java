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

    public static final String SETTINGS_NAME = "wifi_prefs";
    public static final String OPTION_SSIDS = "opt_ssids";
    public static final String OPTION_BACKGRND = "opt_backgrnd";
    public static final String OPTION_INTERVAL = "opt_interval";
    public static final String OPTION_AUTOCONNECT = "opt_autoconnect";

    private SharedPreferences settings;

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


        settings = getApplicationContext().getSharedPreferences(SETTINGS_NAME, 0);

        if (intent != null && intent.getSerializableExtra("UserOptions") != null) {
            userOptions = (UserOptions) intent.getSerializableExtra("UserOptions");
        }
        else{
            userOptions = loadUserOptions();
        }

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

//    public  String getWifiSSID(Context context) {
//        final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
//        if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
//            String ssid = connectionInfo.getSSID();
//            return ssid;
//        }
//        return "??";
//    }

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

//    public static final String OPTION_BACKGRND = "opt_backgrnd";
//    public static final String OPTION_INTERVAL = "opt_interval";
//    public static final String OPTION_AUTOCONNECT = "opt_autoconnect";

    public void saveUserOptions(UserOptions userOptions){
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(OPTION_BACKGRND, userOptions.isRunInBackground());
        editor.putInt(OPTION_INTERVAL, userOptions.getAlarmInterval());
        editor.putBoolean(OPTION_AUTOCONNECT, userOptions.isAutoConnectToStrongest());

        for (WifiEntry wifi : wifiArrayList){
            if (wifi.isSelected()) {
                userOptions.getSelectedSSIDs().add(wifi.getSsid());
            }
        }
        editor.putStringSet(OPTION_SSIDS, userOptions.getSelectedSSIDs());
        editor.apply();
    }

    public UserOptions loadUserOptions(){
        UserOptions userOptions = new UserOptions();
        userOptions.setAlarmInterval(settings.getInt(OPTION_INTERVAL, 2));
        userOptions.setAutoConnectToStrongest(settings.getBoolean(OPTION_AUTOCONNECT, true));
        userOptions.setRunInBackground(settings.getBoolean(OPTION_BACKGRND, true));

        HashSet<String> def = new HashSet<String>(); //default values: empty
        HashSet<String> ps = (HashSet<String>)settings.getStringSet(OPTION_SSIDS, def);

        userOptions.setSelectedSSIDs(ps);

        return(userOptions);
    }


    public void saveSelectedSSIDs(UserOptions userOptions){
        userOptions.getSelectedSSIDs().clear();
        for (WifiEntry wifi : wifiArrayList){
            if (wifi.isSelected()) {
//Log.d(MainActivity.TAG, "Saving entry: " + wifi.getSsid() );
                userOptions.getSelectedSSIDs().add(wifi.getSsid());
            }
        }
        SharedPreferences.Editor editor = settings.edit();
        editor.putStringSet(OPTION_SSIDS, userOptions.getSelectedSSIDs());
        editor.apply();
    }

    public HashSet<String> loadSelectedSSIDs(){
        HashSet<String> def = new HashSet<String>(); //default values: empty
        HashSet<String> ps = (HashSet<String>)settings.getStringSet(OPTION_SSIDS, def);

//        Iterator<String> i=ps.iterator();
//        while(i.hasNext())        {
//              Log.d(MainActivity.TAG, "Loading entry: " + i.next());
//        }
//
        return (ps);
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
