package com.alexhart.maglev2;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Alex on 10/18/2015.
 */
public class MagLevControlFrag extends Fragment implements View.OnClickListener{

    private String TAG = "MaLevFrag";

    //UI members//
    private EditText mBrightSet, mAmpSet, mFreqSet;
    private Switch mOffSwitch;
    private ListView mListView;
    private Button mSendButton, mDefaulButton, mCameraButton;

    private int brightVal, ampVal, freqVal;

    private Uri photoVideoIntent;


    //bluetooth members//
    private String mDeviceName;
    private boolean mBound = false;
    private boolean mScanning = false;
    private Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    private boolean wasConnected = false;
    private String mDeviceAddress;

    // Service state control
    private int mState;
    final private static int STATE_NONE = 0;
    final private static int STATE_BLUETOOTH_OFF = 1;
    final private static int STATE_DISCONNECTED = 2;
    final private static int STATE_CONNECTING = 3;
    final private static int STATE_CONNECTED = 4;
    final private static int STATE_SCAN_FAIL = 5;
    final private static int STATE_SCANNING = 6;
    private static final long SCAN_PERIOD = 10000;
    final private static int REQUEST_ENABLE_BT = 7;
    final private static int REQUEST_CONNECT_BLE_DEVICE = 8;
    final private static int REQUEST_PHOTO = 10;
    final private static int REQUEST_VIDEO = 20;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.maglev_frag, container, false);

        Log.d(TAG, "OnCreateView");
        //allows callback from actionBar with pager view
        setHasOptionsMenu(true);
        mHandler = new Handler();
        mListView = (ListView) v.findViewById(R.id.device_layout);


        // Check if BLE service is supported
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            makeToast(getString(R.string.ble_not_supported));
        }

        // API >18 get a reference to BluetoothAdapter through BluetoothManager!
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            makeToast(getString(R.string.bluetooth_not_supported));
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        initUI(v);
        setListener();



        return v;
    }


    /**
     * Control UI for user depending on app state
     *
     * @param newState state of app as int val, disconnected, searching...
     */
    private void updateState(int newState) {
        mState = newState;
        updateUi();
    }

    private void updateUi() {

        switch  (mState){

            case STATE_NONE:
//                mCameraFrameHolder.setVisibility(View.GONE);
            case STATE_SCANNING:
                getActivity().setTitle(R.string.app_name);
                mListView.setVisibility(View.VISIBLE);
//                mCameraFrameHolder.setVisibility(View.GONE);
//                mCameraPreview.setVisibility(View.GONE);
                break;
            case STATE_CONNECTED:
                mListView.setVisibility(View.GONE);
                break;
            case STATE_DISCONNECTED:
                break;
            case STATE_SCAN_FAIL:
                break;


        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sendData_button:
                double frequency;
                String msg;

                if (mBrightSet.getText().length()>0 && mAmpSet.getText().length()>0 &&
                        mFreqSet.getText().length()>0) {
                    brightVal = Integer.parseInt(mBrightSet.getText().toString());
                    ampVal = Integer.parseInt(mAmpSet.getText().toString());
                    freqVal = Integer.parseInt(mFreqSet.getText().toString());
                }else {
                    makeToast("Not all values set!");
                    break;
                }




                if (!checkValues(brightVal,ampVal,freqVal)){
                    makeToast("Error in values!");
                    break;
                }

                if (freqVal != 0) {
                    frequency = round(convertToMilliseconds(freqVal), 2);

                }else frequency = freqVal;

                msg = "b"+brightVal+"a"+ampVal+"f"+frequency;

                if (mConnected) {
                    mBluetoothLeService.send(msg.getBytes());
                    Log.d(TAG, "Message: " + msg);
                } else makeToast("Not Connected!");

                //sends brightness (PWM), amp (PWM), frequency (ms);

                break;

            case  R.id.default_button:
                makeToast("Set defaults!");

                break;
            case R.id.camera_button:
                break;
        }
    }


    private void initUI(View v) {

        mBrightSet = (EditText)v.findViewById(R.id.brightness_set);
        mAmpSet = (EditText)v.findViewById(R.id.amplitude_set);
        mFreqSet = (EditText)v.findViewById(R.id.frequency_set);
        mOffSwitch = (Switch)v.findViewById(R.id.off_switch);
        mSendButton = (Button)v.findViewById(R.id.sendData_button);
        mDefaulButton = (Button)v.findViewById(R.id.default_button);
        mCameraButton = (Button)v.findViewById(R.id.camera_button);

        mSendButton.setOnClickListener(this);
        mDefaulButton.setOnClickListener(this);
        mCameraButton.setOnClickListener(this);

        mOffSwitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener(){


            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    mBrightSet.setText("0");
                    mBrightSet.setEnabled(false);
                    mAmpSet.setText("0");
                    mAmpSet.setEnabled(false);
                    mFreqSet.setText("0");
                    mFreqSet.setEnabled(false);
                }else {
                    mBrightSet.setEnabled(true);
                    mAmpSet.setEnabled(true);
                    mFreqSet.setEnabled(true);

                }
            }
        });

        mBrightSet.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                //focus lost
                if (!b) {
                    if (mBrightSet.getText().length()>0) {
                        brightVal = Integer.parseInt(mBrightSet.getText().toString());
                        if (brightVal >=0 && brightVal<=255) {
                            makeToast("Volts: " + round(convertToVoltage(brightVal),2));
                        }else {
                            makeToast("Error, pick a value between 0-255");
                            mBrightSet.setText("");
                        }
                    }
                }
            }
        });

        mAmpSet.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b) {
                    if (mAmpSet.getText().length() > 0) {
                        ampVal = Integer.parseInt(mAmpSet.getText().toString());
                        if (ampVal >=0 && ampVal<=255) {
                            makeToast("Volts: " + round(convertToVoltage(ampVal),2));
                        }else {
                            makeToast("Error, pick a value between 0-255");
                            mAmpSet.setText("");
                        }
                    }
                }
            }
        });

        mFreqSet.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b) {
                    if (mFreqSet.getText().length()>0) {
                        freqVal = Integer.parseInt(mFreqSet.getText().toString());
                        if (freqVal >=0 && freqVal<=11) {
                            Log.d(TAG, "Frequency: " + freqVal);
                        }else {
                            makeToast("Error, pick a value between 0-100");
                            mFreqSet.setText("");
                        }
                    }

                }
            }
        });

