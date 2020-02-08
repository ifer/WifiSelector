package ifer.android.wifiselector;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.Serializable;
import java.util.HashSet;

public class UserOptions implements Serializable {
    public static final String SETTINGS_NAME = "wifi_prefs";

    public static final String OPTION_SSIDS = "opt_ssids";
    public static final String OPTION_BACKGRND = "opt_backgrnd";
    public static final String OPTION_INTERVAL = "opt_interval";
    public static final String OPTION_AUTOCONNECT = "opt_autoconnect";
    public static final String OPTION_SWITCH_DIFF = "opt_switchdiff";


    private static boolean runInBackground = true;
    private static int alarmInterval = 2; //minutes
    private static boolean autoConnectToStrongest = true;
    private static int minSwitchDiff = 10;
    private static HashSet<String> selectedSSIDs = new HashSet<String>();

    private static SharedPreferences settings;
    private static Context context = GlobalApplication.getAppContext();

    public static  void save(){
        settings = context.getSharedPreferences(SETTINGS_NAME, 0);

        SharedPreferences.Editor editor = settings.edit();

        editor.clear();

        editor.putBoolean(OPTION_BACKGRND, isRunInBackground());
        editor.putInt(OPTION_INTERVAL, getAlarmInterval());
        editor.putBoolean(OPTION_AUTOCONNECT, isAutoConnectToStrongest());
        editor.putInt(OPTION_SWITCH_DIFF, getMinSwitchDiff());

        HashSet<String> selectedSSIDs = new HashSet<String>();
        selectedSSIDs.addAll(getSelectedSSIDs());
        editor.putStringSet(OPTION_SSIDS, selectedSSIDs);

        editor.apply();
    }

    public static  void  load(){
        settings = context.getSharedPreferences(SETTINGS_NAME, 0);

        setAlarmInterval(settings.getInt(OPTION_INTERVAL, 2));
        setAutoConnectToStrongest(settings.getBoolean(OPTION_AUTOCONNECT, true));
        setRunInBackground(settings.getBoolean(OPTION_BACKGRND, true));
        setMinSwitchDiff(settings.getInt(OPTION_SWITCH_DIFF, 10));

        HashSet<String> def = new HashSet<String>(); //default values: empty
        HashSet<String> ps = (HashSet<String>)settings.getStringSet(OPTION_SSIDS, def);

        setSelectedSSIDs(ps);


    }

    public static boolean isRunInBackground() {
        return runInBackground;
    }

    public static void setRunInBackground(boolean runInBackground) {
        UserOptions.runInBackground = runInBackground;
    }

    public static int getAlarmInterval() {
        return alarmInterval;
    }

    public static void setAlarmInterval(int alarmInterval) {
        UserOptions.alarmInterval = alarmInterval;
    }

    public static boolean isAutoConnectToStrongest() {
        return autoConnectToStrongest;
    }

    public static void setAutoConnectToStrongest(boolean autoConnectToStrongest) {
        UserOptions.autoConnectToStrongest = autoConnectToStrongest;
    }

    public static int getMinSwitchDiff() {
        return minSwitchDiff;
    }

    public static void setMinSwitchDiff(int minSwitchDiff) {
        UserOptions.minSwitchDiff = minSwitchDiff;
    }

    public static HashSet<String> getSelectedSSIDs() {
        return selectedSSIDs;
    }

    public static void setSelectedSSIDs(HashSet<String> selectedSSIDs) {
        UserOptions.selectedSSIDs = selectedSSIDs;
    }
}
