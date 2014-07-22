package com.example.battletanks;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends Activity implements SensorEventListener {

    private static final UUID MY_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    int[] sensorValues = {0, 0, 0};
    String command = "0";
    private SensorManager mSensorManager;
    private Sensor mGrav;
    private TextView dName;
    private TextView dAddr;
    private TextView instructions;
    private ImageView commandImage;
    private ImageView forwardImage;
    private ImageView reverseImage;
    private OutputStream outStream;
    private String address;
    private String deviceName;
    private BluetoothAdapter myBluetoothAdapter;
    private BluetoothSocket btSocket;
    private boolean sendFlag;
    private boolean forwardPressed;
    private boolean reversePressed;
    private boolean connected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Prevent screen from turning off
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Get UI elements
        dName = (TextView) findViewById(R.id.deviceName);
        dAddr = (TextView) findViewById(R.id.deviceAddress);
        instructions = (TextView) findViewById(R.id.instructionText);
        commandImage = (ImageView) findViewById(R.id.cmdImageView);
        forwardImage = (ImageView) findViewById(R.id.forwardButton);
        reverseImage = (ImageView) findViewById(R.id.reverseButton);

        // Get preferences
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        if (settings.contains("name")) {
            instructions.setText("Not Connected");
        } else {
            instructions.setText("Go to settings to pick a device.");
        }
        deviceName = settings.getString("name", "Device Name");
        address = settings.getString("address", "Device Address");
        dName.setText(deviceName);
        dAddr.setText(address);

        // Setup gravity sensor
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mGrav = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        // Check if the device has a gravity sensor
        if (mGrav == null) {
            instructions.setText("Your device does not have a gravity sensor. Sorry!");
        }

        // Get the devices bluetooth adapter
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Check if the device has a gravity sensor
        if (myBluetoothAdapter == null) {
            instructions.setText("Your device does not have bluetooth. Sorry!");
        }

        forwardImage.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    forwardImage.setColorFilter(0xFFFF0000, PorterDuff.Mode.MULTIPLY); // Make the image look clicked
                    forwardPressed = true;
                    if (connected && sendFlag) {
                        sendData(command);
                    }
                    return true;
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    forwardImage.clearColorFilter();
                    forwardPressed = false;
                    if (connected && sendFlag) {
                        sendData("0");
                    }
                    return true;
                }
                return false;
            }
        });
        reverseImage.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    reverseImage.setColorFilter(0xFFFF0000, PorterDuff.Mode.MULTIPLY); // Make the image look clicked
                    reversePressed = true;
                    if (connected && sendFlag) {
                        commandImage.setImageResource(R.drawable.arrow_down);
                        sendData("2");
                    }
                    return true;
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    reverseImage.clearColorFilter();
                    reversePressed = false;
                    if (connected && sendFlag) {
                        commandImage.setImageResource(R.drawable.arrows_up);
                        sendData("0");
                    }
                    return true;
                }
                return false;
            }
        });
    }

    public void setSendFlag(View view) {

        if (connected) {
            sendFlag = ((ToggleButton) view).isChecked();
            if (sendFlag) instructions.setText("Data is enabled!");
            else instructions.setText("Data is disabled!");
        } else {
            instructions.setText("Must be connected to send data!");
            ((ToggleButton) view).setChecked(false);
        }
    }

    public void connect(View view) {
        // Is the toggle on?
        boolean on = ((ToggleButton) view).isChecked();

        if (on) {
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
                instructions.setText("Connected with device! Data is disabled.");
                Log.d("Cnct", "...Connection established and data link opened...");
            } catch (IOException e) {
                Log.d("Cnct", "e3");
            }
        } else {

            connected = false;
            commandImage.setImageResource(R.drawable.stop);

            // Turn data off if disconnect occurs
            ToggleButton dataToggle = (ToggleButton) findViewById(R.id.dataToggle);
            dataToggle.setChecked(false);

            sendData("8"); // turn the lights off first
            ToggleButton lightsToggle = (ToggleButton) findViewById(R.id.lightsToggle);
            lightsToggle.setChecked(false);

            try {
                outStream.close();
                btSocket.close();
                instructions.setText("Disconnected from device.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void lightToggle(View view) {
        // Is the toggle on?
        boolean on = ((ToggleButton) view).isChecked();
        if (on && connected) {
            sendData("7");
        } else if (connected) {
            sendData("8");
        } else {
            ((ToggleButton) view).setChecked(false);
            instructions.setText("Must be connected to turn the lights on!");
        }
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
        Intent intent = new Intent(this, SettingsActivity.class);

        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivityForResult(intent, 0);
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

        String newCommand = "";

        if (sendFlag && connected) {
            int y = (int) (event.values[1] + 10f);

            if (y != sensorValues[1]) {
                Log.i("Gravity", "Y: " + y);

                sensorValues[1] = y;

                // Don't change the image if reverse is pressed
                if (!reversePressed) {
                    // Change the image even if you aren't pressing forward to show where you're going to go
                    if (y >= 13) {
                        commandImage.setImageResource(R.drawable.arrow_right);
                        newCommand = "6";
                    } else if (y <= 5) {
                        commandImage.setImageResource(R.drawable.arrow_left);
                        newCommand = "3";
                    } else if (y >= 11) {
                        commandImage.setImageResource(R.drawable.arrow_right_up);
                        newCommand = "5";
                    } else if (y <= 7) {
                        commandImage.setImageResource(R.drawable.arrow_left_up);
                        newCommand = "4";
                    } else {
                        commandImage.setImageResource(R.drawable.arrows_up);
                        newCommand = "1";
                    }
                    if (command != newCommand && forwardPressed) sendData(newCommand);
                    command = newCommand;
                }
            }
        } else if (connected) {
            commandImage.setImageResource(R.drawable.stop);
            newCommand = "0";
            if (!newCommand.equalsIgnoreCase(command)) {
                sendData(newCommand);
                command = newCommand;
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK && requestCode == 0) {
            address = data.getExtras().getString("address");
            deviceName = data.getExtras().getString("name");

            instructions.setVisibility(View.INVISIBLE);

            dAddr.setText(address);
            dName.setText(deviceName);
        }
    }

    private void sendData(String message) {

        byte[] msgBuffer = message.getBytes();

        Log.d("Send", "...Sending data: " + message + "...");

        if (outStream != null) {
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

    @Override
    protected void onStop() {

        super.onStop();
        // Store preferences
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("name", deviceName);
        editor.putString("address", address);
        editor.commit();

        // Turn data off if disconnect occurs
        ToggleButton dataToggle = (ToggleButton) findViewById(R.id.dataToggle);
        dataToggle.setChecked(false);

        ToggleButton connectToggle = (ToggleButton) findViewById(R.id.connectToggle);
        connectToggle.setChecked(false);

        sendData("8"); // Try to turn the lights off first

        try {
            outStream.close();
            btSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
