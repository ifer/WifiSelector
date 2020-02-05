package ifer.android.wifiselector;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
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

import static ifer.android.wifiselector.AndroidUtils.*;


public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 100;
    private static final int SETTINGS_REQUEST = 101;

    public static final String ACTION_DATA_REFRESH = "DataRefresh";
    public static final String ACTION_WIFI_SELECTION_CHANGED = "wifi_selection_changed";

    public static final String PREF_SSIDS = "sel_ssids";
    public static final String TAG="WifiSelector";
    private final String VERSION_PATTERN = "@version@";


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

    private UserOptions userOptions = new UserOptions();

    private WifiService wifiService;
    public UpdateReceiver updateReceiver;
//    private BoundService mBoundService;
    private boolean serviceBound = false;
    private boolean serviceStarted = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


//Log.d(TAG, "activity onCreate");
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                if (serviceBound == true && wifiService != null){
                    wifiService.scanWifi();
                }

            }
        });

        settings = getApplicationContext().getSharedPreferences(UserOptionsHelper.SETTINGS_NAME, 0);


        tvCurSSID = findViewById(R.id.curSSID);

        listView = findViewById(R.id.wifiList);

        requestPermissionForLocation();

    }

    private void initApp(boolean permissionLocation){
        //Ask user permission for location
        if (!permissionLocation){
            System.exit(1);
        }
//Log.d(TAG, "activity initApp");

        userOptions = UserOptionsHelper.loadUserOptions();

        //Check if wifi is enabled
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
           showPopupInfo(this, getString(R.string.wifi_not_enabled),  new FinishPosAction());
        }


    }

    // Class representing WifiService through Binding
    public ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            wifiService = ((WifiService.ServiceBinder) binder).getService();
//Log.d(TAG, "Service connected");

            // If bound-only, perform an immediate scan, because of a strange behaviour
            // when switching from runInTheBackground to bound-only
            if (serviceBound && (! serviceStarted)){
                wifiService.scanWifi();
            }

        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "Service disconnected");
            wifiService = null;
        }
    };


    // Start WifiService as a permanent background service.
    // This must happen before binding so that even if the app is terminated,
    // the service continues working.
    private void startWifiService(){
        Intent intent = new Intent(this, WifiService.class);
        intent.putExtra("UserOptions", userOptions);
        startService(intent);
        serviceStarted = true;
    }


    // Stop WifiService as a permanent background service.
    private void stopWifiService(){
        Intent intent = new Intent(this, WifiService.class);
        stopService(intent);
        serviceStarted = false;
    }

    // Bind WifiService so that the activity can communicate with it.
    // If the service is not started as a background service as well,
    // binding terminates along with the app, and the service stops working
    private void bindWifiService(){
        Intent intent = new Intent(this, WifiService.class);
        intent.putExtra("UserOptions", userOptions);

        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        serviceBound = true;
    }

    //Unbind WifiService
    private void unbindWifiService(){
        unbindService(serviceConnection);
        serviceBound = false;
    }


    // OnStart event: start service in the background if option isRunInBackground is true.
    // Then bind the service.
    @Override
    protected void onStart() {
        super.onStart();
//Log.d(TAG, "activity onStart");

        if (userOptions.isRunInBackground()) {
            startWifiService();
        }

        bindWifiService();


    }

    //OnResume event: create the UpdateReceiver (if not already created).
    //Then register  the receiver for that it receives messages:
    // - From WifiService that data have been refreshed
    // - From ScanAdapter that the list of selected WiFis is changed

    @Override
    protected void onResume(){
        super.onResume();
        if (updateReceiver == null) {
            updateReceiver = new UpdateReceiver();
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_DATA_REFRESH);
        intentFilter.addAction(ACTION_WIFI_SELECTION_CHANGED);
        getApplicationContext().registerReceiver(updateReceiver, intentFilter);
    }

    // OnPause event: unregister the receiver
    @Override
    protected void onPause(){
        super.onPause();
//Log.d(TAG, "activity onPause");
        if (updateReceiver != null) {
            getApplicationContext().unregisterReceiver(updateReceiver);
        }
    }

    // OnStop event: unbint the WifiService

    @Override
    protected void onStop() {
        super.onStop();
//Log.d(TAG, "activity onStop");

        unbindWifiService();
    }

    //Update all data
    private void updateData(){

        curSSID = wifiService.getCurSSID();
        registeredSSIDList = wifiService.getRegisteredSSIDList();
        wifiArrayList = wifiService.getWifiArrayList();

        scanAdapter = new ScanAdapter(this, wifiArrayList, curSSID, registeredSSIDList);

        tvCurSSID.setText(curSSID);
        listView.setAdapter(scanAdapter);

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    // Activity menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, SETTINGS_REQUEST);
            return true;
        }
        else if (id == R.id.action_about) {
            showAbout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // If user changed the option "run as a background service,
    // we need to restart the application so that WifiService starts properly
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        boolean runInBackgroundChanged = false;
        if (requestCode == SETTINGS_REQUEST && resultCode == RESULT_OK && data != null) {
            userOptions = UserOptionsHelper.loadUserOptions();
            runInBackgroundChanged = data.getBooleanExtra("runInBackgroundChanged", false);
        }
        if (runInBackgroundChanged){
            if (userOptions.isRunInBackground()){
                startWifiService();
            }
            else {
               stopWifiService();
            }
            showPopupInfo(this, getString(R.string.warn_app_will_restart),  new RestartPosAction());

        }
    }


    // The UpdateReceiver class. Handles the two kind of actions specified (see onResume)
    public class UpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_DATA_REFRESH)) {
//Log.d(TAG, "Update data!");
                updateData();
            }
            else if (intent.getAction().equals(ACTION_WIFI_SELECTION_CHANGED)) {
//Log.d(TAG, "WIFI selection changed!");
                String ssid = (String) intent.getSerializableExtra("SSID");
                String action = (String) intent.getSerializableExtra("ACTION");
                if (action.equals("add")){
                    userOptions.getSelectedSSIDs().add(ssid);
                }
                else {
                    userOptions.getSelectedSSIDs().remove(ssid);
                }
                UserOptionsHelper.saveUserOptions(userOptions, null);
            }

        }
    }

    // Listener for restarting app popup
    class RestartPosAction implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }
    }

    // Listener for enabling wifi popup
    class FinishPosAction implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            Intent intent = getIntent();
            finish();
        }
    }

    public void showAbout (){
        String version = null;
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        }
        catch (PackageManager.NameNotFoundException e){
            Log.d(TAG, e.getLocalizedMessage());
        }
        if (version == null)
            version = getResources().getString(R.string.version_uknown);

        String text = getResources().getString(R.string.text_about);
        text = text.replace(VERSION_PATTERN, version);

//        AndroidUtils.showPopupInfo(this, text);
        AndroidUtils.showPopup(this, AndroidUtils.Popup.INFO, getString(R.string.action_about), text, null, null);
    }


    // Google methods to ask user permissions
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
