package ifer.android.wifiselector;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;

public class MainActivity extends AppCompatActivity implements ServiceConnection {
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 100;

    public static final String ACTION_DATA_REFRESH = "DataRefresh";

    public static final String SETTINGS_NAME = "wifi_prefs";
    public static final String PREF_SSIDS = "sel_ssids";
    public static final String TAG="WifiSelector";

    private ListView listView;
    private Button buttonScan;
    private Button buttonSave;

    private TextView tvCurSSID;
    private int size = 0;
    private String curSSID;
    private ArrayList<WifiEntry> wifiArrayList;
    private ArrayList<String> registeredSSIDList;
    private ScanAdapter scanAdapter;
    private SharedPreferences settings;
    private HashSet<String> selectedSSIDs;

    /* Options */
//    private boolean autoConnectToStrongest = true;
//    private boolean runInBackground = true;
//    private int alarmInterval = 1; //minutes

    private UserOptions userOptions = new UserOptions();

    private WifiService wifiService;
    private UpdateReceiver updateReceiver;
//    private BoundService mBoundService;
    private boolean serviceBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();


            }
        });

        //TODO
//        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//
//        if (!wifiManager.isWifiEnabled()) {
//            Toast.makeText(this, "WiFi is disabled ... We need to enable it", Toast.LENGTH_LONG).show();
//            wifiManager.setWifiEnabled(true);
//        }


        settings = getApplicationContext().getSharedPreferences(SETTINGS_NAME, 0);


        tvCurSSID = findViewById(R.id.curSSID);

        listView = findViewById(R.id.wifiList);

        requestPermissionForLocation();

    }

    private void initApp(boolean permissionLocation){
        if (!permissionLocation){
            System.exit(1);
        }

        //TODO
        //to remove later
        userOptions.getSelectedSSIDs().add("PB11WF5");
        userOptions.getSelectedSSIDs().add("PB11WF6");
        userOptions.getSelectedSSIDs().add("PB11WF7");



        Intent intent = new Intent(this, WifiService.class);
        intent.putExtra("UserOptions", userOptions);

        startService(intent);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    private void updateData(){

        wifiService.saveUserOptions(userOptions);

        curSSID = wifiService.getCurSSID();
        registeredSSIDList = wifiService.getRegisteredSSIDList();
        wifiArrayList = wifiService.getWifiArrayList();

        scanAdapter = new ScanAdapter(this, wifiArrayList, curSSID, registeredSSIDList);

        tvCurSSID.setText(curSSID);
        listView.setAdapter(scanAdapter);

    }




    @Override
    protected void onStop() {
        super.onStop();
        if (serviceBound && (! userOptions.isRunInBackground())) {
            unbindService(this);
            serviceBound = false;
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (updateReceiver == null) {
            updateReceiver = new UpdateReceiver();
        }
        IntentFilter intentFilter = new IntentFilter(ACTION_DATA_REFRESH);
        registerReceiver(updateReceiver, intentFilter);
    }

    @Override
    protected void onPause(){
        super.onPause();
        if (updateReceiver != null) {
            unregisterReceiver(updateReceiver);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
//        LocalWordService.MyBinder b = (LocalWordService.MyBinder) binder;
        WifiService.ServiceBinder b = (WifiService.ServiceBinder) binder;
        wifiService = b.getService();
        Log.d(TAG, "Service connected");
//        updateData();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        wifiService = null;
    }

    private class UpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_DATA_REFRESH)) {
                updateData();
            }
        }
    }



    private void requestPermissionForLocation() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (result == PackageManager.PERMISSION_GRANTED) {
            initApp(true);
            return;
        }
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else {
            // Android version is lesser than 6.0 or the permission is already granted.
            initApp(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            //In order not to crash (Crashlytics incident)
            if (grantResults.length == 0){
                Toast.makeText(this, getResources().getString(R.string.permdenied), Toast.LENGTH_LONG).show();
                initApp(false);
                return;
            }
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // Permission is granted
                initApp(true);
            } else {
                Toast.makeText(this, getResources().getString(R.string.permdenied), Toast.LENGTH_LONG).show();
                initApp(false);
            }
        }
    }


}
