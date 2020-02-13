package ifer.android.wifiselector;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

// Brodacast Manager that runs after the end of the visual activity.
// It gets registered and unregistered by the MainActivity (onPause and onResume events)
// It starts the WifiForegroundService at specified intervals.
public class WifiBackgroundUpdater extends BroadcastReceiver {
    public static final String TAG="WifiSelector";
//    private static final TimeZone timezoneAthens = TimeZone.getTimeZone("Europe/Athens");

    public static final int REQUEST_CODE = 123456;
    public static final String ACTION_SCAN_WIFI = "scan_wifi";

    private Context context;

    private WifiSelector wifiSelector = new WifiSelector();

    public WifiBackgroundUpdater(){
        this.context = GlobalApplication.getAppContext();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
//        Log.d(TAG, "Alarm received: " + intent.getAction());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(new Intent(context, WifiForegroundService.class));
        } else {
            context.startService(new Intent(context, WifiForegroundService.class));
        }


        scheduleAlarm();

        if (intent.getAction().equals(ACTION_SCAN_WIFI)){
            Log.d(TAG, "ACTION_SCAN_WIFI: Alarm service triggers scanWifi()");
        }
        else if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            Log.d(TAG, "ACTION_BOOT_COMPLETED: Alarm service triggers scanWifi() and reschedules");

//            schedulePeriodicAlarm();

        }
        else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)){
            Log.d(TAG, "SCREEN ON: Alarm service triggers scanWifi()");
        }




    }

    public static void schedulePeriodicAlarm() {
        Context context = GlobalApplication.getAppContext();
        AlarmManager alarmManager= (AlarmManager) context.getSystemService(context.ALARM_SERVICE);

        Intent intent = new Intent(context, WifiBackgroundUpdater.class);
        intent.setAction(WifiBackgroundUpdater.ACTION_SCAN_WIFI);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, WifiBackgroundUpdater.REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        long intervalMillis = UserOptions.getAlarmInterval() * 60 * 1000;

        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), intervalMillis, pendingIntent);

//        Toast.makeText(this, "Alarm set every 15 seconds", Toast.LENGTH_LONG).show();
    }

    public static void scheduleAlarm() {
        Context context = GlobalApplication.getAppContext();
        AlarmManager alarmManager= (AlarmManager) context.getSystemService(context.ALARM_SERVICE);

//        int refresh_interval = interval;
        Intent intent = new Intent(context, WifiBackgroundUpdater.class);
//        PendingIntent pi=PendingIntent.getBroadcast(context, 0, i, 0);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, WifiBackgroundUpdater.REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        long intervalMillis = UserOptions.getAlarmInterval() * 60 * 1000;
        alarmManager.set (AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + intervalMillis, pendingIntent);

    }

    public static void cancelPeriodicAlarm() {
        Log.d(TAG, "Cancelling alarm..");
        Context context = GlobalApplication.getAppContext();
        AlarmManager alarmManager= (AlarmManager) context.getSystemService(context.ALARM_SERVICE);

        Intent intent = new Intent(context, WifiBackgroundUpdater.class);
        intent.setAction(WifiBackgroundUpdater.ACTION_SCAN_WIFI);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, WifiBackgroundUpdater.REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.cancel(pendingIntent);
    }
}
