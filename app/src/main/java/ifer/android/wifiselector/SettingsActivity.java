package ifer.android.wifiselector;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import static ifer.android.wifiselector.AndroidUtils.*;

public class SettingsActivity extends AppCompatActivity {
//    private  UserOptions userOptions;

    private CheckBox chkBackgrnd    ;
    private CheckBox chkAutoconnect ;
    private RadioButton radOne      ;
    private RadioButton radTwo      ;
    private RadioButton radFive     ;
    private RadioButton radFifteen  ;
    private RadioButton radThirty   ;
    private EditText etSwitchDiff   ;
    private EditText etMinDistance   ;
    private CheckBox chkStopBackgrnd    ;
    private EditText etStopThreshold;
    private TextView tvStopThreshold;
    private Spinner spinInterval;

    private boolean settingsChanged = false;

    private boolean oldRunInBackground;
    private int oldSwitchDiff;
    private int oldMinDistance;
    private boolean oldStopBackground;
    private int oldStopThreshold;
    private int oldAlarmInterval;
//    private String[] intervals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        chkBackgrnd    = findViewById(R.id.checkbox_backgrnd);
        chkAutoconnect = findViewById(R.id.checkbox_autoconnect);
        spinInterval   =  findViewById(R.id.spin_interval);
        etSwitchDiff   = findViewById(R.id.switch_diff);
        etMinDistance  = findViewById(R.id.min_dist);
        chkStopBackgrnd = findViewById(R.id.checkbox_stopbackground);
        etStopThreshold = findViewById(R.id.stop_threshold);
        tvStopThreshold = findViewById(R.id.tv_stop_threshold);


//        spinInterval.setOnItemClickListener(new OnIntervalSelectedListener());

//        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, UserOptions.intervals);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.interval_item, UserOptions.intervals);
        spinnerArrayAdapter.setDropDownViewResource(R.layout.interval_item);
        spinInterval.setAdapter(spinnerArrayAdapter);
        spinInterval.setOnItemSelectedListener(new OnIntervalSelectedListener());

        loadUserOptions();

    }

    private void loadUserOptions(){
        UserOptions.load();

        chkBackgrnd.setChecked(UserOptions.isRunInBackground());
        chkAutoconnect.setChecked(UserOptions.isAutoConnectToStrongest());
        etSwitchDiff.setText(String.valueOf(UserOptions.getMinSwitchDiff()));
        etMinDistance.setText(String.valueOf(UserOptions.getMinDistance()));
        chkStopBackgrnd.setChecked(UserOptions.isStopBackground());
        etStopThreshold.setText(String.valueOf(UserOptions.getStopThreshold()));

        int intpos = getIndexOfInterval(UserOptions.getAlarmInterval());
        spinInterval.setSelection(intpos);


        if (UserOptions.isStopBackground() == true){
            tvStopThreshold.setEnabled(true);
            etStopThreshold.setEnabled(true);
        }
        else {
            tvStopThreshold.setEnabled(false);
            etStopThreshold.setEnabled(false);
        }

        oldRunInBackground = UserOptions.isRunInBackground(); //keep initial value
        oldSwitchDiff = UserOptions.getMinSwitchDiff();
        oldMinDistance = UserOptions.getMinDistance();
        oldStopBackground = UserOptions.isStopBackground();
        oldStopThreshold = UserOptions.getStopThreshold();
        oldAlarmInterval = UserOptions.getAlarmInterval();
    }



    public class OnIntervalSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
//            Toast.makeText(parent.getContext(),
//                    "OnItemSelectedListener : " + parent.getItemAtPosition(pos).toString(),
//                    Toast.LENGTH_SHORT).show();
          String item = (String) parent.getItemAtPosition(pos);
          UserOptions.setAlarmInterval(Integer.parseInt(item));
