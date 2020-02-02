package ifer.android.wifiselector;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;

public class UserOptionsHelper {
    public static final String SETTINGS_NAME = "wifi_prefs";

    public static final String OPTION_SSIDS = "opt_ssids";
    public static final String OPTION_BACKGRND = "opt_backgrnd";
    public static final String OPTION_INTERVAL = "opt_interval";
    public static final String OPTION_AUTOCONNECT = "opt_autoconnect";

    private static SharedPreferences settings;
    private static Context context = GlobalApplication.getAppContext();


    public static void saveUserOptions(UserOptions userOptions, ArrayList<WifiEntry> wifiArrayList){
        settings = context.getSharedPreferences(SETTINGS_NAME, 0);

        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(OPTION_BACKGRND, userOptions.isRunInBackground());
        editor.putInt(OPTION_INTERVAL, userOptions.getAlarmInterval());
        editor.putBoolean(OPTION_AUTOCONNECT, userOptions.isAutoConnectToStrongest());

        for (WifiEntry wifi : wifiArrayList){
            if (wifi.isSelected()) {
                userOptions.getSelectedSSIDs().add(wifi.getSsid());
            }
        }
        editor.putStringSet(OPTION_SSIDS, userOptions.getSelectedSSIDs());
        editor.apply();
    }

    public static UserOptions loadUserOptions(){
        settings = context.getSharedPreferences(SETTINGS_NAME, 0);

        UserOptions userOptions = new UserOptions();
        userOptions.setAlarmInterval(settings.getInt(OPTION_INTERVAL, 2));
        userOptions.setAutoConnectToStrongest(settings.getBoolean(OPTION_AUTOCONNECT, true));
        userOptions.setRunInBackground(settings.getBoolean(OPTION_BACKGRND, true));

        HashSet<String> def = new HashSet<String>(); //default values: empty
        HashSet<String> ps = (HashSet<String>)settings.getStringSet(OPTION_SSIDS, def);

        userOptions.setSelectedSSIDs(ps);

        return(userOptions);
    }

}
