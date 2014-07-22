package com.example.battletanks;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity implements SensorEventListener
{

  private static final UUID MY_UUID = UUID
      .fromString("00001101-0000-1000-8000-00805F9B34FB");
  int[] sensorValues = { 0, 0, 0 };
  String command = "0";
  private SensorManager mSensorManager;
  private Sensor mGrav;
  private TextView dName;
  private TextView dAddr;
  private TextView instructions;
  private ImageView commandImage;
  private OutputStream outStream;
  private String address;
  private String deviceName;
  private BluetoothAdapter myBluetoothAdapter;
  private BluetoothSocket btSocket;
  private HashMap<String, String> deviceMap;
  private ArrayAdapter<String> BTArrayAdapter;
  private boolean connected = false;
  private BroadcastReceiver bReceiver;
  private Set<BluetoothDevice> pairedDevices;
  private boolean sendFlag;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    // Prevent screen from turning off
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    // Get UI elements
    dName = (TextView) findViewById(R.id.deviceName);
    dAddr = (TextView) findViewById(R.id.deviceAddress);
    instructions = (TextView) findViewById(R.id.instructionText);
    commandImage = (ImageView) findViewById(R.id.imageView);

    // Setup gravity sensor
    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    mGrav = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

    // Check if the device has a gravity sensor
    if (mGrav == null)
    {
      Toast.makeText(getApplicationContext(),
          "Your device does not support Gravity sensor!!", Toast.LENGTH_LONG)
          .show();
    }
    
 // Get the devices bluetooth adapter
    myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    // Check if the device has a gravity sensor
    if (myBluetoothAdapter == null)
    {
      Toast.makeText(getApplicationContext(),
          "Your device does not support Bluetooth", Toast.LENGTH_LONG).show();
    }

  }
  
  public void setSendFlag(View view)
  {
    // Is the toggle on?
    sendFlag = ((ToggleButton) view).isChecked();
    
  }

  public void connect(View view)
  {
    // Is the toggle on?
    boolean on = ((ToggleButton) view).isChecked();

    if (on)
    {
      // Set up a pointer to the remote node using it's address.
      BluetoothDevice device = myBluetoothAdapter.getRemoteDevice(address);

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
      myBluetoothAdapter.cancelDiscovery();

      // Establish the connection. This will block until it connects.
      Log.d("Cnct", "...Connecting to Remote...");
      try
      {
        btSocket.connect();
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
        connected = true;
        Toast.makeText(getApplicationContext(),
            "Connection established and data link opened!!", Toast.LENGTH_LONG)
            .show();
        Log.d("Cnct", "...Connection established and data link opened...");
      } catch (IOException e)
      {
        Log.d("Cnct", "e3");
      }
    }
    else
    {
      
      ToggleButton toggleButton = (ToggleButton) findViewById(R.id.dataToggle);
      toggleButton.setChecked(false);
      
      
      try
      {
        outStream.close();
        btSocket.close();
      } catch (IOException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
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
    Intent intent = new Intent(this, SettingsActivity.class);

    int id = item.getItemId();
    if (id == R.id.action_settings)
    {
      startActivityForResult(intent, 0);
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
    if (sendFlag)
    {
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
        if (z >= 17)
        {
          if (y >= 11)
          {
            commandImage.setImageResource(R.drawable.arrow_right_up);
            newCommand = "5";
            Log.d("Send", "Z: " + z + "; Y: " + y);
          }
          else if (y <= 6)
          {
            commandImage.setImageResource(R.drawable.arrow_left_up);
            newCommand = "4";
            Log.d("Send", "Z: " + z + "; Y: " + y);
          }
          else
          {
            commandImage.setImageResource(R.drawable.arrows_up);
            newCommand = "1";
            Log.d("Send", "Z: " + z + "; Y: " + y);
          }
        }
        else if (z <= 11)
        {
          commandImage.setImageResource(R.drawable.arrow_down);
          newCommand = "2";
          Log.d("Send", "Z: " + z + "; Y: " + y);
        }
        else if (y >= 11)
        {
          commandImage.setImageResource(R.drawable.arrow_right);
          newCommand = "6";
          Log.d("Send", "Z: " + z + "; Y: " + y);
        }
        else if (y <= 6)
        {
          commandImage.setImageResource(R.drawable.arrow_left);
          newCommand = "3";
          Log.d("Send", "Z: " + z + "; Y: " + y);
        }
        else
        {
          commandImage.setImageResource(R.drawable.stop);
          newCommand = "0";
          Log.d("Send", "Z: " + z + "; Y: " + y);
        }

        if (!newCommand.equalsIgnoreCase(command))
        {
          sendData(newCommand);
          command = newCommand;
        }
      }
    }
    else
      sendData("0");
  }

  @Override
  protected void onResume()
  {

    super.onResume();
    mSensorManager.registerListener(this, mGrav,
        SensorManager.SENSOR_DELAY_NORMAL);
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
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    // TODO Auto-generated method stub
    if (resultCode == RESULT_OK && requestCode == 0)
    {
      address = data.getExtras().getString("address");
      deviceName = data.getExtras().getString("name");

      instructions.setVisibility(View.INVISIBLE);
      
      dAddr.setText(address);
      dName.setText(deviceName);
    }
  }

  private void sendData(String message)
  {

    byte[] msgBuffer = message.getBytes();

    Log.d("Send", "...Sending data: " + message + "...");

    if(outStream != null) {
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

}
