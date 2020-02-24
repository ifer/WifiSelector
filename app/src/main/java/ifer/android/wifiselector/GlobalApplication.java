package ifer.android.wifiselector;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;

import com.bugfender.sdk.Bugfender;


// Class used by utility classes which need to get the application context
public class GlobalApplication extends Application {
    private static Context appContext;
    private static WifiScanResultsReceiver wifiScanResultsReceiver;
    private static boolean resultsReceiverRegistered = false;
    private static WifiSelector wifiSelector;

    private static EventReceiver eventReceiver;
    private static boolean eventReceiverRegistered = false;


    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        wifiSelector = new WifiSelector();
        wifiScanResultsReceiver = new WifiScanResultsReceiver();
        eventReceiver = new EventReceiver();

        Bugfender.init(this, "24ro9krZG86j2R5xtX9kEgbktm0zyBpW", BuildConfig.DEBUG);
        Bugfender.enableCrashReporting();
        Bugfender.enableUIEventLogging(this);
        Bugfender.enableLogcatLogging(); // optional, if you want logs automatically collected from logcat

    }

    public static Context getAppContext() {
        return appContext;
    }


    public static WifiSelector getWifiSelector() {
        return wifiSelector;
    }


    public static WifiScanResultsReceiver getWifiScanResultsReceiver() {
        return wifiScanResultsReceiver;
    }

    public static void registerWificanResultsReceiver(){
        if (!resultsReceiverRegistered) {
            appContext.registerReceiver(wifiScanResultsReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            resultsReceiverRegistered = true;
        }
    }

    public static void unregisterWificanResultsReceiver(){
        if (resultsReceiverRegistered) {
            appContext.unregisterReceiver(wifiScanResultsReceiver);
            resultsReceiverRegistered = false;
        }
    }

    public static boolean isResultsReceiverRegistered() {
        return resultsReceiverRegistered;
    }

    // Register EventReceiver for events updating when app is off
    public static void registerEventReceiver (){
        IntentFilter filter = new IntentFilter();
//        filter.addAction(EventReceiver.ACTION_SCAN_WIFI);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);

        eventReceiver = new EventReceiver();
        appContext.registerReceiver(eventReceiver, filter);
        eventReceiverRegistered = true;
    }

    public static void unregisterEventReceiver () {
        appContext.unregisterReceiver(eventReceiver);
        eventReceiverRegistered = false;
    }

    public static boolean isEnentReceiverRegistered() {
        return eventReceiverRegistered;
    }

    public static EventReceiver getEventReceiver() {
        return eventReceiver;
    }
}
