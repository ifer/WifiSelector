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
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;

import static ifer.android.wifiselector.AndroidUtils.showPopupInfo;
import static ifer.android.wifiselector.AndroidUtils.showToastMessage;


public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 100;
    private static final int SETTINGS_REQUEST = 101;
    public static final int JOBID = 1;

    public static final String ACTION_DATA_REFRESH = "DataRefresh";
    public static final String ACTION_WIFI_SELECTION_CHANGED = "wifi_selection_changed";

    public static final String PREF_SSIDS = "sel_ssids";
    public static final String TAG="WifiSelector";
    private final String VERSION_PATTERN = "@version@";

    public static final String CHANNEL_ID = "ForegroundServiceChannel";


    private ListView listView;
    private TextView tvCurSSID;
    private int size = 0;
    private String curSSID;
    private ArrayList<WifiEntry> wifiArrayList;
    private ArrayList<String> registeredSSIDList;
    private ScanAdapter scanAdapter;
    private SharedPreferences settings;
    private HashSet<String> selectedSSIDs;

//    private static JobScheduler jobScheduler;
    private EventReceiver eventReceiver;
    private WifiBoundService wifiBoundService;
    public UpdateReceiver updateReceiver;
//    private WifiScanResultsReceiver wifiScanResultsReceiver;

    private boolean serviceBound = false;
    private boolean permissionGranted = false;


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
                if (serviceBound == true && wifiBoundService != null){
                    showToastMessage(getApplicationContext(), getString(R.string.scanWifiMessage));
                    wifiBoundService.scanWifi();
                }


            }
        });

        settings = getApplicationContext().getSharedPreferences(UserOptions.SETTINGS_NAME, 0);


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

        UserOptions.load();

//        eventReceiver = GlobalApplication.getEventReceiver();
    }

    public  boolean isWifiEnabled() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return (wifiManager.isWifiEnabled());
    }

    // Class representing WifiBoundService through Binding
    public ServiceConnection boundServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            wifiBoundService = ((WifiBoundService.ServiceBinder) binder).getService();
//Log.d(TAG, "Service connected");

            // If bound-only, perform an immediate scan, because of a strange behaviour
            // when switching from runInTheBackground to bound-only
//            if (serviceBound ){
//                wifiBoundService.scanWifi();
//            }

        }

        public void onServiceDisconnected(ComponentName className) {
//Log.d(TAG, "Service disconnected");
            wifiBoundService = null;
        }
    };

