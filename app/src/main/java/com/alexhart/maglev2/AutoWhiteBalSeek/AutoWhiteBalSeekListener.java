package com.alexhart.maglev2.AutoWhiteBalSeek;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.alexhart.maglev2.AutoWhiteBalSeek.AutoWhiteBalSeekBar;
import com.alexhart.maglev2.R;

/**
 *
 */
public class AutoWhiteBalSeekListener implements AutoWhiteBalSeekBar.OnAwbSeekBarChangeListener {
    private TextView mTextView;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mCameraCaptureSession;
    private Handler mHandler;
    private CameraCaptureSession.CaptureCallback mPreviewSessionCallback;

    public AutoWhiteBalSeekListener(Context mContext, TextView mTextView, CaptureRequest.Builder mPreviewBuilder, CameraCaptureSession mCameraCaptureSession, Handler mHandler, CameraCaptureSession.CaptureCallback mPreviewSessionCallback) {
        this.mTextView = mTextView;
        this.mPreviewBuilder = mPreviewBuilder;
        this.mCameraCaptureSession = mCameraCaptureSession;
        this.mHandler = mHandler;
    }

    @Override
    public void handleProgress1() {
        mTextView.setText(R.string.seek_bar_auto);
        mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_AUTO);
        updatePreview();
    }

    @Override
    public void handleProgress2() {
        mTextView.setText(R.string.seek_bar_cloudy_daylight);
        mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT);
        updatePreview();
    }

    @Override
    public void handleProgress3() {
        mTextView.setText(R.string.seek_bar_daylight);
        mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT);
        updatePreview();
    }

    @Override
    public void handleProgress4() {
        mTextView.setText(R.string.seek_bar_fluorescent);
        mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT);
        updatePreview();
    }

    @Override
    public void handleProgress5() {
        mTextView.setText(R.string.seek_bar_incandescent);
        mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT);
        updatePreview();
    }

    @Override
    public void handleProgress6() {
        mTextView.setText(R.string.seek_bar_shade);
        mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_SHADE);
        updatePreview();
    }

    @Override
    public void handleProgress7() {
        mTextView.setText(R.string.seek_bar_twilight);
        mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_TWILIGHT);
        updatePreview();
    }

    @Override
    public void handleProgress8() {
        mTextView.setText(R.string.seek_bar_warm_fluorescent);
        mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT);
        updatePreview();
    }

    @Override
    public void onStopTrackingTouch(int num) {
        switch (num) {
            case 0:
                mTextView.setText(R.string.seek_bar_auto);
                mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_AUTO);
                break;
            case 10:
                mTextView.setText(R.string.seek_bar_cloudy_daylight);
                mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT);
                break;
            case 20:
                mTextView.setText(R.string.seek_bar_daylight);
                mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT);
                break;
            case 30:
                mTextView.setText(R.string.seek_bar_fluorescent);
                mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT);
                break;
            case 40:
                mTextView.setText(R.string.seek_bar_incandescent);
                mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT);
                break;
            case 50:
                mTextView.setText(R.string.seek_bar_shade);
                mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_SHADE);
                break;
            case 60:
                mTextView.setText(R.string.seek_bar_twilight);
                mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_TWILIGHT);
                break;
            case 70:
                mTextView.setText(R.string.seek_bar_warm_fluorescent);
                mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT);
                break;
        }
        updatePreview();
        mTextView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mTextView.setVisibility(View.VISIBLE);
    }


    private void updatePreview() {
        try {
            mCameraCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("updatePreview", "Other exception");
        }
    }
}
