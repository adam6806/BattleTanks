package com.example.battletanks;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity implements SensorEventListener
{

  private SensorManager mSensorManager;
  private Sensor mGrav;
  int[] sensorValues = { 0, 0, 0 };
  String command = "0";
  private Button On, Off, Visible, list;
  private BluetoothAdapter BA;
  private BluetoothSocket btSocket = null;
  private OutputStream outStream = null;
  private Set<BluetoothDevice> pairedDevices;
  private ListView lv;
  private static final UUID MY_UUID = UUID
      .fromString("00001101-0000-1000-8000-00805F9B34FB");
  private static String address = "00:06:66:68:39:6E";

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
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

  public void on(View view)
  {

    Log.i("Test", "On button");

    if (!BA.isEnabled())
    {
      Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(turnOn, 0);
      Toast.makeText(getApplicationContext(), "Turned on", Toast.LENGTH_LONG)
          .show();
    }
    else
    {
      Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG)
          .show();
    }
  }

  public void list(View view)
  {

    Log.i("Test", "list button");

    pairedDevices = BA.getBondedDevices();

    ArrayList list = new ArrayList();
    for (BluetoothDevice bt : pairedDevices)
      list.add(bt.getName());

    Toast.makeText(getApplicationContext(), "Showing Paired Devices",
        Toast.LENGTH_SHORT).show();
    final ArrayAdapter adapter = new ArrayAdapter(this,
        android.R.layout.simple_list_item_1, list);
    lv.setAdapter(adapter);

  }

  public void off(View view)
  {

    Log.i("Test", "Off buttons");

    BA.disable();
    Toast.makeText(getApplicationContext(), "Turned off", Toast.LENGTH_LONG)
        .show();
  }

  public void visible(View view)
  {

    Log.i("Test", "visible button");

    Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
    startActivityForResult(getVisible, 0);

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    if (id == R.id.action_settings)
    {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy)
  {
    // Do something here if sensor accuracy changes.
  }

  @Override
  public void onSensorChanged(SensorEvent event)
  {
    // The light sensor returns a single value.
    // Many sensors return 3 values, one for each axis.
    // if (event.sensor.getType() == Sensor.TYPE_GRAVITY)
    // {
    int x = (int) (event.values[0] + 10f);
    int y = (int) (event.values[1] + 10f);
    int z = (int) (event.values[2] + 10f);

    if (y != sensorValues[1] || z != sensorValues[2])
    {
      Log.i("Test", "X: " + x + "; Y: " + y + "; Z: " + z);

      sensorValues[0] = x;
      sensorValues[1] = y;
      sensorValues[2] = z;

      String newCommand;
      // Adam is excited to come out of closet
      // don't be afraid to call boys ;)
      if (z >= 17)
      {
        if (y >= 11)
        {
          newCommand = "5";
          Log.d("Send", "Z: " + z + "; Y: " + y);
        }
        else if (y <= 6)
        {
          newCommand = "4";
          Log.d("Send", "Z: " + z + "; Y: " + y);
        }
        else
        {
          newCommand = "1";
          Log.d("Send", "Z: " + z + "; Y: " + y);
        }
      }
      else if (z <= 11)
      {
        newCommand = "2";
        Log.d("Send", "Z: " + z + "; Y: " + y);
      }
      else if (y >= 11)
      {
        newCommand = "6";
        Log.d("Send", "Z: " + z + "; Y: " + y);
      }
      else if (y <= 6)
      {
        newCommand = "3";
        Log.d("Send", "Z: " + z + "; Y: " + y);
      }
      else
      {
        newCommand = "0";
        Log.d("Send", "Z: " + z + "; Y: " + y);
      }
      
      if(!newCommand.equalsIgnoreCase(command)) {
        sendData(newCommand);
        command = newCommand;        
      }
      
      // sendData(String.valueOf(z) + "*");

      // do something with bluetooth
    }

    // Do something with this sensor value.

    // }
  }

  @Override
  protected void onResume()
  {
    super.onResume();
    mSensorManager.registerListener(this, mGrav,
        SensorManager.SENSOR_DELAY_NORMAL);

    // Set up a pointer to the remote node using it's address.
    BluetoothDevice device = BA.getRemoteDevice(address);

    // Two things are needed to make a connection:
    // A MAC address, which we got above.
    // A Service ID or UUID. In this case we are using the
    // UUID for SPP.
    try
    {
      btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
      Log.d("Cnct", "uuid");
    } catch (IOException e)
    {
      Log.d("Cnct", "e");
    }

    // Discovery is resource intensive. Make sure it isn't going on
    // when you attempt to connect and pass your message.
    BA.cancelDiscovery();

    // Establish the connection. This will block until it connects.
    Log.d("Cnct", "...Connecting to Remote...");
    try
    {
      btSocket.connect();
      Log.d("Cnct", "...Connection established and data link opened...");
    } catch (IOException e)
    {
      try
      {
        btSocket.close();
      } catch (IOException e2)
      {
        Log.d("Cnct", "e2!");
      }
    }

    // Create a data stream so we can talk to server.
    Log.d("Cnct", "...Creating Socket...");

    try
    {
      outStream = btSocket.getOutputStream();
    } catch (IOException e)
    {
      Log.d("Cnct", "e3");
    }
  }

  @Override
  protected void onPause()
  {
    super.onPause();
    mSensorManager.unregisterListener(this);

    Log.d("Paused", "...In onPause()...");

    if (outStream != null)
    {
      try
      {
        outStream.flush();
      } catch (IOException e)
      {

      }
    }

    try
    {
      btSocket.close();
    } catch (IOException e2)
    {
    }
  }

  private void sendData(String message)
  {
    byte[] msgBuffer = message.getBytes();

    Log.d("Send", "...Sending data: " + message + "...");

    try
    {
      outStream.write(msgBuffer);
    } catch (IOException e)
    {
      String msg = "In onResume() and an exception occurred during write: "
          + e.getMessage();
      if (address.equals("00:00:00:00:00:00"))
        msg = msg
            + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 37 in the java code";
      msg = msg + ".\n\nCheck that the SPP UUID: " + MY_UUID.toString()
          + " exists on server.\n\n";
    }
  }

}
