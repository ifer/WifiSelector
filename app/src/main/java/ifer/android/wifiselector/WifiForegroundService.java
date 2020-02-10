package ifer.android.wifiselector;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class WifiForegroundService extends Service {
    private static String TAG = "WifiSelector";
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
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
Log.d(TAG, "service onStartCommand");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =  PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
        {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel( MainActivity.CHANNEL_ID,
                    mContext.getString(R.string.notif_text),
                    importance);
//            mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
            mNotificationManager.createNotificationChannel(notificationChannel);
        }

        Notification notification = new NotificationCompat.Builder(mContext, MainActivity.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_wifisel)
                .setAutoCancel(true)
                .setContentTitle(mContext.getResources().getString(R.string.app_name))
                .setContentText(mContext.getResources().getString(R.string.notif_text))
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

        wifiSelector.scanWifi();

        stopSelf();

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
//        Log.d(TAG, "service onBind");


        return null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "service onDestroy");


    }

}
