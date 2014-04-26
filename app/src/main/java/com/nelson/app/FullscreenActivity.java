package com.nelson.app;

import com.nelson.app.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


/******Global Parameters***********/

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class FullscreenActivity extends Activity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;
    private final static int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mBluetoothSocket;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static OutputStream mOutputStream = null;
    private static BluetoothDevice mdevice;

    //Control button
    private Button mUpBtn, mStopBtn, mDownBtn;
    private TextView mMessageOutput;

    //private int mCount=0;
    //private TextView mTextViewConnect1;
    //private TextView mTextViewConnect2;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;
    private static final int SHOW_FLAGS = SystemUiHider.FLAG_FULLSCREEN;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_fullscreen);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        //findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        InitAction();

        mMessageOutput = (TextView)findViewById(R.id.messageoutput);
        mUpBtn = (Button)findViewById(R.id.upbutton);
        mStopBtn = (Button)findViewById(R.id.stopbutton);
        mDownBtn = (Button)findViewById(R.id.downbutton);
        mUpBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                mMessageOutput.setText("User press Up button");
                sendCommand("IamUpButton\n");
            }
        });
        mStopBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                mMessageOutput.setText("User press Stop button");
                sendCommand("IamStopButton\n");
            }
        });
        mDownBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                mMessageOutput.setText("User press Down button");
                sendCommand("IamDownButton\n");
            }
        });

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onPause() {
        /*
        super.onPause();
        try {
            if (mBluetoothSocket.isConnected()){
                mBluetoothSocket.close();
            }
            unregisterReceiver(mReceiver);
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
            //mSystemUiHider.show(); //NelsonDBG: modify here to test.
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    public void InitAction(){
        //Check the BT feature is supported.
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(this, "Device doesn't support bluetooth", Toast.LENGTH_SHORT).show();
            //return false;
        }

        //Enable Bluetooth
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else
        {
            //Communication with bt device
            CommunicationWithBtDevice();
        }
        //return true;
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        /* handle activity result here */
        if(result==RESULT_OK)
        {
            Toast.makeText(this, "User allow to enable bt feature", Toast.LENGTH_SHORT).show();
            Log.d("NelsonDBG","User allow to enable bt feature");
            CommunicationWithBtDevice();
        }
        else if(result==RESULT_CANCELED)
        {
            Toast.makeText(this, "User not allow to enable bt feature", Toast.LENGTH_SHORT).show();
        }
    }

    private void ShowAlertDialog(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        /*
        AlertDialog.Builder alertdialog = new AlertDialog.Builder(this);
        alertdialog.setTitle("中文測試");
        alertdialog.setMessage(msg);
        alertdialog.show();
        delayedHide(2000);
        */
    }
    private void sendCommand(String message){
        Log.d("NelsonDBG","send the message - " +message+ " - to arduino.");
        try{
            //mBluetoothSocket = mdevice.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            if(!mBluetoothSocket.isConnected()){
                Log.d("NelsonDBG","bluetooth socket is not connected and try to connect again.");
                mBluetoothSocket.connect();
            }
            else{
                Log.d("NelsonDBG","bluetooth socket is connected.");
            }

            mOutputStream = mBluetoothSocket.getOutputStream();
            Log.d("NelsonDBG","getOutoutStream ok");

            mOutputStream.write(message.getBytes());
            Log.d("NelsonDBG","write ok");

            //mBluetoothSocket.close();
        }
        catch(IOException e){
            Log.d("NelsonDBG","****Something wrong in function - setCommand ****");
            ShowAlertDialog("請重新連線藍芽裝置");
        }
    }

    private boolean CommunicationWithBtDevice()
    {
        Log.d("NelsonDBG","run CommunicationWithBtDevice");
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            Log.d("NelsonDBG","pairedDevice.size() is "+pairedDevices.size());
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                //Log.d("NelsonDBG","pairedDevices: "+device.getName() + "\t" + device.getAddress());

                String strdevicename = "“nelsonchung”的 MacBook Pro";
                if(device.getName().equals(strdevicename)){
                    try{
                        Log.d("NelsonDBG","check the device name is " +strdevicename);
                        //mBluetoothAdapter.cancelDiscovery();
                        mdevice = device;
                        mBluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                        mBluetoothSocket.connect();
                        Log.d("NelsonDBG","get bluetooth socket ok");
                        mOutputStream = mBluetoothSocket.getOutputStream();
                        Log.d("NelsonDBG","getOutoutStream ok");

                        String message = "hello";
                        mOutputStream.write(message.getBytes());
                        Log.d("NelsonDBG","write ok");

                        //mBluetoothSocket.close();
                    }
                    catch(IOException e){
                        Log.d("NelsonDBG","****Something wrong ****");
                        ShowAlertDialog("請重新連線藍芽裝置");
                    }

                }
            }
        }
        /* Nelson: Let user to pair bluetooth via the bt connection in device.
        else //No Any paired devices, so we need to discover devices
        {
            Log.d("NelsonDBG","start bt discovery");
            // Register the BroadcastReceiver
            //mBluetoothAdapter.startDiscovery();
            //Intent discoverableIntent = new
            //        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            //discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            //startActivity(discoverableIntent);

            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
            mBluetoothAdapter.startDiscovery();
        }
        */
        return true;
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d("NelsonDBG","call onReceive");
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
            //if (BluetoothDevice.FOUND.equals(intent.getAction())) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                Log.d("NelsonDBG","1");
                Log.d("NelsonDBG","search devices: "+device.getName() + "\t" + device.getAddress());

                /*switch(mCount)
                {
                    case 0:
                        mTextViewConnect1 = (TextView) findViewById(R.id.connect1);
                        mTextViewConnect1.setText(device.getName());
                        break;
                    case 1:
                        mTextViewConnect2 = (TextView) findViewById(R.id.connect2);
                        mTextViewConnect2.setText(device.getName());
                        break;
                    default:
                        break;

                }*/
            }
            else
            {
                Log.d("NelsonDBG",intent.getAction());
            }
        }
    };
}
