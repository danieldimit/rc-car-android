package com.danieldimit.bluetoothcarcontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.LightingColorFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity {


    // direction constants
    final int IR = -1; //irrelevant
    final int S = 0;
    final int F = 1;
    final int B = 2;
    final int L = 3;
    final int Ri = 4;

    private static final boolean D = true;

    // Blocking queue for passing messages to the bluetooth terminal
    BlockingQueue<String> queue = new LinkedBlockingQueue<String>();

    private boolean raspberryOn = false;

    final String TAG = "direction";

    /* Current State- the first element is either 0- Still, 1- Forward, 2-Backward
     * the second element is 0- Still, 1- Left, 2- Right
     */
    private int[] curState = new int[2];

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothDevice chosenDevice = null;

    //Initiate elements
    private ImageButton f;
    private ImageButton b;
    private ImageButton l;
    private ImageButton r;
    private ImageButton sd;
    private Button retry;
    private Spinner pairedDev;
    private TextView t;

    //The bluetooth worker thread
    Thread mRemoteService = null;
    BluetoothTerminal bt;

    boolean disconnect = false;

    @Override
    public void onStart() {
        super.onStart();
        Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 2);
            // Otherwise, setup the chat session
        }
        while (!mBluetoothAdapter.isEnabled()) {
        }
        setupChat();
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        disconnect = true;
        if (D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (D) Log.e(TAG, "+ ON RESUME +");

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        if (D) Log.e(TAG, "++ ON CREATE ++");

        // Initiate still state
        curState[0] = S;
        curState[1] = S;

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }



    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth remote control services
        if (mRemoteService != null) mRemoteService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void setupChat() {
        // Create a bluetooth threadthread = new Thread(bt);
        mRemoteService = new Thread(bt);

        t = (TextView) findViewById(R.id.text);
        f = (ImageButton) findViewById(R.id.imgF);
        b = (ImageButton) findViewById(R.id.imgB);
        l = (ImageButton) findViewById(R.id.imgL);
        r = (ImageButton) findViewById(R.id.imgR);
        sd = (ImageButton) findViewById(R.id.imgOff);
        retry = (Button) findViewById(R.id.retryConnection);
        pairedDev = (Spinner) findViewById(R.id.pairedDevSpinner);
        bt = new BluetoothTerminal(queue, MainActivity.this, chosenDevice, t, disconnect);

        // Populate paired devices
        final Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        final List<String> devicesList = new ArrayList<>();
        for(BluetoothDevice device : pairedDevices) {
            if(device.getName().equals("raspberrypi")) {
                devicesList.add(0, device.getName());
                chosenDevice = device;
            } else {
                devicesList.add(device.getName());
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, devicesList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pairedDev.setAdapter(adapter);

        pairedDev.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                for(BluetoothDevice device : pairedDevices) {
                    if(device.getName().equals(devicesList.get(position))) {
                        chosenDevice = device;
                        Log.e(TAG, device.getName());
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });

        //Listeners for the forward, backward, left and right buttons in that order
        f.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN) {
                    handleCmd(F, IR);
                    f.setColorFilter(new LightingColorFilter(0xFFFFFFFF, 0xFFAA0000));
                } else {
                    if(event.getAction()==MotionEvent.ACTION_UP) {
                        handleCmd(S, F);
                        f.setColorFilter(null);
                    }
                }
                return false;
            }
        });

        b.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN) {
                    handleCmd(B, IR);
                    b.setColorFilter(new LightingColorFilter(0xFFFFFFFF, 0xFFAA0000));
                } else {
                    if(event.getAction()==MotionEvent.ACTION_UP) {
                        handleCmd(S, B);
                        b.setColorFilter(null);
                    }
                }
                return false;
            }
        });

        l.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN) {
                    handleCmd(L, IR);
                    l.setColorFilter(new LightingColorFilter(0xFFFFFFFF, 0xFFAA0000));
                } else {
                    if(event.getAction()==MotionEvent.ACTION_UP) {
                        handleCmd(S, L);
                        l.setColorFilter(null);
                    }
                }
                return false;
            }
        });

        r.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN) {
                    handleCmd(Ri, IR);
                    r.setColorFilter(new LightingColorFilter(0xFFFFFFFF, 0xFFAA0000));
                } else {
                    if(event.getAction()==MotionEvent.ACTION_UP) {
                        handleCmd(S, Ri);
                        r.setColorFilter(null);
                    }
                }
                return false;
            }
        });

        sd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    if (raspberryOn == true) {
                        queue.put("SD");
                        t.setText("Shut Down");
                        sd.setColorFilter(new LightingColorFilter(0xFFFFFFFF, 0xFFAA0000));
                        raspberryOn = false;
                    } else {
                        t.setText("lel can't turn in on from the phone");
                        sd.setColorFilter(null);
                        raspberryOn = true;
                    }

                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        retry.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            if (!bt.isConnected()) {
                t.setText("Retrying...");
                disconnect = false;
                bt.setMmDevice(chosenDevice);
                mRemoteService = new Thread(bt);
                mRemoteService.start();
            } else {
                t.setText("Already connected");
            }
            }
        });
        mRemoteService.start();
        raspberryOn = true;
    }
    /**
     * Handler for the commands.
     * @param newState- newState
     * @param oldState- oldState
     */
    void handleCmd(int newState, int oldState) {

        //Set new state values
        switch (newState) {
            case S:
                switch (oldState) {
                    case F:
                    case B:
                        curState[0] = newState;
                        break;
                    case L:
                    case Ri:
                        curState[1] = newState;
                        break;
                }
                break;
            case F:
            case B:
                curState[0] = newState;
                break;
            case L:
            case Ri:
                curState[1] = newState - 2;
                break;
        }

        try {
            //Change text in the app and send the new direction to the bluetooth terminal
            if(curState[0] == S) {
                switch(curState[1]) {
                    case S:
                        t.setText("Staying Still");
                        queue.put("S");
                        Log.d(TAG, "still");
                        break;
                    case (L - 2):
                        t.setText("Still Left");
                        queue.put("L");
                        Log.d(TAG, "still left");
                        break;
                    case (Ri - 2):
                        t.setText("Still Right");
                        queue.put("R");
                        Log.d(TAG, "still right");
                        break;
                }
            } else {
                if (curState[0] == F) {
                    switch(curState[1]) {
                        case S:
                            t.setText("Moving Forward");
                            queue.put("F");
                            Log.d(TAG, "forward");
                            break;
                        case (L - 2):
                            t.setText("Forward Left");
                            queue.put("FL");
                            Log.d(TAG, "forward left");
                            break;
                        case (Ri - 2):
                            t.setText("Forward Right");
                            queue.put("FR");
                            Log.d(TAG, "forward right");
                            break;
                    }
                } else {
                    if (curState[0] == B) {
                        switch(curState[1]) {
                            case S:
                                t.setText("Moving Backward");
                                queue.put("B");
                                Log.d(TAG, "backward");
                                break;
                            case (L - 2):
                                t.setText("Backward Left");
                                queue.put("BL");
                                Log.d(TAG, "backward left");
                                break;
                            case (Ri - 2):
                                t.setText("Backward Right");
                                queue.put("BR");
                                Log.d(TAG, "backward right");
                                break;
                        }
                    }
                }
            }
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }
}