//        mBrightSet.addTextChangedListener(new TextWatcher() {
//            double brightVolt;
//            String currentText;
//            String newText;;
//            boolean changeText = false;
//            int valIndex;
//
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                if(changeText) {
//                    return;
//                }
//
//                if (mBrightSet.getText().length() > 0) {
//                    currentText = mBrightSet.getText().toString();
//                    valIndex = currentText.indexOf(" ");
//                    if (valIndex == -1) {
//                        brightVal = Integer.parseInt(currentText);
//                    }else {
//                        brightVal = Integer.parseInt(currentText.substring(0,valIndex));
//                    }
//
//                    brightVolt = round(convertToVoltage(brightVal),2);
//                    newText = currentText + "  (Volts: " + Double.toString(brightVolt) + ")";
//                    changeText = true;
//
//                    mBrightSet.setText(newText, TextView.BufferType.EDITABLE);
//                    changeText = false;
//                    makeToast(mBrightSet.getText().toString());
//                }
//
//
//
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//
//
//            }
//        });


    }




    private boolean checkValues (int bright, int amp, int freq) {

        if (bright > 255 || bright<0){
            return false;
        }else if (amp > 255 || amp<0){
            return false;
        }else if (freq > 100 || freq<0) {
            return false;
        }
        return true;

    }


    private void makeToast(String s) {
        Toast.makeText(getActivity(),s,Toast.LENGTH_SHORT).show();
    }

    //convert PWM value to a voltage to show user
    //assumes operating voltage of 3V
    public double convertToVoltage (int pwmLevel) {
        return (double)pwmLevel*(3.0 / 255.0);

    }

    public double convertToMilliseconds (int freq) {
        return (1000.0 / freq);
    }



    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    //-------------------------------------------------------//
    //------------- BLUETOOTH LIGHT MANAGEMENT---------------//
    //-------------------------------------------------------//

    //set Listview listener to connect to BLE device once selected
    private void setListener (){
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
                if (device == null) return;

                mDeviceAddress = device.getAddress();
                mDeviceName = device.getName();
                getActivity().setTitle(mDeviceName);
//                getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
                Intent gattServiceIntent = new Intent(getActivity(), BluetoothLeService.class);
                getActivity().bindService(gattServiceIntent, mServiceConnection, getActivity().BIND_AUTO_CREATE);
                mBound = true;
                if (mScanning) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mScanning = false;
                }


            }
        });

    }

    /**
     * Handles event updates from service
     */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateState(STATE_CONNECTED);
                makeToast("Connected");

                wasConnected = true;
                getActivity().invalidateOptionsMenu();
                Log.d(TAG, "Connected to service");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                updateState(STATE_DISCONNECTED);
                mConnected = false;
                mBound = false;
