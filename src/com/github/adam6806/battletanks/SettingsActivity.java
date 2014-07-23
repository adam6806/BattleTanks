package com.github.adam6806.battletanks;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by Adam Smith on 7/21/2014.
 */
public class SettingsActivity extends Activity {


    private Button listBtn;
    private ListView myListView;
    private BluetoothAdapter myBluetoothAdapter;
    private HashMap<String, String> deviceMap;
    private ArrayAdapter<String> BTArrayAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private String deviceName;
    private String address;
    private Intent myIntent;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        myIntent = this.getIntent();

        listBtn = (Button) findViewById(R.id.paired);
        myListView = (ListView) findViewById(R.id.listView);

        // ArrayAdapter to hold the list of devices
        BTArrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1);
        myListView.setAdapter(BTArrayAdapter);
        deviceMap = new HashMap();

        // Get the devices bluetooth adapter
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Check if the device supports bluetooth
        if (myBluetoothAdapter == null) {
            listBtn.setEnabled(false);
            Toast.makeText(getApplicationContext(),
                    "Your device does not support Bluetooth", Toast.LENGTH_LONG).show();
        } else {
            BroadcastReceiver bReceiver = new BroadcastReceiver() {

                public void onReceive(Context context, Intent intent) {

                    String action = intent.getAction();
                    // When discovery finds a device
                    if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                        // Get the BluetoothDevice object from the Intent
                        BluetoothDevice device = intent
                                .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        // add the name and the MAC address of the object to the arrayAdapter
                        BTArrayAdapter.add(device.getName());
                        deviceMap.put(device.getName(), device.getAddress());
                        BTArrayAdapter.notifyDataSetChanged();
                    }
                }
            };

            myListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i,
                                        long l) {

                    deviceName = (String) ((TextView) view).getText();
                    address = deviceMap.get(deviceName);

                    Bundle deviceInfo = new Bundle();
                    deviceInfo.putString("name", deviceName);
                    deviceInfo.putString("address", address);
                    myIntent.putExtras(deviceInfo);
                    setResult(RESULT_OK, myIntent);
                    finish();
                }
            });
        }
    }

    public void list(View view) {
        // get paired devices
        pairedDevices = myBluetoothAdapter.getBondedDevices();
        BTArrayAdapter.clear();
        deviceMap.clear();

        // Put each paired device in the list and the device map
        for (BluetoothDevice device : pairedDevices) {
            BTArrayAdapter.add(device.getName());
            deviceMap.put(device.getName(), device.getAddress());
        }

        Toast.makeText(getApplicationContext(), "Updating list of paired devices",
                Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void onResume() {
        super.onResume();
        list(listBtn);
    }

    // Send user to phone's bluetooth settings to turn on bt and/or pair with a new device
    public void goToBtSettings(View v) {

        Intent settingsIntent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivity(settingsIntent);
    }
}