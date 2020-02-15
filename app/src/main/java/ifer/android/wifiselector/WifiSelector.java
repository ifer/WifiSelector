package ifer.android.wifiselector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WifiSelector {
    private static String TAG = "WifiSelector";

    private final int MIN_WIFI_LEVEL = -80; //dBm

    private WifiManager wifiManager;
    private String curSSID;
    private List<ScanResult> results;
    private ArrayList<WifiEntry> wifiArrayList = new ArrayList<WifiEntry>();
    private ArrayList<String> registeredSSIDList = new ArrayList<String>();
    private WifiEntry lastWifiConnected;
    private SharedPreferences settings;

    private Context context;

    public WifiSelector(){
        this.context = GlobalApplication.getAppContext();
        wifiManager = (WifiManager) context.getSystemService(this.context.WIFI_SERVICE);
        settings = context.getSharedPreferences(UserOptions.SETTINGS_NAME, 0);

        UserOptions.load();

    }

    public void scanWifi() {
Log.d(MainActivity.TAG, "scanWifi!");


        // Scan only if the device is already connected to an access point
        if (checkWifiOnAndConnected() == false){
Log.d(MainActivity.TAG, "Wifi disabled or not connected. Doing nothing.");
            return;
        }

        WifiScanResultsReceiver wifiScanResultsReceiver = GlobalApplication.getWifiScanResultsReceiver();
        if (! GlobalApplication.isReceiverRegistered()) {
//Log.d(MainActivity.TAG, "registering registerWificanResultsReceiver");
            GlobalApplication.registerWificanResultsReceiver();
        }

        wifiManager.startScan();
    }



    public void processScanResults ( List<ScanResult> results){

        wifiArrayList.clear();
        curSSID = getWifiSSID(this.context);

        getRegisteredSSIDs();

        if (lastWifiConnected != null)
            lastWifiConnected.setSignalPercentage(0); // Set to zero for the case that it is not detected at all this time

        for (ScanResult scanResult : results) {
            int percentage = WifiManager.calculateSignalLevel(scanResult.level, 100);

//String connectable = (scanResult.level < MIN_WIFI_LEVEL) ? "Not connectable" : "Connectable";
//Log.d(TAG, "scanResult.level=" + scanResult.level  + ", percentage=" + percentage + " " + connectable) ;


            WifiEntry wfe = new WifiEntry();

            if (scanResult.SSID == null || scanResult.SSID.trim().equals("")) {
                scanResult.SSID = "<SSID undefined>";
            }
            wfe.setSsid(scanResult.SSID);
            wfe.setSignalLevel(scanResult.level);
            wfe.setSignalPercentage(percentage);
            wfe.setSignalLabel(String.valueOf(percentage) + "%");

            if(registeredSSIDList.contains(scanResult.SSID)){
                wfe.setRegistered(true);
            }
            if (UserOptions.getSelectedSSIDs().contains(scanResult.SSID)){
//                    Log.d(MainActivity.TAG, "selected: " + scanResult.SSID);
                wfe.setSelected(true);
            }

            // Update lastWifiConnected CURRENT signal strength so that comparison can be done in chooseWifiToConnect
            if(lastWifiConnected != null && wfe.getSsid().equals(lastWifiConnected.getSsid())){
                lastWifiConnected.setSignalPercentage(wfe.getSignalPercentage());
            }

            wifiArrayList.add(wfe);

//                scanAdapter.notifyDataSetChanged();
        }
        Collections.sort(wifiArrayList);

//        Log.d(TAG, "wifiArrayList.size="+wifiArrayList.size());
        if (UserOptions.isAutoConnectToStrongest() && wifiArrayList.size() > 0){
            String chosenSSID = chooseWifiToConnect();

            if (chosenSSID != null) { // SSIDs selected
                Log.d(TAG, "chosenSSID=" + chosenSSID);

                connectToWifiSSID(context, chosenSSID);
            }

//                connectToWifiSSID(context, wifiArrayList.get(0).getSsid());
        }

        curSSID = getWifiSSID(GlobalApplication.getAppContext());

        Intent updateIntent = new Intent();
        updateIntent.setAction(MainActivity.ACTION_DATA_REFRESH);
        updateIntent.putExtra("wifiArrayList", wifiArrayList);
        updateIntent.putExtra("curSSID", curSSID);
        updateIntent.putExtra("registeredSSIDList", registeredSSIDList);


        context.sendBroadcast(updateIntent);

    }

    // Choose to which wifi AP to connect, according to the strength percentage.
    // If the difference between the strongest now and the one already connected (lastWifiConnected)
    // is less than UserOptions.OPTION_SWITCH_DIFF, stay to the lastWifiConnected
    private String chooseWifiToConnect(){
        WifiEntry weChosen = null;

        // Bypass all entries which are not either registered or selected
        for (WifiEntry we : wifiArrayList){
            if((! registeredSSIDList.contains(we.getSsid())) ||
               (! UserOptions.getSelectedSSIDs().contains(we.getSsid())) ){
                continue;
            }
            weChosen = we;
//Log.d(TAG, "1. weChosen=" + weChosen.getSsid());
            break;
        }

        // Do not try to connect to a too weak signal
        if (weChosen.getSignalLevel() < MIN_WIFI_LEVEL){
            return (null);
        }

        // 1st time use: there are no selected wifis to connect
        if (weChosen == null){
            return null;
        }


        if (lastWifiConnected == null ||
                (! registeredSSIDList.contains(lastWifiConnected.getSsid())) ||
                (! UserOptions.getSelectedSSIDs().contains(lastWifiConnected.getSsid()))){

            lastWifiConnected = weChosen;
            return (weChosen.getSsid());
        }

        if (lastWifiConnected.getSsid().equals(weChosen.getSsid())){
            return (lastWifiConnected.getSsid());
        }
        else {
            if ((weChosen.getSignalPercentage() - lastWifiConnected.getSignalPercentage()) < UserOptions.getMinSwitchDiff()){
//String msg = String.format("chosen perc = %d, last perc = %d, minDif=%d", weChosen.getSignalPercentage(), lastWifiConnected.getSignalPercentage(), UserOptions.getMinSwitchDiff());
//Log.d(TAG, msg );
                return (lastWifiConnected.getSsid());
            }
            else {
                lastWifiConnected = weChosen;
                return weChosen.getSsid();
            }
        }



    }

    // Sometimes current ssid is returned as null
    // So try the two methods for three times each
    public String getWifiSSID (Context context){
        String ssid = "";
        for (int i=0; i<3; i++){
            ssid = getWifiSSIDMethod1 (context);
            if (! TextUtils.isEmpty(ssid)){
                return (ssid);
            }

            ssid = getWifiSSIDMethod2 (context);
            if (! TextUtils.isEmpty(ssid)){
                return (ssid);
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return ("");

    }

    public String getWifiSSIDMethod2 (Context context) {
        WifiManager manager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (manager.isWifiEnabled()) {
            WifiInfo wifiInfo = manager.getConnectionInfo();
            if (wifiInfo != null) {
                NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
                if (state == NetworkInfo.DetailedState.CONNECTED || state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                    return wifiInfo.getSSID().replaceAll("\"", "");
                }
            }
        }
        return "";
    }

    public static String getWifiSSIDMethod1 (Context context) {
        if (context == null) {
            return "";
        }
        final Intent intent = context.registerReceiver(null, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
        if (intent != null) {
            final WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            if (wifiInfo != null) {
                final String ssid = wifiInfo.getSSID().replaceAll("\"", "");
//Log.d(TAG, "ssid=" + ssid);
                if (ssid != null) {
                    return ssid;
                }
            }
        }
        return "";
    }


    private void connectToWifiSSID(Context context, String ssid) {
        if (curSSID != null && curSSID.equals(ssid)){
            return;
        }
Log.d(TAG, "Connecting to " + ssid);
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        if (list == null)
            return;
        for (WifiConfiguration i : list) {
             if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
// Log.d(MainActivity.TAG, "i.SSID=" + i.SSID + " ssid=" + ssid + " networkId=" + i.networkId);
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                wifiManager.reconnect();

                curSSID = ssid.replaceAll("\"", "");


            }
        }
    }

    private void getRegisteredSSIDs (){
        registeredSSIDList.clear();
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        if (list == null)
            return;
        for (WifiConfiguration i : list) {
            if (i.SSID != null ) {
                registeredSSIDList.add(i.SSID.replaceAll("\"", ""));
            }
        }
    }

    public boolean checkWifiOnAndConnected() {

        if (wifiManager.isWifiEnabled()) { // Wi-Fi adapter is ON

            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            if( wifiInfo.getNetworkId() == -1 ){
                return false; // Not connected to an access point
            }
            return true; // Connected to an access point
        }
        else {
            return false; // Wi-Fi adapter is OFF
        }
    }

    public String getCurSSID() {
        return curSSID;
    }

    public ArrayList<WifiEntry> getWifiArrayList() {
        return wifiArrayList;
    }

    public ArrayList<String> getRegisteredSSIDList() {
        return registeredSSIDList;
    }

}
