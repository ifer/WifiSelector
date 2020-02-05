package ifer.android.wifiselector;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class WifiJobService extends JobService {
    private static String TAG = "WifiSelector";

    private WifiSelector wifiSelector;
    private UserOptions userOptions;


    public WifiJobService() {
        wifiSelector = new WifiSelector();
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        userOptions = UserOptionsHelper.loadUserOptions();
        wifiSelector.scanWifi();

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
