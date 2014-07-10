package com.example.battletanks;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity implements SensorEventListener {

	private SensorManager mSensorManager;
	private Sensor mGrav;
	int[] sensorValues = {0,0,0};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mGrav = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
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
