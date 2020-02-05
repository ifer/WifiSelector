package ifer.android.wifiselector;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class WifiBoundService extends Service {
    private static String TAG = "WifiSelector";

//    private final int SIGNAL_LEVEL_THRESHOLD = 10;

    private IBinder mBinder = new ServiceBinder();

    private SharedPreferences settings;

//    private WifiManager wifiManager;
    private String curSSID;
    private List<ScanResult> results;
    private ArrayList<WifiEntry> wifiArrayList = new ArrayList<WifiEntry>();
    private ArrayList<String> registeredSSIDList = new ArrayList<String>();
    private UserOptions userOptions;
    private Handler handler = new Handler();
    private Runnable periodicUpdate;

//    private boolean boundOnly = true;

    private WifiSelector wifiSelector;

    @Override
    public void onCreate() {
        super.onCreate();
//Log.d(TAG, "service onCreate");
//        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiSelector = new WifiSelector();
//        settings = getApplicationContext().getSharedPreferences(UserOptionsHelper.SETTINGS_NAME, 0);

    }


    public void scanWifi(){
        wifiSelector.scanWifi();
        this.curSSID = wifiSelector.getCurSSID();
        this.wifiArrayList = wifiSelector.getWifiArrayList();
        this.registeredSSIDList = wifiSelector.getRegisteredSSIDList();
    }

    private void mainWork (){
        wifiSelector.scanWifi();


        if (periodicUpdate != null) {
            handler.removeCallbacks(periodicUpdate);
        }


        periodicUpdate = new Runnable() {
            public void run() {
                wifiSelector.scanWifi();
                handler.postDelayed(this, 1000 * 30 * userOptions.getAlarmInterval());
//                handler.postDelayed(this, 1000 * 60 * userOptions.getAlarmInterval());
            }
        };
        handler.postDelayed(periodicUpdate, 1000  * 30 * userOptions.getAlarmInterval());
//        handler.postDelayed(periodicUpdate, 1000  * 60 * userOptions.getAlarmInterval());

    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
Log.d(TAG, "service onBind");

//        if ( boundOnly){
            if (intent != null && intent.getSerializableExtra("UserOptions") != null) {
                userOptions = (UserOptions) intent.getSerializableExtra("UserOptions");
            }
            else{
                userOptions = UserOptionsHelper.loadUserOptions();
            }

            mainWork();

//        }

        return mBinder;
    }


    public class ServiceBinder extends Binder {
        WifiBoundService getService() {
            return WifiBoundService.this;
        }
    }

    @Override
    public void onRebind(Intent intent) {
Log.d(TAG, "service onRebind");
        super.onRebind(intent);
    }
    @Override
    public boolean onUnbind(Intent intent) {
Log.d(TAG, "service onUnbind");
//        if (boundOnly) {
//Log.d(TAG, "Stopping periodicUpdate") ;
            handler.removeCallbacks(periodicUpdate);
//        }
        return true;
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

    public void setUserOptions(UserOptions userOptions) {
        this.userOptions = userOptions;
        wifiSelector.setUserOptions(userOptions);
    }
}
