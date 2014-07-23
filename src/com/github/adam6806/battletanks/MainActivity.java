package com.github.adam6806.battletanks;

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
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.UUID;

public class MainActivity extends Activity implements SensorEventListener {

    // This is the standard SPP UUID
    private static final UUID MY_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    int[] sensorValues = {0, 0, 0};
    private SensorManager mSensorManager;
    private Sensor mGrav;
    private TextView dName;
    private TextView dAddr;
    private TextView instructions;
    private EditText urlText;
    private ImageView commandImage;
    private ImageView forwardImage;
    private ImageView reverseImage;
    private OutputStream outStream;
    private String address;
    private String deviceName;
    private String vidAddress;
    private String command = "0";
    private BluetoothAdapter myBluetoothAdapter;
    private BluetoothSocket btSocket;
    private boolean sendFlag;
    private boolean forwardPressed;
    private boolean reversePressed;
    private boolean connected;
    private SharedPreferences preferences;

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
        urlText = (EditText) findViewById(R.id.editText);
        commandImage = (ImageView) findViewById(R.id.cmdImageView);
        forwardImage = (ImageView) findViewById(R.id.forwardButton);
        reverseImage = (ImageView) findViewById(R.id.reverseButton);

        // Get preferences
        preferences = getPreferences(MODE_PRIVATE);
        if (preferences.contains("name")) {
            instructions.setText("Not Connected");
        } else {
            instructions.setText("Go to Settings to pick a device.");
        }
        deviceName = preferences.getString("name", "Device Name");
        address = preferences.getString("address", "Device Address");
        vidAddress = preferences.getString("url", "");

        dName.setText(deviceName);
        dAddr.setText(address);
        urlText.setText(vidAddress);

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

