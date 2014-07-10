package com.example.battletanks;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends Activity implements SensorEventListener {

	private SensorManager mSensorManager;
	private Sensor mGrav;
	int[] sensorValues = {0,0,0};
    private Button On, Off, Visible, list;
    private BluetoothAdapter BA;
    private Set<BluetoothDevice> pairedDevices;
    private ListView lv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mGrav = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        On = (Button) findViewById(R.id.button1);
        Off = (Button) findViewById(R.id.button2);
        Visible = (Button) findViewById(R.id.button3);
        list = (Button) findViewById(R.id.button4);

        lv = (ListView) findViewById(R.id.listView1);

        BA = BluetoothAdapter.getDefaultAdapter();
    }

    public void on(View view) {

        if (!BA.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on"
                    , Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Already on",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void list(View view) {

        pairedDevices = BA.getBondedDevices();

        ArrayList list = new ArrayList();
        for (BluetoothDevice bt : pairedDevices)
            list.add(bt.getName());

        Toast.makeText(getApplicationContext(), "Showing Paired Devices",
                Toast.LENGTH_SHORT).show();
        final ArrayAdapter adapter = new ArrayAdapter
                (this, android.R.layout.simple_list_item_1, list);
        lv.setAdapter(adapter);

    }

    public void off(View view) {

        BA.disable();
        Toast.makeText(getApplicationContext(), "Turned off",
                Toast.LENGTH_LONG).show();
    }

    public void visible(View view) {

        Intent getVisible = new Intent(BluetoothAdapter.
                ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(getVisible, 0);

    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do something here if sensor accuracy changes.
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// The light sensor returns a single value.
		// Many sensors return 3 values, one for each axis.
		// if(event.sensor.getType() == Sensor.TYPE_GRAVITY){

		int x = (int) event.values[0];
		int y = (int) event.values[1];
		int z = (int) event.values[2];
		
		if(x!=sensorValues[0]||y!=sensorValues[1]||z!=sensorValues[2]) {
			Log.i("Test", "X: " + x + "; Y: " + y + "; Z: " + z);
			sensorValues[0] = x;
			sensorValues[1] = y;
			sensorValues[2] = z;
			
			//do something with bluetooth     
		}
	
		// Do something with this sensor value.
		
		// }
	}

	@Override
	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, mGrav,
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}

}
