package ifer.android.wifiselector;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;

public class SettingsActivity extends AppCompatActivity {
    private  UserOptions userOptions;

    private CheckBox chkBackgrnd    ;
    private CheckBox chkAutoconnect ;
    private RadioButton radOne      ;
    private RadioButton radTwo      ;
    private RadioButton radFive     ;
    private RadioButton radFifteen  ;
    private RadioButton radThirty   ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        chkBackgrnd    = findViewById(R.id.checkbox_backgrnd);
        chkAutoconnect = findViewById(R.id.checkbox_autoconnect);
        radOne         = findViewById(R.id.intv_one);
        radTwo         = findViewById(R.id.intv_two);
        radFive        = findViewById(R.id.intv_five);
        radFifteen     = findViewById(R.id.intv_fifteen);
        radThirty      = findViewById(R.id.intv_thirty);

        loadUserOptions();

    }

    private void loadUserOptions(){
        userOptions = UserOptionsHelper.loadUserOptions();
        chkBackgrnd.setChecked(userOptions.isRunInBackground());
        chkAutoconnect.setChecked(userOptions.isAutoConnectToStrongest());
        int intv = userOptions.getAlarmInterval();
        switch (intv){
            case 1: radOne.setChecked(true);
            break;

            case 2: radTwo.setChecked(true);
                break;

            case 5: radFive.setChecked(true);
                break;

            case 15: radFifteen.setChecked(true);
                break;

            case 30: radThirty.setChecked(true);
                break;
        }
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.intv_one:
                if (checked)
                    userOptions.setAlarmInterval(1);
                    break;
            case R.id.intv_two:
                if (checked)
                    userOptions.setAlarmInterval(2);
                    break;
            case R.id.intv_five:
                if (checked)
                    userOptions.setAlarmInterval(5);
                    break;
            case R.id.intv_fifteen:
                if (checked)
                    userOptions.setAlarmInterval(15);
                    break;
            case R.id.intv_thirty:
                if (checked)
                    userOptions.setAlarmInterval(30);
                break;
        }
    }

    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        // Check which checkbox was clicked
        switch(view.getId()) {
            case R.id.checkbox_backgrnd:
                if (checked)
                    userOptions.setRunInBackground(true);
                else
                    userOptions.setRunInBackground(false);
                break;
            case R.id.checkbox_autoconnect:
                if (checked)
                    userOptions.setAutoConnectToStrongest(true);
                else
                    userOptions.setAutoConnectToStrongest(false);
                break;

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

}
