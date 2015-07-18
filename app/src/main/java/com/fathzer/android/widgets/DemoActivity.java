package com.fathzer.android.widgets;

import android.app.Activity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.fathzer.android.seekbar.RangeSeekBar;

public class DemoActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        RangeSeekBar rsb = (RangeSeekBar)findViewById(R.id.seekBar);
        setSeekBarText(rsb);
        rsb.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar bar, int minValue, int maxValue) {
                setSeekBarText(bar);
            }
        });
    }

    private void setSeekBarText(RangeSeekBar rsb) {
        TextView txt = (TextView)findViewById(R.id.seekBarValues);
        Log.i("Test", "txt is "+(txt==null?"null":"not null"));
        Log.i("Test", "rsb is "+(rsb==null?"null":"not null"));
        txt.setText("Values: "+rsb.getSelectedMinValue()+" to "+rsb.getSelectedMaxValue()+
                ". Range is from "+rsb.getAbsoluteMinValue()+" to "+rsb.getAbsoluteMaxValue());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_demo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