//    // Register BroadCastReceiver for events updating when app is off
//    private void registerEventReceiver (){
//        IntentFilter filter = new IntentFilter();
////        filter.addAction(EventReceiver.ACTION_SCAN_WIFI);
//        filter.addAction(Intent.ACTION_SCREEN_ON);
//        filter.addAction(Intent.ACTION_USER_PRESENT);
//        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
//
//        eventReceiver = new EventReceiver();
//        getApplicationContext().registerReceiver(eventReceiver, filter);
//    }


    // Bind WifiBoundService so that the activity can communicate with it.
    // If the service is not started as a background service as well (i.e. LocationService not started),
    // binding terminates along with the app, and the service stops working
    private void bindWifiBoundService(){
        Intent intent = new Intent(this, WifiBoundService.class);

        bindService(intent, boundServiceConnection, Context.BIND_AUTO_CREATE);
        serviceBound = true;


    }

    //Unbind WifiBoundService
    private void unbindWifiBoundService(){
        if(serviceBound) {
            unbindService(boundServiceConnection);
            serviceBound = false;
        }
    }


    // OnStart event: bind the bound service
    @Override
    protected void onStart() {
        super.onStart();
//Log.d(TAG, "activity onStart");

        if (UserOptions.getSelectedSSIDs().size() == 0){
            showToastMessage(this, getResources().getString(R.string.notif_no_ssids_selected));
        }

        if (permissionGranted) {
            bindWifiBoundService();

        }

    }

    //OnResume event (app in foreground):
    // Stop LocationService and unregister the EventReceiver.
    // Create the UpdateReceiver (if not already created).
    // Then register  the receiver so that it receives messages:
    // - From WifiBoundService that data have been refreshed
    // - From ScanAdapter that the list of selected WiFis is changed

    @Override
    protected void onResume() {
        super.onResume();
Log.d(TAG, "activity onResume");
        if (permissionGranted) {
            //Check if wifi is enabled
            if (! isWifiEnabled() || ! LocationService.isLocationEnabled()) {
                showPopupInfo(this, getString(R.string.notif_wifi_or_location_not_enabled),  new FinishPosAction());
            }

            if (GlobalApplication.isEnentReceiverRegistered()) {
                GlobalApplication.unregisterEventReceiver();
            }

            this.stopService(new Intent(this, LocationService.class));


            if (updateReceiver == null) {
                updateReceiver = new UpdateReceiver();
            }
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_DATA_REFRESH);
            intentFilter.addAction(ACTION_WIFI_SELECTION_CHANGED);
            getApplicationContext().registerReceiver(updateReceiver, intentFilter);
        }
    }


    // OnPause event: (app goes to background)
    // Unregister the updateReceiver
    // If UserOptions.isRunInBackground():
    //  - Register the EventReceiver
    //  - Start the LocationService
    @Override
    protected void onPause(){
        super.onPause();
Log.d(TAG, "activity onPause");
        if (updateReceiver != null) {
            getApplicationContext().unregisterReceiver(updateReceiver);
        }

        if (UserOptions.isRunInBackground()) {
            GlobalApplication.registerEventReceiver();


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.startForegroundService(new Intent(this, LocationService.class));
            } else {
                this.startService(new Intent(this, LocationService.class));
            }

        }
    }

    // OnStop event: unbind the WifiBoundService
    @Override
    protected void onStop() {
        super.onStop();
Log.d(TAG, "activity onStop");

        unbindWifiBoundService();

    }

    //Update all data
    private void updateData(Intent intent){


        curSSID = (String) intent.getSerializableExtra("curSSID");
        registeredSSIDList = (ArrayList<String>)intent.getSerializableExtra("registeredSSIDList");
        wifiArrayList = (ArrayList<WifiEntry>)intent.getSerializableExtra("wifiArrayList");

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        boolean runInBackgroundChanged = false;
        if (requestCode == SETTINGS_REQUEST && resultCode == RESULT_OK && data != null) {
            UserOptions.load();
            runInBackgroundChanged = data.getBooleanExtra("runInBackgroundChanged", false);
        }
        if (runInBackgroundChanged && UserOptions.isRunInBackground() == false){
//Log.d(TAG, "Option changed, cancel backgroun updates");
            if (GlobalApplication.isEnentReceiverRegistered()) {
                GlobalApplication.unregisterEventReceiver();
            }

        }
    }


    // The UpdateReceiver class. Handles the two kind of actions specified (see onResume)
    public class UpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_DATA_REFRESH)) {
//Log.d(TAG, "Update data!");
                updateData(intent);
            }
            else if (intent.getAction().equals(ACTION_WIFI_SELECTION_CHANGED)) {
//Log.d(TAG, "WIFI selection changed!");
                String ssid = (String) intent.getSerializableExtra("SSID");
                String action = (String) intent.getSerializableExtra("ACTION");
                if (action.equals("add")){
                    UserOptions.getSelectedSSIDs().add(ssid);
                }
                else {
                    UserOptions.getSelectedSSIDs().remove(ssid);
                }
                UserOptions.save();
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
            permissionGranted = true;
            initApp(true);
            return;
        }
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else {
            // Android version is lesser than 6.0 or the permission is already granted.
            permissionGranted = true;
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
                permissionGranted = true;
                onStart();
                onResume();

                initApp(true);
            } else {
                Toast.makeText(this, getResources().getString(R.string.permdenied), Toast.LENGTH_LONG).show();
                initApp(false);
            }
        }
    }


}
