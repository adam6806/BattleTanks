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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity implements SensorEventListener {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final UUID MY_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    int[] sensorValues = {0, 0, 0};
    String command = "0";
    private SensorManager mSensorManager;
    private Sensor mGrav;
    private Button onBtn;
    private Button offBtn;
    private Button listBtn;
    private Button findBtn;
    private TextView text;
    private TextView cmdView;
    private BluetoothAdapter myBluetoothAdapter;
    private BluetoothSocket btSocket;
    private OutputStream outStream;
    private BroadcastReceiver bReceiver;
    private Set<BluetoothDevice> pairedDevices;
    private ListView myListView;
    private ArrayAdapter<String> BTArrayAdapter;
    private String address;
    private String deviceName;
    private HashMap<String, String> deviceMap;
    private boolean connected = false;
    private final String cmdString = "Command sent: ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Prevent screen from turning off
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        deviceMap = new HashMap();

        // Get UI elements
        text = (TextView) findViewById(R.id.text);
        cmdView = (TextView) findViewById(R.id.commandText);
        onBtn = (Button) findViewById(R.id.turnOn);
        offBtn = (Button) findViewById(R.id.turnOff);
        listBtn = (Button) findViewById(R.id.paired);
        findBtn = (Button) findViewById(R.id.search);
        myListView = (ListView) findViewById(R.id.listView1);

        // ArrayAdapter to hold the list of devices
        BTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        myListView.setAdapter(BTArrayAdapter);

        // Setup gravity sensor
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mGrav = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        // Check if the device has a gravity sensor
        if (mGrav == null) {
            Toast.makeText(getApplicationContext(), "Your device does not support Gravity sensor!!",
                    Toast.LENGTH_LONG).show();
        }

        // Get the devices bluetooth adapter
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Check if the device has a gravity sensor
        if (myBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Your device does not support Bluetooth",
                    Toast.LENGTH_LONG).show();
        }

        // Check if the device supports bluetooth
        if (myBluetoothAdapter == null || mGrav == null) {
            onBtn.setEnabled(false);
            offBtn.setEnabled(false);
            listBtn.setEnabled(false);
            findBtn.setEnabled(false);
            text.setText("Status: not supported");
        } else {
            bReceiver = new BroadcastReceiver() {

                public void onReceive(Context context, Intent intent) {

                    String action = intent.getAction();
                    // When discovery finds a device
                    if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                        // Get the BluetoothDevice object from the Intent
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        // add the name and the MAC address of the object to the arrayAdapter
                        BTArrayAdapter.add(device.getName());
                        deviceMap.put(device.getName(), device.getAddress());
                        BTArrayAdapter.notifyDataSetChanged();
                    }
                }
            };

            myListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                    deviceName = (String) ((TextView) view).getText();
                    address = deviceMap.get(deviceName);

                    // Set up a pointer to the remote node using it's address.
                    BluetoothDevice device = myBluetoothAdapter.getRemoteDevice(address);

                    // Two things are needed to make a connection:
                    // A MAC address, which we got above.
                    // A Service ID or UUID. In this case we are using the
                    // UUID for SPP.
                    try {
                        btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                        Log.d("Cnct", "uuid");
                    } catch (IOException e) {
                        Log.d("Cnct", "e");
                    }

                    // Discovery is resource intensive. Make sure it isn't going on
                    // when you attempt to connect and pass your message.
                    myBluetoothAdapter.cancelDiscovery();

                    // Establish the connection. This will block until it connects.
                    Log.d("Cnct", "...Connecting to Remote...");
                    try {
                        btSocket.connect();
                    } catch (IOException e) {
                        try {
                            btSocket.close();
                        } catch (IOException e2) {
                            Log.d("Cnct", "e2!");
                        }
                    }

                    // Create a data stream so we can talk to server.
                    Log.d("Cnct", "...Creating Socket...");

                    try {
                        outStream = btSocket.getOutputStream();
                        connected = true;
                        text.setText("Status: Connected to " + deviceName);
                        Toast.makeText(getApplicationContext(), "Connection established and data link opened!!",
                                Toast.LENGTH_LONG).show();
                        Log.d("Cnct", "...Connection established and data link opened...");
                    } catch (IOException e) {
                        Log.d("Cnct", "e3");
                    }
                }
            });
        }
    }

    public void on(View view) {

        if (!myBluetoothAdapter.isEnabled()) {
            Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);

            Toast.makeText(getApplicationContext(), "Bluetooth turned on",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Bluetooth is already on",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        if (requestCode == REQUEST_ENABLE_BT) {
            if (myBluetoothAdapter.isEnabled()) {
                text.setText("Status: Enabled");
            } else {
                text.setText("Status: Disabled");
            }
        }
    }

    public void list(View view) {
        // get paired devices
        pairedDevices = myBluetoothAdapter.getBondedDevices();
        BTArrayAdapter.clear();
        deviceMap.clear();

        // put it's one to the adapter
        for (BluetoothDevice device : pairedDevices) {
            BTArrayAdapter.add(device.getName());
            deviceMap.put(device.getName(), device.getAddress());
        }

        Toast.makeText(getApplicationContext(), "Show Paired Devices",
                Toast.LENGTH_SHORT).show();

    }

    public void find(View view) {

        if (myBluetoothAdapter.isDiscovering()) {
            // the button is pressed when it discovers, so cancel the discovery
            myBluetoothAdapter.cancelDiscovery();
        } else {
            BTArrayAdapter.clear();
            deviceMap.clear();
            myBluetoothAdapter.startDiscovery();

            registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        }
    }

    public void off(View view) {

        myBluetoothAdapter.disable();
        text.setText("Status: Disconnected");

        Toast.makeText(getApplicationContext(), "Bluetooth turned off",
                Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        unregisterReceiver(bReceiver);
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

        if (connected) {
            int x = (int) (event.values[0] + 10f);
            int y = (int) (event.values[1] + 10f);
            int z = (int) (event.values[2] + 10f);

            if (y != sensorValues[1] || z != sensorValues[2]) {
                Log.i("Test", "X: " + x + "; Y: " + y + "; Z: " + z);

                sensorValues[0] = x;
                sensorValues[1] = y;
                sensorValues[2] = z;

                String newCommand;
                if (z >= 17) {
                    if (y >= 11) {
                        newCommand = "5";
                        cmdView.setText(cmdString + "Turn Right");
                        Log.d("Send", "Z: " + z + "; Y: " + y);
                    } else if (y <= 6) {
                        newCommand = "4";
                        cmdView.setText(cmdString + "Turn Left");
                        Log.d("Send", "Z: " + z + "; Y: " + y);
                    } else {
                        newCommand = "1";
                        cmdView.setText(cmdString + "Forward");
                        Log.d("Send", "Z: " + z + "; Y: " + y);
                    }
                } else if (z <= 11) {
                    newCommand = "2";
                    cmdView.setText(cmdString + "Backwards");
                    Log.d("Send", "Z: " + z + "; Y: " + y);
                } else if (y >= 11) {
                    newCommand = "6";
                    cmdView.setText(cmdString + "Spin Right");
                    Log.d("Send", "Z: " + z + "; Y: " + y);
                } else if (y <= 6) {
                    newCommand = "3";
                    cmdView.setText(cmdString + "Spin Left");
                    Log.d("Send", "Z: " + z + "; Y: " + y);
                } else {
                    newCommand = "0";
                    cmdView.setText(cmdString + "Stop");
                    Log.d("Send", "Z: " + z + "; Y: " + y);
                }

                if (!newCommand.equalsIgnoreCase(command)) {
                    sendData(newCommand);
                    command = newCommand;
                }
            }
        }
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

        Log.d("Paused", "...In onPause()...");

        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {

            }
        }
    }

    private void sendData(String message) {

        byte[] msgBuffer = message.getBytes();

        Log.d("Send", "...Sending data: " + message + "...");

        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
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
