package ifer.android.wifiselector;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.util.Log;

public class WifiJobService extends JobService {
    private static String TAG = "WifiSelector";

    private WifiSelector wifiSelector;



    public WifiJobService() {
        wifiSelector = new WifiSelector();
    }

    @Override
    public boolean onStartJob(JobParameters params) {
Log.d(TAG, "Job service triggers scanWifi()");

        wifiSelector.scanWifi();

        //Reschedule
//        MainActivity.schedulePeriodicJob();

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }


}
