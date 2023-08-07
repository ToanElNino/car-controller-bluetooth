package com.imax.carcontrollergroup34;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BluetoothExample";
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 3;
    private static final Character ACTION_UP = '1';
    private static final Character ACTION_DOWN = '2';
    private static final Character ACTION_LEFT = '3';
    private static final Character ACTION_RIGHT = '4';
    private static final Character ACTION_STOP = '6';


    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothSocket bluetoothSocket;
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); // UUID for SPP (Serial Port Profile)

    MediaRecorder mediaRecorder = new MediaRecorder();

    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean isConnected = false;
    // thread
    public static Handler handler;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred while closing BluetoothSocket: " + e.getMessage());
                Toast.makeText(this, "Error occurred while closing BluetoothSocket: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if Bluetooth is supported on the device
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported on this device");
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show();
            return;
        }


        // Request Bluetooth permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, PERMISSION_REQUEST_CODE);
        }
        findViewById(R.id.btnConnect).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        enableBluetooth();
                    }
                }
        );


        findViewById(R.id.upBtn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d("CONTROL", "UP");
                        sendData(ACTION_UP);
                    }
                }
        );

        findViewById(R.id.down).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d("CONTROL", "DOWN");
                        sendData(ACTION_DOWN);
                    }
                }
        );

        findViewById(R.id.leftBtn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d("CONTROL", "LEFT");
                        sendData(ACTION_LEFT);
                    }
                }
        );

        findViewById(R.id.rightBtn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d("CONTROL", "RIGHT");
                        sendData(ACTION_RIGHT);
                    }
                }
        );

        findViewById(R.id.stop).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d("CONTROL", "STOP");
                        sendData(ACTION_STOP);
                    }
                }
        );
    }


    private void enableBluetooth() {
        // Check if Bluetooth is enabled, and if not, request to enable it
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                // Request permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                            REQUEST_ENABLE_BLUETOOTH);
                }
                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            connectToBluetoothDevice();
        }
//        connectToBluetoothDevice();
    }

    private void connectToBluetoothDevice() {
        // Get a set of bonded (paired) devices
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                // Request permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                            REQUEST_ENABLE_BLUETOOTH);
                }
//                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
            return;
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // Loop through the paired devices to find the HC-05 module
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("HC-05")) {
                    try {
                        // Create a BluetoothSocket for the device using the UUID
                        if(bluetoothSocket != null){
                            try {
                                bluetoothSocket.close();
                            } catch (IOException closeException) {
                                Log.e(TAG, "Could not close the client socket", closeException);
                            }
                        }
                            String add = device.getAddress();
                            BluetoothDevice hc = bluetoothAdapter.getRemoteDevice(add);
                            bluetoothSocket = hc.createRfcommSocketToServiceRecord(MY_UUID);
//                        bluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                            Log.d(TAG, "Name: " + device.getName() + "\n Address: " + device.getAddress() + ", UUID: " + Arrays.toString(device.getUuids()));
                            // Connect to the device
                            bluetoothSocket.connect();

                            inputStream = bluetoothSocket.getInputStream();
                            outputStream = bluetoothSocket.getOutputStream();

                            Log.d(TAG, "Connected to HC-05");
                            isConnected = true;
                            Toast.makeText(this, "Connected to HC-05", Toast.LENGTH_SHORT).show();
                            TextView status = (TextView) findViewById(R.id.tvStatus);
                            status.setText("Connected");
                            status.setTextColor(getResources().getColor(R.color.green));




                        // Perform read and write operations with the BluetoothSocket

                    } catch (IOException e) {
                        Log.e(TAG, "Error occurred while connecting to HC-05: " + e.getMessage());
                        Toast.makeText(this, "Error occurred while connecting to HC-05: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableBluetooth();
            } else {
                Log.e(TAG, "Bluetooth permission denied");
                Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show();
            }
        }
        enableBluetooth();
    }

    public void sendData(Character data) {
        if (bluetoothSocket != null) {
            try {
                outputStream.write(data.toString().getBytes());
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close the BluetoothSocket if it's open
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred while closing BluetoothSocket: " + e.getMessage());
                Toast.makeText(this, "Error occurred while closing BluetoothSocket: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }


}



