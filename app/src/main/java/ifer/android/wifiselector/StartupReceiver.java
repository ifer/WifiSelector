package ifer.android.wifiselector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartupReceiver extends BroadcastReceiver {
    private static String TAG = "WifiSelector";

    @Override
    public void onReceive(Context context, Intent intent) {
//        UserOptions userOptions = new UserOptions();

//        userOptions.getSelectedSSIDs().add("PB11WF5");
//        userOptions.getSelectedSSIDs().add("PB11WF6");
//        userOptions.getSelectedSSIDs().add("PB11WF7");

//        Log.d(TAG, "TRYING TO START WifiBoundService..");
        Intent service = new Intent(context, WifiBoundService.class);
//        service.putExtra("UserOptions", userOptions);
        context.startService(service);
    }
}