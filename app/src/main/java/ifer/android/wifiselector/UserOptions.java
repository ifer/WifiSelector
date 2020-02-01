package ifer.android.wifiselector;

import java.io.Serializable;
import java.util.HashSet;

public class UserOptions implements Serializable {
    private boolean runInBackground = true;
    private int alarmInterval = 1; //minutes
    private boolean autoConnectToStrongest = true;
    private HashSet<String> selectedSSIDs = new HashSet<String>();

    public boolean isRunInBackground() {
        return runInBackground;
    }

    public void setRunInBackground(boolean runInBackground) {
        this.runInBackground = runInBackground;
    }

    public int getAlarmInterval() {
        return alarmInterval;
    }

    public void setAlarmInterval(int alarmInterval) {
        this.alarmInterval = alarmInterval;
    }

    public boolean isAutoConnectToStrongest() {
        return autoConnectToStrongest;
    }

    public void setAutoConnectToStrongest(boolean autoConnectToStrongest) {
        this.autoConnectToStrongest = autoConnectToStrongest;
    }

    public HashSet<String> getSelectedSSIDs() {
        return selectedSSIDs;
    }

    public void setSelectedSSIDs(HashSet<String> selectedSSIDs) {
        this.selectedSSIDs = selectedSSIDs;
    }
}
