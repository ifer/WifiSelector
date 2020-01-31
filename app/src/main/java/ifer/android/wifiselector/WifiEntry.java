package ifer.android.wifiselector;

import android.support.annotation.NonNull;

public class WifiEntry implements Comparable  {
    private String ssid;
    private String signalLevel;
    private String signalLabel;
    private int signalPercentage;
    private boolean selected = false;
    private boolean registered = false;

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getSignalLevel() {
        return signalLevel;
    }

    public void setSignalLevel(String signalLevel) {
        this.signalLevel = signalLevel;
    }

    public String getSignalLabel() {
        return signalLabel;
    }

    public void setSignalLabel(String signalLabel) {
        this.signalLabel = signalLabel;
    }

    public int getSignalPercentage() {
        return signalPercentage;
    }

    public void setSignalPercentage(int signalPercentage) {
        this.signalPercentage = signalPercentage;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    @Override
    public int compareTo(@NonNull Object o) {
        WifiEntry other = (WifiEntry)o;

        int comparedSignal=((WifiEntry)other).getSignalPercentage();
        /* For Ascending order*/
//        return this.getSignalPercentage() - comparedSignal;

        /* For Descending order  */
        return comparedSignal - this.getSignalPercentage();

    }
}
