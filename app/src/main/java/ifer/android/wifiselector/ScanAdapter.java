package ifer.android.wifiselector;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import ifer.android.wifiselector.R;

public class ScanAdapter extends BaseAdapter {
    private static String LOG_TAG = "WifiSelector";
    private final int POSITION_KEY = 1;
    private Context context;
    public static ArrayList<WifiEntry> wifiArrayList;
    private String currentSSID;
    private String defaultTextColor;
    private ArrayList<String> registeredSSIDList;


    public ScanAdapter(Context context, ArrayList<WifiEntry> wifiArrayList, String currentSSID, ArrayList<String> registeredSSIDList) {
        this.context = context;
        this.wifiArrayList = wifiArrayList;
        this.currentSSID = currentSSID;
        this.registeredSSIDList = registeredSSIDList;
    }

    @Override
    public int getCount() {
        return wifiArrayList.size();
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        if (convertView == null) {
            holder = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.wifi_item, null, true);

            holder.selected = (CheckBox) convertView.findViewById(R.id.selected);
            holder.ssid = (TextView) convertView.findViewById(R.id.ssid);
            holder.signal = (TextView) convertView.findViewById(R.id.signal);

//            defaultTextColor = holder.ssid.getCurrentTextColor();

            convertView.setTag(holder);
        }else {
            // the getTag returns the viewHolder object set as a tag to the view
            holder = (ViewHolder)convertView.getTag();
        }

        String ssid = wifiArrayList.get(position).getSsid();
        holder.ssid.setText(wifiArrayList.get(position).getSsid());

        boolean registered = wifiArrayList.get(position).isRegistered();

        if (registered){
            holder.ssid.setTypeface(Typeface.DEFAULT_BOLD);
        }
        else {
            holder.ssid.setTypeface(Typeface.DEFAULT);
        }

        if(ssid.equals(this.currentSSID)){
            holder.ssid.setTextColor(Color.parseColor("#0000FF")); //Blue
        }
        else {
            holder.ssid.setTextColor(Color.parseColor("#000000")); //Black
        }
        holder.signal.setText(wifiArrayList.get(position).getSignalLabel());
        holder.selected.setChecked(wifiArrayList.get(position).isSelected());

        holder.selected.setTag(position);

        holder.selected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int pos = (int) holder.selected.getTag();

                if (wifiArrayList.get(pos).isSelected()) {
                    wifiArrayList.get(pos).setSelected(false);
                    notifyMainActivity(wifiArrayList.get(pos), -1);

                } else {
                    if ( !registeredSSIDList.contains(wifiArrayList.get(pos).getSsid())){
                        Toast.makeText(context, context.getResources().getString(R.string.error_unregistered_ssid), Toast.LENGTH_LONG).show();
                        holder.selected.setChecked(false);
                        return;
                    }
                    wifiArrayList.get(pos).setSelected(true);
                    notifyMainActivity(wifiArrayList.get(pos), 1);
                }

            }
        });

        return convertView;
    }

    private void notifyMainActivity(WifiEntry wfe, int action){
        Context context = GlobalApplication.getAppContext();
        Intent intent = new Intent(MainActivity.ACTION_WIFI_SELECTION_CHANGED);
        intent.putExtra("SSID",wfe.getSsid());

        if (action == 1) { //added
            intent.putExtra("ACTION","add");
        }
        else {            //removed
            intent.putExtra("ACTION","removed");
        }
        context.sendBroadcast(intent);
    }

    private class ViewHolder {

        protected CheckBox selected;
        private TextView ssid;
        private TextView signal;

    }

    public void setcurrentSSID(String currentSSID) {
        this.currentSSID = currentSSID;
    }

    public void setRegisteredSSIDList(ArrayList<String> registeredSSIDList) {
        this.registeredSSIDList = registeredSSIDList;
    }
}