//                updateConnectionState(R.string.disconnected);
                makeToast("Disconnected!");
                getActivity().invalidateOptionsMenu();
                Log.d(TAG, "Disconnected from service");
            }
//              } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
//                // Show all the supported services and characteristics on the user interface.
//                displayGattServices(mBluetoothLeService.getSupportedGattServices());
//            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
//            }
        }
    };






    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = getActivity().getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }



        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }


    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();



            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            mConnected = false;
        }
    };



    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    getActivity().invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        getActivity().invalidateOptionsMenu();
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_BLE_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                }
                break;

            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    makeToast("Bluetooth Enabled!");
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    makeToast("Bluetooth not enabled!");
                }
                break;

            case REQUEST_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    MediaScannerConnection.scanFile(getActivity().getApplicationContext(), new String[]{photoVideoIntent.getPath()}, null, null);
                    makeToast("Saved to: " + photoVideoIntent.getPath());
                    sendCameraBroadcast();

                }else if (resultCode == Activity.RESULT_CANCELED) {
                    makeToast("Photo cancelled!");
                }
                break;

            case REQUEST_VIDEO:
                if (resultCode == Activity.RESULT_OK) {
                    makeToast("Saved to: " + photoVideoIntent.getPath());
                    MediaScannerConnection.scanFile(getActivity().getApplicationContext(), new String[]{photoVideoIntent.getPath()}, null, null);
                    sendCameraBroadcast();

                }else if (resultCode == Activity.RESULT_CANCELED) {
                    makeToast("Video cancelled!");
                }
        }


    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                if (mBluetoothAdapter.isEnabled()) {
                    updateState(STATE_SCANNING);
                    scanLeDevice(true);
                } else makeToast("Bluetooth not enabled!");
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;

            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;

            case R.id.menu_camera_intent:
                Intent i = new Intent (MediaStore.ACTION_IMAGE_CAPTURE);
                photoVideoIntent = PreviewFrag.getOutputMediaFileUri(PreviewFrag.MEDIA_TYPE_IMAGE);
                i.putExtra(MediaStore.EXTRA_OUTPUT, photoVideoIntent);
                startActivityForResult(i,REQUEST_PHOTO);
                break;
            case R.id.menu_video_intent:
                Intent i2 = new Intent (MediaStore.ACTION_VIDEO_CAPTURE);
                photoVideoIntent = PreviewFrag.getOutputMediaFileUri(PreviewFrag.MEDIA_TYPE_VIDEO);
                i2.putExtra(MediaStore.EXTRA_OUTPUT, photoVideoIntent);
                //high quality
                i2.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
                startActivityForResult(i2, REQUEST_VIDEO);
                break;
        }
        return true;
    }


    private void sendCameraBroadcast() {
        Intent i = new Intent(GalleryViewFrag.CAMERA_ACTION);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(i);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "OnPause");
        mLeDeviceListAdapter.clear();
        getActivity().unregisterReceiver(mGattUpdateReceiver);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "OnDestroyView");

        if (mBound) {
            getActivity().unbindService(mServiceConnection);
            mBound = false;
        }

        mBluetoothLeService = null;

        mBluetoothAdapter.stopLeScan(mLeScanCallback);

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "OnResume");

        mLeDeviceListAdapter = new LeDeviceListAdapter();
        getActivity().invalidateOptionsMenu();

        mListView.setAdapter(mLeDeviceListAdapter);
        getActivity().registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

//        openBackgroundThread();
//
//        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);

    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        if (mConnected && wasConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else if (wasConnected) {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
    }

}

