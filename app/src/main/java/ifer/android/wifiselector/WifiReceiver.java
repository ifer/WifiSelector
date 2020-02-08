package ifer.android.wifiselector;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import java.util.TimeZone;

public class WifiReceiver extends BroadcastReceiver {
    public static final String TAG="WifiSelector";
//    private static final TimeZone timezoneAthens = TimeZone.getTimeZone("Europe/Athens");

    public static final int REQUEST_CODE = 123456;
    public static final String ACTION_SCAN_WIFI = "scan_wifi";

    private Context context;

    private WifiSelector wifiSelector = new WifiSelector();

    public WifiReceiver(){
        this.context = GlobalApplication.getAppContext();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received");
        if (intent.getAction().equals(ACTION_SCAN_WIFI)){
            Log.d(TAG, "ACTION_SCAN_WIFI: Alarm service triggers scanWifi()");
            wifiSelector.scanWifi();
        }
        else if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            Log.d(TAG, "ACTION_BOOT_COMPLETED: Alarm service triggers scanWifi() and reschedules");
            wifiSelector.scanWifi();
            schedulePeriodicAlarm();
        }
        else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)){
            Log.d(TAG, "SCREEN ON: Alarm service triggers scanWifi()");
            wifiSelector.scanWifi();
        }
    }

    public static void schedulePeriodicAlarm() {
        Context context = GlobalApplication.getAppContext();
        AlarmManager alarmManager= (AlarmManager) context.getSystemService(context.ALARM_SERVICE);

        Intent intent = new Intent(context, WifiReceiver.class);
        intent.setAction(WifiReceiver.ACTION_SCAN_WIFI);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, WifiReceiver.REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        long intervalMillis = UserOptions.getAlarmInterval() * 60 * 1000;

        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), intervalMillis, pendingIntent);

//        Toast.makeText(this, "Alarm set every 15 seconds", Toast.LENGTH_LONG).show();
    }

    public static void cancelPeriodicAlarm() {
        Log.d(TAG, "Cancelling alarm..");
        Context context = GlobalApplication.getAppContext();
        AlarmManager alarmManager= (AlarmManager) context.getSystemService(context.ALARM_SERVICE);

        Intent intent = new Intent(context, WifiReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, WifiReceiver.REQUEST_CODE, intent, 0);

        alarmManager.cancel(pendingIntent);
    }
}
