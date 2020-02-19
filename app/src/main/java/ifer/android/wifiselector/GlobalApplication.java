package ifer.android.wifiselector;

import android.app.Application;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;

import com.bugfender.sdk.Bugfender;


// Class used by utility classes which need to get the application context
public class GlobalApplication extends Application {
    private static Context appContext;
    private static WifiScanResultsReceiver wifiScanResultsReceiver;
    private static boolean receiverRegistered = false;
    private static WifiSelector wifiSelector;


    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        wifiSelector = new WifiSelector();
        wifiScanResultsReceiver = new WifiScanResultsReceiver();

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
        if (! receiverRegistered) {
            appContext.registerReceiver(wifiScanResultsReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            receiverRegistered = true;
        }
    }

    public static void unregisterWificanResultsReceiver(){
        if (receiverRegistered) {
            appContext.unregisterReceiver(wifiScanResultsReceiver);
            receiverRegistered = false;
        }
    }

    public static boolean isReceiverRegistered() {
        return receiverRegistered;
    }
}
