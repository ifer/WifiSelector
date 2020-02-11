package ifer.android.wifiselector;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.support.v4.app.NotificationCompat;
import java.util.ArrayList;
import java.util.List;

// Service that runs during the time that the visual activity is active.
// It gets started and terminated by the MainActivity (onStart and OnStop events)
// It performs periodic wifi scans using Handler
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
    private Handler handler = new Handler();
    private Runnable periodicUpdate;

    private Context mContext;
//    private boolean boundOnly = true;


    private WifiSelector wifiSelector;

    @Override
    public void onCreate() {
        super.onCreate();
//Log.d(TAG, "service onCreate");
        mContext = GlobalApplication.getAppContext();
        wifiSelector = new WifiSelector();

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
Log.d(TAG, "Bound service triggers scanWifi()");
                wifiSelector.scanWifi();
//                handler.postDelayed(this, 1000 * 30 * userOptions.getAlarmInterval());
                handler.postDelayed(this, 1000 * 60 * UserOptions.getAlarmInterval());
            }
        };
//        handler.postDelayed(periodicUpdate, 1000  * 30 * userOptions.getAlarmInterval());
        handler.postDelayed(periodicUpdate, 1000  * 60 * UserOptions.getAlarmInterval());

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
Log.d(TAG, "service onBind");

        mainWork();

        return mBinder;
    }


    public class ServiceBinder extends Binder {
        WifiBoundService getService() {
            return WifiBoundService.this;
        }
    }

    @Override
    public void onRebind(Intent intent) {
//Log.d(TAG, "service onRebind");
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


}
