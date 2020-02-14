package ifer.android.wifiselector;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

public class LocationServiceOld extends Service {
    public static final String TAG="WifiSelector";

    private final int LOCATION_INTERVAL = 500;
    private final int LOCATION_DISTANCE = 10;

    private LocationListener mLocationListener;
    private LocationManager mLocationManager;
    private NotificationManager notificationManager;

    private WifiSelector wifiSelector;

    private Context mContext;

    @Override
    public void onCreate()
    {
        Log.d(TAG, "LocationService onCreate");
        mContext = GlobalApplication.getAppContext();
        wifiSelector = GlobalApplication.getWifiSelector();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);
Log.d(TAG, "LocationService onStartCommand");

        startForeground(12345678, getNotification());
        startTracking();

        return START_NOT_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private class LocationListener implements android.location.LocationListener
    {
        private Location lastLocation = null;
        private final String TAG = "LocationListener";
        private Location mLastLocation;

        public LocationListener(String provider)
        {
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location)
        {
Log.d(TAG, "LocationService onLocationChanged");
            if (!location.equals(mLastLocation)){
                wifiSelector.scanWifi();
            }

            mLastLocation = location;
            Log.d(TAG, "LocationChanged: "+location);

        }

        @Override
        public void onProviderDisabled(String provider)
        {
            Log.d(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            Log.d(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.d(TAG, "onStatusChanged: " + status);
        }
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (mLocationManager != null) {
            try {
                mLocationManager.removeUpdates(mLocationListener);
            } catch (Exception ex) {
                Log.d(TAG, "fail to remove location listners, ignore", ex);
            }
        }
    }

    private void initializeLocationManager() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    public void startTracking() {

Log.d(TAG, "LocationService startTracking");
        initializeLocationManager();
        mLocationListener = new LocationListener(LocationManager.GPS_PROVIDER);

        try {
            mLocationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, mLocationListener );

        } catch (SecurityException ex) {
            // Log.d(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            // Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }

    }

    public void stopTracking() {
        this.onDestroy();
    }

//    private Notification getNotification(){
//        Intent notificationIntent = new Intent(this, MainActivity.class);
//        PendingIntent pendingIntent =  PendingIntent.getActivity(this, 0, notificationIntent, 0);
//
//        NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
//
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
//        {
//
//            int importance = NotificationManager.IMPORTANCE_LOW;
//            NotificationChannel notificationChannel = new NotificationChannel( MainActivity.CHANNEL_ID,
//                    mContext.getString(R.string.notif_text),
//                    importance);
////            mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
//            mNotificationManager.createNotificationChannel(notificationChannel);
//        }
//
//        Notification notification = new NotificationCompat.Builder(mContext, MainActivity.CHANNEL_ID)
//                .setSmallIcon(R.mipmap.ic_wifisel)
//                .setAutoCancel(true)
//                .setContentTitle(mContext.getResources().getString(R.string.app_name))
//                .setContentText(mContext.getResources().getString(R.string.notif_text))
//                .setContentIntent(pendingIntent)
//                .build();
//
//        return (notification);
//
//    }

    private Notification getNotification() {

        NotificationChannel channel = new NotificationChannel("channel_01", "My Channel", NotificationManager.IMPORTANCE_LOW);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        Notification.Builder builder = new Notification.Builder(getApplicationContext(), "channel_01").setAutoCancel(true);
        return builder.build();
    }


}
