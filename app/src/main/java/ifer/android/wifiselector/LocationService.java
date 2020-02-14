package ifer.android.wifiselector;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.DateFormat;
import java.util.Date;

public class LocationService extends Service {
    public static final String TAG="WifiSelector";


    // location updates interval - 10sec
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    // fastest updates interval - 5 sec
    // location updates will be received if another app is requesting the locations
    // than your app can handle
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 5000;

    private static final int REQUEST_CHECK_SETTINGS = 100;


    // bunch of location related apis
    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;
    private Location mLastLocation;

    // boolean flag to toggle the ui
    private Boolean mRequestingLocationUpdates;


    private WifiSelector wifiSelector;

    private Context mContext;

    @Override
    public void onCreate()
    {
        Log.d(TAG, "LocationService onCreate");
        mContext = GlobalApplication.getAppContext();
        wifiSelector = GlobalApplication.getWifiSelector();

        init();
    }

    private void init() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
Log.d(TAG, "Location is received");
                mCurrentLocation = locationResult.getLastLocation();


                if (mLastLocation != null) {
                    float[] result = new float[1];

                    Location.distanceBetween(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(),
                            mLastLocation.getLatitude(), mLastLocation.getLongitude(), result);
Log.d(TAG, "Distance=" + String.valueOf(result[0]));
                }

                mLastLocation = mCurrentLocation;

                Log.d(TAG, ">>>> LocationService Scanning...");
                wifiSelector.scanWifi();
            }
        };

        mRequestingLocationUpdates = false;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(12F);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    /**
     * Starting location updates
     * Check whether location settings are satisfied and then
     * location updates will be requested
     */
    private void startLocationUpdates() {
        mRequestingLocationUpdates = true;

        mSettingsClient
                .checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener( new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
Log.d(TAG, "Started location updates!");

//                        Toast.makeText(mContext, "Started location updates!", Toast.LENGTH_SHORT).show();

                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());

//                        updateLocationUI();
                    }
                })
                .addOnFailureListener( new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.d(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);

//                                Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
                        }

//                        updateLocationUI();
                    }
                });
    }

    public void stopLocationUpdates() {
        mRequestingLocationUpdates = false;

        // Removing location updates
        mFusedLocationClient
                .removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
Log.d(TAG, "Stopped location updates!");
//                        Toast.makeText(mContext, "Location updates stopped!", Toast.LENGTH_SHORT).show();
//                        toggleButtons();
                    }
                });
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);
Log.d(TAG, "LocationService onStartCommand");

        startForeground(12345678, getNotification());
        startLocationUpdates();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "LocationService onDestroy");
        stopLocationUpdates();

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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

        NotificationChannel channel = new NotificationChannel("channel_01", "My Channel", NotificationManager.IMPORTANCE_DEFAULT);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        Notification.Builder builder = new Notification.Builder(getApplicationContext(), "channel_01").setAutoCancel(true);
        return builder.build();
    }


}