        // Touch listener for the forward image.
        forwardImage.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    forwardImage.setColorFilter(0xFFFF0000, PorterDuff.Mode.MULTIPLY); // Make the image look clicked
                    forwardPressed = true;
                    if (connected && sendFlag) {
                        sendData(command); // Send whatever the current forward direction is
                    }
                    return true;
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    forwardImage.clearColorFilter();
                    forwardPressed = false;
                    if (connected && sendFlag) {
                        sendData("0"); // Send stop
                    }
                    return true;
                }
                return false;
            }
        });

        // Touch listener for the reverse image.
        reverseImage.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    reverseImage.setColorFilter(0xFFFF0000, PorterDuff.Mode.MULTIPLY); // Make the image look clicked
                    reversePressed = true;
                    if (connected && sendFlag) {
                        commandImage.setImageResource(R.drawable.arrow_down);
                        sendData("2"); // Send reverse
                    }
                    return true;
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    reverseImage.clearColorFilter();
                    reversePressed = false;
                    if (connected && sendFlag) {
                        commandImage.setImageResource(R.drawable.arrows_up);
                        sendData("0"); // Send stop
                    }
                    return true;
                }
                return false;
            }
        });
    }

    // Click listener for the send data toggle button
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

    // Click listener for the connect/disconnect toggle button
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
                Log.i("Connect", "Creating bt socket.");
            } catch (IOException e) {
                Log.e("Connect", e.getMessage());
                Toast.makeText(getApplicationContext(), "Error connecting to device.",
                        Toast.LENGTH_SHORT).show();
            }

            // Discovery is resource intensive. Make sure it isn't going on
            // when you attempt to connect and pass your message.
            myBluetoothAdapter.cancelDiscovery();

            // Establish the connection. This will block until it connects.
            Log.i("Connect", "...Connecting to Remote...");
            Toast.makeText(getApplicationContext(), "Connecting to " + deviceName,
                    Toast.LENGTH_LONG).show();
            try {
                btSocket.connect();
            } catch (IOException e) {
                try {
                    btSocket.close();
                } catch (IOException e2) {
                    Log.e("Connect", e2.getMessage());
                }
            }

            // Create a data stream so we can talk to server.
            Log.i("Connect", "...Creating Socket...");

            try {
                outStream = btSocket.getOutputStream();
                connected = true;
                instructions.setText("Connected with device! Data is disabled.");
            } catch (IOException e) {
                Log.e("Connect", e.getMessage());
            }

            Log.i("Connect", "...Connection established and data link opened...");

            ((ToggleButton) findViewById(R.id.lightsToggle)).setChecked(true);
            sendData("7");

        } else {

            sendData("8"); // turn the lights off first
            SystemClock.sleep(200); // give the data time to be sent before closing the connection

            connected = false;
            commandImage.setImageResource(R.drawable.stop);

            // Turn data off if disconnect occurs
            ToggleButton dataToggle = (ToggleButton) findViewById(R.id.dataToggle);
            dataToggle.setChecked(false);
            sendFlag = false;

            ToggleButton lightsToggle = (ToggleButton) findViewById(R.id.lightsToggle);
            lightsToggle.setChecked(false);

            // Close all the connections
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
            sendData("7"); // Turn on led on microcontroller

            // Send command to other phone to turn on the flash
            // Network IO must run in separate thread
            if (!vidAddress.isEmpty()) {
                Thread thread = new Thread(new Runnable() {

                    @Override
                    public void run() {

                        try {
                            String urlString = vidAddress.substring(0, vidAddress.length() - 14);
                            urlString = urlString + "enabletorch";
                            URL url = new URL(urlString);
                            InputStream is = url.openStream();
                            is.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
            }

        } else if (connected) {
            sendData("8"); // Turn off led on arduino

            // Send command to other phone to turn off the flash
            // Network IO must run in separate thread
            if (!vidAddress.isEmpty()) {
                Thread thread = new Thread(new Runnable() {

                    @Override
                    public void run() {

                        try {
                            String urlString = vidAddress.substring(0, vidAddress.length() - 14);
                            urlString = urlString + "disabletorch";
                            URL url = new URL(urlString);
                            InputStream is = url.openStream();
                            is.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
            }

        } else {
            ((ToggleButton) view).setChecked(false);
            instructions.setText("Must be connected to turn the lights on!");
        }
    }

    // Connect/disconnect to the other phone's video stream
    public void toggleVideoStream(View view) {

        boolean on = ((ToggleButton) view).isChecked();
        WebView vidView = (WebView) findViewById(R.id.videoView);
        //vidView.getSettings().setJavaScriptEnabled(true);
        //vidView.getSettings().setBuiltInZoomControls(true);
        boolean noUrl = urlText.getText().toString().equals("");

        if (on && !noUrl) {
            vidAddress = urlText.getText().toString();
            vidView.loadUrl(vidAddress);
        } else if (on && noUrl) {
            ((ToggleButton) view).setChecked(false);
            instructions.setText("Must provide the url of the video stream.");
        } else {
            vidView.stopLoading();
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

        // Go to the Settings activity
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


    // Change steering based on Y value from gravity sensor
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
                    } else if (y <= 6) {
                        commandImage.setImageResource(R.drawable.arrow_left);
                        newCommand = "3";
                    } else if (y >= 11) {
                        commandImage.setImageResource(R.drawable.arrow_right_up);
                        newCommand = "5";
                    } else if (y <= 8) {
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

    // Return from Settings activity. Set the address and name of selected device
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK && requestCode == 0) {
            address = data.getExtras().getString("address");
            deviceName = data.getExtras().getString("name");

            dAddr.setText(address);
            dName.setText(deviceName);

            // Store preferences
            SharedPreferences settings = getPreferences(MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("name", deviceName);
            editor.putString("address", address);
            editor.commit();
        }
    }

    // Send data to arduino
    private void sendData(String message) {

        byte[] msgBuffer = message.getBytes();

        Log.d("Send", "...Sending data: " + message + "...");

        if (outStream != null) {
            try {
                outStream.write(msgBuffer);
            } catch (IOException e) {
                Log.e("Sending", e.getMessage());
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
        editor.putString("url", vidAddress);
        editor.commit();

        // Turn data off if disconnect occurs
        ToggleButton dataToggle = (ToggleButton) findViewById(R.id.dataToggle);
        dataToggle.setChecked(false);

        ToggleButton connectToggle = (ToggleButton) findViewById(R.id.connectToggle);
        connectToggle.setChecked(false);

        sendData("8"); // Try to turn the lights off first

        // Close streams if they were open
        try {
            if (outStream != null)
                outStream.close();
            if (btSocket != null)
                btSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
