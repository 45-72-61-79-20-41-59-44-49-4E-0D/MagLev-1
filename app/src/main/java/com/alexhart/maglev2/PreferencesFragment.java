package com.alexhart.maglev2;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import java.util.Arrays;


/**
 * Display preferences fragment on an activity rather than having a separate fragment
 * holder. Contains bluetooth settings, user settings...
 */


public class PreferencesFragment extends Activity {

    private static String mCameraID;
    private static final String TAG = "PrefFrag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCameraID = getIntent().getStringExtra("cameraID");
        Toast.makeText(this, "Cameraid" + mCameraID, Toast.LENGTH_SHORT).show();
        //display preferences fragment as only fragment in activity
        
        getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFrag()).commit();


    }

    public static class PrefsFrag extends PreferenceFragment{

        private ListPreference mPictureQuality;
        private ListPreference mVideoQuality;
        private ListPreference mVideoFormat;
        private CheckBoxPreference mCameraStartup;
        private CameraManager mCameraManager;
        private CameraCharacteristics mCameraCharacteristics;




        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_frag);
            initUI();





//            PreferenceScreen pPFDeleteUserData = (PreferenceScreen) findPreference("user_deleteData_pref");
//            PreferenceScreen pPFDeleteUserInfo = (PreferenceScreen) findPreference("user_deleteInfo_pref");
//
//            final CheckBoxPreference pCheckboxBluetooth = (CheckBoxPreference)findPreference("checkbox_preference");
//
//            final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//            if (mBluetoothAdapter.isEnabled()) {
//                pCheckboxBluetooth.setChecked(true);
//            }
//            else {pCheckboxBluetooth.setChecked(false);}
//
//            pCheckboxBluetooth.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//                @Override
//                public boolean onPreferenceClick(Preference preference) {
//                    if (pCheckboxBluetooth.isChecked()) {
//                        mBluetoothAdapter.enable();
//                        Log.d(TAG, "bluetooth enabled");
//                        return true;
//                    } else {
//                        mBluetoothAdapter.disable();
//                        pCheckboxBluetooth.setSummary("ITS OFF");
//
//                        return false;
//                    }
//                }
//            });
//
//            pPFDeleteUserData.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//                @Override
//                public boolean onPreferenceClick(Preference preference) {
//                    onDeleteUserData();
//                    return false;
//                }
//            });
//
//            pPFDeleteUserInfo.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//                @Override
//                public boolean onPreferenceClick(Preference preference) {
//                    onDeleteUserInfo();
//                    return false;
//                }
//            });




        }

        private void initUI() {
            mPictureQuality = (ListPreference) findPreference("picture_quality");
            mVideoQuality = (ListPreference) findPreference("video_quality");
            mVideoFormat = (ListPreference) findPreference("video_format");
            mCameraStartup = (CheckBoxPreference) findPreference("camera_startup");


            mCameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
            try {
                mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraID);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            StreamConfigurationMap map = mCameraCharacteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            Size[] picSizes = map.getOutputSizes(ImageFormat.JPEG);

            //TODO fix pref size/format selection
//            char[] mPicQualityArray = new char[picSizes.length];
//            for (int i=0; i < picSizes.length; i++) {
//                mPicQualityArray[i] = picSizes[i].toString();
//            }
//            mPictureQuality.setEntries(map.getOutputSizes(ImageFormat.JPEG));

        }


        @Override
        public void onStop() {
            super.onStop();
        }

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

}
