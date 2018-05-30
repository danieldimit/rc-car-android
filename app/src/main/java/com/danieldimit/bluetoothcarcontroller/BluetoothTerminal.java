package com.danieldimit.bluetoothcarcontroller;


import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi")
/**
 * A class that runs in a separate thread and sends the messages
 * to the raspberry. It creates the socket connection once and then
 * passes the commands through it.
 * @author Daniel Dimitrov
 *
 */
public class BluetoothTerminal implements Runnable {

    private BluetoothAdapter mBluetoothAdapter = null;
    private boolean connected = false;
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice = null;
    protected BlockingQueue<String> queue = null;
    private Activity parentActivity;
    private boolean disconnect = false;
    private TextView t;

    private String TAG = "Bluetooth thread";

    public BluetoothTerminal(BlockingQueue<String> queue, Activity parentActivity,
                             BluetoothDevice chosenDevice, TextView t, boolean disconnect) {
        this.queue = queue;
        this.parentActivity = parentActivity;
        this.mmDevice = chosenDevice;
        this.t = t;
        this.disconnect = disconnect;
    }

    @Override
    public void run() {
        Log.e(TAG, "HERE AGAIN+++++++++++++++++++++++++++ " + (mmDevice != null));
        String msg;
        disconnect = false;

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //UUID of the application- can be anything, but must match in both ends of the socket
        UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");


        try {
            if (mmDevice != null) {
                Log.e(TAG, mmDevice.getName());
                mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
                if (!mmSocket.isConnected()){
                    Log.e(TAG, "not connected");
                    mmSocket.connect();
                    parentActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            t.setText("Connected");
                        }
                    });
                    Log.e(TAG, "connected");
                    connected = true;
                }

                while(connected) {
                    try {
                        if (disconnect) {
                            Log.e(TAG, "DISCONNECTINGGGGG ");
                            mmSocket.close();
                            connected = false;
                        }
                        msg = (String) queue.take();
                        sendBtMsg(msg);
                    } catch (InterruptedException e) {
                        connected = false;
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            connected = false;
            parentActivity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(parentActivity, "Connection failed",
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public void sendBtMsg(String msg) {
        OutputStream mmOutputStream;
        try {
            mmOutputStream = mmSocket.getOutputStream();
            mmOutputStream.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            connected = false;
        }
    }

    public void setDisconnect(boolean dc) {
        disconnect = dc;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setMmDevice(BluetoothDevice mm) {
        mmDevice = mm;
    }

}