//          settingsChanged = true;
        }


        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
        }

    }

    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        // Check which checkbox was clicked
        switch(view.getId()) {
            case R.id.checkbox_backgrnd:
                if (checked)
                    UserOptions.setRunInBackground(true);
                else
                    UserOptions.setRunInBackground(false);
                break;
            case R.id.checkbox_autoconnect:
                if (checked)
                    UserOptions.setAutoConnectToStrongest(true);
                else
                    UserOptions.setAutoConnectToStrongest(false);
                break;
            case R.id.checkbox_stopbackground:
                if(checked){
                    UserOptions.setStopBackground(true);
                    tvStopThreshold.setEnabled(true);
                    etStopThreshold.setEnabled(true);
                }
                else {
                    UserOptions.setStopBackground(false);
                    tvStopThreshold.setEnabled(false);
                    etStopThreshold.setEnabled(false);
                }

        }
        settingsChanged = true;
    }

    private boolean validateSwitchDiff (){
        Integer switchDiff = getIntegerFromTextView(etSwitchDiff);
        if (switchDiff == null || switchDiff < 0 || switchDiff > 100){
            showToastMessage(this, getString(R.string.error_switch_diff));
            return (false);
        }
        return (true);
    }
    private boolean validateMinDistance (){
        Integer minDist = getIntegerFromTextView(etMinDistance);
        if (minDist == null || minDist < 0 ){
            showToastMessage(this, getString(R.string.error_min_dist));
            return (false);
        }
        return (true);
    }
    private boolean validateStopThreshold(){
        Integer stopthres = getIntegerFromTextView(etStopThreshold);
        if (stopthres == null || stopthres < 5){
            showToastMessage(this, getString(R.string.error_stop_threshold));
            return (false);
        }
        return(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (item.getItemId()) {
            case R.id.action_settings_save:
                if (validateSwitchDiff() == false){
                    return true;
                }
                if (validateMinDistance() == false){
                    return true;
                }

                if(UserOptions.isStopBackground() == true){
                    if (validateStopThreshold() == false){
                        return true;
                    }
                }

                UserOptions.setMinSwitchDiff( getIntegerFromTextView(etSwitchDiff));
                UserOptions.setMinDistance(getIntegerFromTextView(etMinDistance));
                UserOptions.setStopThreshold(getIntegerFromTextView(etStopThreshold));

                UserOptions.save();

                //Notify main activity if runInBackground has changed
                boolean runInBackgroundChanged = false;
                if (UserOptions.isRunInBackground() != oldRunInBackground){
                    runInBackgroundChanged = true;
                }
                Intent output = new Intent();
                output.putExtra("runInBackgroundChanged", runInBackgroundChanged);
                setResult(RESULT_OK, output);

                finish();
                return true;

            case R.id.action_settings_cancel:
                cancelEdit ();
                return true;

            case android.R.id.home:    //make toolbar home button behave like cancel
                cancelEdit();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void cancelEdit (){
        if (! getIntegerFromTextView(etSwitchDiff).equals(oldSwitchDiff))    {
            settingsChanged = true;
        }
        if (! getIntegerFromTextView(etMinDistance).equals(oldMinDistance))    {
            settingsChanged = true;
        }
        if (! getIntegerFromTextView(etStopThreshold).equals(oldStopThreshold))    {
            settingsChanged = true;
        }
        if ( UserOptions.getAlarmInterval() != oldAlarmInterval){
            settingsChanged = true;
        }

        if (settingsChanged){
            showPopup(this, Popup.WARNING, getString(R.string.warn_not_saved),  new CancelPosAction(), new CancelNegAction());
        }
        else {                                          //Data not changed
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    class CancelPosAction implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            finish();
        }
    }

    class CancelNegAction implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
        }
    }

    private int getIndexOfInterval(Integer intv){
        for(int i=0; i<UserOptions.intervals.length; i++){
            int interval = Integer.parseInt(UserOptions.intervals[i]);
            if (intv == interval){
                return (i);
            }
        }
        return (-1);
    }


}
