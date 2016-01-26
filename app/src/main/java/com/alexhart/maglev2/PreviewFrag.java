package com.alexhart.maglev2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.alexhart.maglev2.AutoWhiteBalSeek.AutoWhiteBalSeekBar;
import com.alexhart.maglev2.AutoWhiteBalSeek.AutoWhiteBalSeekListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by Alex on 10/18/2015.
 *
 *
 */
public class PreviewFrag extends Fragment implements View.OnClickListener{

    //
    private boolean mRecording = false;
    private ImageView mVideoButton;
    private Switch mSwitchAutoFoc;
    private boolean inPicturePreview = false;
    private boolean inVideoPreview = false;

    //UI members
    private ImageView mCameraButton;
    private AutoWhiteBalSeekBar mAutoWhiteBalSeek;
    private LinearLayout mLayoutIso;
    private TextView mSeekBarTextView;
    private AutoFitTextureView mTextureView;
    private RelativeLayout mLayoutBottom;
    private LinearLayout mLayoutAutoFoc;
    private RelativeLayout mLayoutCapture;
    private boolean showAutoFocFlag = false;
    private boolean showZoomFlag = false;
    private boolean showAutoExpFlag = false;
    private boolean showIsoFlag = false;
    private boolean showAutoWhitBalFlag = false;
    private List<View> mLayoutList;

    private static final int SHOW_AUTOFOC = 1;
    private static final int SHOW_AUTOEXP = 2;
    private static final int SHOW_AUTOWHITBAL = 3;
    private static final int SHOW_ISO = 4;
    private static final int SHOW_ZOOM = 5;

    //video members
    private Size mVideoSize;
    private Size mVideoPreviewSize;
    private MediaRecorder mMediaRecorder;
    private Uri mVideoUri;
    boolean recording = false;
    private CameraCaptureSession mVideoCaptureSession;


    //camera members
    private boolean mCameraConfig = false;
    private Size mLargestImageSize;
    public static final int MEDIA_TYPE_IMAGE = 10;
    public static final int MEDIA_TYPE_VIDEO = 20;
    final private static int STATE_OFF = 0;
    final private static int STATE_CAMERA = 1;
    final private static int STATE_VIDEO = 2;
    final private static int STATE_CAPTURE = 3;
    private int mCameraState = STATE_OFF;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice = null;
    //front facing camera has id 0, LOCKED on this
    private String mCameraId = "0";
    private CaptureRequest mPreviewCaptureRequest;
    private CameraCaptureSession mCameraCaptureSession;
    private float valueAF;
    private int valueAE;
    private long valueAETime;
    private int valueISO;
    //TODO add in last vals to exposure pipeline
    private float lastValueAF;
    private long lastValueAETime;
    private int lastValueAEComp;
    private float lastAETime;
    private int lastValueISO;


    private CameraCharacteristics mCameraCharacteristics;
    private List<Surface> mOutputSurfaces;
    private Size mPreviewSize;
    private File mWrittenFile;
    private File mVideoFile;
    private static String mImageFileLocation = "";
    private Surface mSurface;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private CaptureRequest.Builder mPreviewBuilder;
    private CaptureRequest.Builder mCaptureBuilder;

    private ImageReader mImageReader;
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Log.d(TAG, "OnImageAvail");
                    try {
                        mWrittenFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                        mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(),
                                mWrittenFile));
                        scanFile(mWrittenFile.getAbsolutePath());
                        makeToast("Photo saved: " + mWrittenFile.getAbsolutePath());
                        sendCameraBroadcast();


                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };

    private static class ImageSaver implements Runnable {
        private Image mImage;
        private File mFile;
        private ImageSaver(Image image, File file) {
            mImage = image;
            mFile=file;
        }


        @Override
        public void run() {
            Log.d(TAG, "ImageSaverRun");
            ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);

            FileOutputStream fileOutputStream = null;

            try {
                fileOutputStream = new FileOutputStream(mFile);
                fileOutputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally{

                mImage.close();
                if (fileOutputStream != null){
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0,90);
        ORIENTATIONS.append(Surface.ROTATION_90,0);
        ORIENTATIONS.append(Surface.ROTATION_180,270);
        ORIENTATIONS.append(Surface.ROTATION_270,180);
    }





    private static final String TAG = "PreviewFrag";


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.preview_frag, container, false);

        Log.d(TAG, "onCreateView");

        setHasOptionsMenu(true);

        initUIListeners(v);

        return v;
    }



    private void initUIListeners(View v) {
        Log.d(TAG, "initUI");
        initializeSeekBarVals();

        mTextureView = (AutoFitTextureView)v.findViewById(R.id.camera_preview);

        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);

        mSeekBarTextView = (TextView) v.findViewById(R.id.text_seekbar);
        mSeekBarTextView.setVisibility(View.INVISIBLE);

        mAutoWhiteBalSeek = (AutoWhiteBalSeekBar) v.findViewById(R.id.autowhitebal_seekbar);

        mLayoutBottom = (RelativeLayout) v.findViewById(R.id.bottom_layout);

        mLayoutAutoFoc = (LinearLayout) v.findViewById(R.id.focus_layout);
        mSwitchAutoFoc = (Switch) v.findViewById(R.id.focus_switch);
        SeekBar seekAutoFoc = (SeekBar) v.findViewById(R.id.focus_seekbar);

        mLayoutIso = (LinearLayout) v.findViewById(R.id.iso_layout);
        Switch switchIso = (Switch) v.findViewById(R.id.iso_switch);
        SeekBar seekIso = (SeekBar) v.findViewById(R.id.iso_seekbar);

        LinearLayout layoutZoom = (LinearLayout) v.findViewById(R.id.zoom_layout);
        SeekBar seekZoom = (SeekBar) v.findViewById(R.id.zoom_seekbar);

        LinearLayout layoutAutoExp = (LinearLayout) v.findViewById(R.id.autoexp_layout);
        Switch switchAutoExp = (Switch) v.findViewById(R.id.autoexp_switch);
        SeekBar seekAutoExp = (SeekBar) v.findViewById(R.id.autoexp_seekbar);

        LinearLayout layoutAutoWhiteBal = (LinearLayout) v.findViewById(R.id.autowhitebal_layout);

        mLayoutCapture = (RelativeLayout) v.findViewById(R.id.capture_layout);
        mCameraButton = (ImageView) v.findViewById(R.id.camera_button);
        mVideoButton = (ImageView) v.findViewById(R.id.video_button);
        ImageView btnFocus = (ImageView) v.findViewById(R.id.focus_button);
        ImageView btnAutoExp = (ImageView) v.findViewById(R.id.autoExp_button);
        ImageView btnAutoWhiteBal = (ImageView) v.findViewById(R.id.autowhitebal_button);
        ImageView btnIso = (ImageView) v.findViewById(R.id.iso_button);
        ImageView btnZoom = (ImageView) v.findViewById(R.id.zoom_button);
        ImageView btnSettings = (ImageView) v.findViewById(R.id.options_button);

        mCameraButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        if (inPicturePreview) {
                            try {
                                takePicture();
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        } else {
                            closeCamera();
                            releaseMediaRecorder();
                            releaseVideoPreview();
                            mCameraState = STATE_CAMERA;
                            mCameraButton.setImageResource(R.drawable.camera_btn_ctrl);
                            mVideoButton.setImageResource(R.drawable.video_btn);
                            try {

                                openCamera(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.i("setOnTouchListener", "MotionEvent.ACTION_DOWN");
                        break;
                    case MotionEvent.ACTION_UP:

                        if (inPicturePreview) {
                            continuePreview();
                        }
                        Log.i("setOnTouchListener", "MotionEvent.ACTION_UP");
                        break;
                }
                return true;
            }
        });

        btnSettings.setOnClickListener(this);
        btnFocus.setOnClickListener(this);
        btnZoom.setOnClickListener(this);
        btnIso.setOnClickListener(this);
        btnAutoExp.setOnClickListener(this);
        btnAutoWhiteBal.setOnClickListener(this);
        mVideoButton.setOnClickListener(this);

        CheckListener mCheckListener = new CheckListener();
        mSwitchAutoFoc.setOnCheckedChangeListener(mCheckListener);
        switchAutoExp.setOnCheckedChangeListener(mCheckListener);
        switchIso.setOnCheckedChangeListener(mCheckListener);

        mSwitchAutoFoc.setChecked(true);
        switchIso.setChecked(true);
        switchAutoExp.setChecked(true);

        SeekListener mSeekListener = new SeekListener();
        seekAutoFoc.setOnSeekBarChangeListener(mSeekListener);
        seekAutoExp.setOnSeekBarChangeListener(mSeekListener);
        seekZoom.setOnSeekBarChangeListener(mSeekListener);
        seekIso.setOnSeekBarChangeListener(mSeekListener);


        seekAutoFoc.setEnabled(false);
        seekIso.setEnabled(false);

        seekAutoFoc.setMax(100);
        seekIso.setMax(100);
        seekAutoExp.setMax(100);
        seekZoom.setMax(100);

        seekAutoFoc.setProgress(50);
        seekAutoExp.setProgress(50);
        seekIso.setProgress(50);
        seekZoom.setProgress(0);


        mLayoutList = new ArrayList<View>();
        mLayoutList.add(mLayoutBottom);//0
        mLayoutList.add(mLayoutAutoFoc);//1
        mLayoutList.add(layoutAutoExp);//2
        mLayoutList.add(layoutAutoWhiteBal);//3
        mLayoutList.add(mLayoutIso);//4
        mLayoutList.add(layoutZoom);//5

    }

    private void initializeSeekBarVals() {
        valueAF = 5.0f;
        valueAETime = (214735991 - 13231) / 2;
        valueISO = (800 - 50) / 2;
        valueAE = 0;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.video_button:

                if (inPicturePreview && !recording) {
                    mCameraState = STATE_VIDEO;
                    mVideoButton.setImageResource(R.drawable.video_btn_active);
                    mCameraButton.setImageResource(R.drawable.camera_btn);
                    releaseCameraPreview();
                    closeCamera();
                    try {
                        openCamera(mVideoPreviewSize.getWidth(), mVideoPreviewSize.getHeight());
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    createVideoPreviewSession();
                } else if (inVideoPreview && !recording) {
                    startRecordingVideo();
                    mVideoButton.setImageResource(R.drawable.video_btn_stop_active);
                } else if (inVideoPreview && recording) {
                    mVideoButton.setImageResource(R.drawable.video_btn_active);
                    stopRecordingVideo();
                }


                break;

            case R.id.focus_button:
                showAutoFocFlag = !showAutoFocFlag;
                showLayout(SHOW_AUTOFOC, showAutoFocFlag);
                break;

            case R.id.zoom_button:
                showZoomFlag = !showZoomFlag;
                showLayout(SHOW_ZOOM,showZoomFlag);
                break;
            case R.id.autoExp_button:
                showAutoExpFlag = !showAutoExpFlag;
                showLayout(SHOW_AUTOEXP, showAutoExpFlag);
                break;
            case R.id.autowhitebal_button:
                showAutoWhitBalFlag = !showAutoWhitBalFlag;
                showLayout(SHOW_AUTOWHITBAL, showAutoWhitBalFlag);
                break;
            case R.id.iso_button:
                showIsoFlag = !showIsoFlag;
                showLayout(SHOW_ISO, showIsoFlag);
                break;
            case R.id.options_button:
                Intent i = new Intent(getActivity(), PreferencesFragment.class);
                i.putExtra("cameraID", mCameraId);
                startActivity(i);

        }


    }

    private void showLayout(int showWhat, boolean showOrNot) {
        View v = mLayoutList.get(showWhat);
        if (showOrNot) {
            for (int i = 0; i < mLayoutBottom.getChildCount(); i++) {
                if (mLayoutBottom.getChildAt(i).getVisibility() == View.VISIBLE) {
                    mLayoutBottom.getChildAt(i).setVisibility(View.INVISIBLE);
                }
            }
            v.setVisibility(View.VISIBLE);
        } else {
            for (int i = 0; i < mLayoutBottom.getChildCount(); i++) {
                if (mLayoutBottom.getChildAt(i).getVisibility() == View.VISIBLE) {
                    mLayoutBottom.getChildAt(i).setVisibility(View.INVISIBLE);
                }
            }
            mLayoutCapture.setVisibility(View.VISIBLE);
        }
    }


    private class SeekListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            if (mPreviewBuilder == null || getView() == null) {
                return;
            }
            switch (seekBar.getId()) {
                case R.id.focus_seekbar:
                    float minimumLens = mCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
                    float num = (((float) i) * minimumLens / 100);
                    mPreviewBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, num);
                    int showNum = (int) num;
                    mSeekBarTextView.setText("Focus：" + showNum);
                    break;
                case R.id.zoom_seekbar:
                    Rect rect = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
//                    int maxZoom = mCameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM).intValue();

                    //forced max zoom, camera characteristics picked out 4x for some reason...!!!
                    int maxZoom = 8;

                    int minL = (rect.width() - (rect.width() / maxZoom)) / 2;
                    int minR = rect.width() - minL;
                    int minT = (rect.height() - (rect.height() / maxZoom)) / 2;
                    int minB = rect.height() - minT;
                    Rect checkRect = new Rect(minL, minT, minR, minB);

                    Rect newRect = new Rect(i * minL / 100,
                            i * minT / 100,
                            rect.width() - (rect.width() - minR) * i / 100,
                            rect.height() - (rect.height() - minB)*i / 100);

                    if (newRect.height() * newRect.width() < checkRect.width() * checkRect.height()) {
                        makeToast("Zoom Error!");
                        return;
                    }
                    mPreviewBuilder.set(CaptureRequest.SCALER_CROP_REGION, newRect);
                    mSeekBarTextView.setText("Zoom：" + i + "%");
                    break;
                case R.id.autoexp_seekbar:
                    Switch switchAutoExp = (Switch) getView().findViewById(R.id.autoexp_switch);
                    if (switchAutoExp.isChecked()) {
                        Range<Integer> range1 = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
                        int maxAE = range1.getUpper();
                        int minAE = range1.getLower();
                        int all = (-minAE) + maxAE;
                        int time = 100 / all;
                        int ae = ((i / time) - maxAE) > maxAE ? maxAE : ((i / time) - maxAE) < minAE ? minAE : ((i / time) - maxAE);
                        mPreviewBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, ae);
                        mSeekBarTextView.setText("Exposure Compensation：" + ae);
                        valueAE = ae;
                    }
                    //TODO fix exposure
//                    else {
//                        Range<Long> range = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
//                        long max = range.getUpper();
//                        long min = range.getLower();
//                        long ae = ((i * (max - min)) / 100 + min);
//                        mPreviewBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, ae);
//                        mSeekBarTextView.setText("Exposure Time：" + ae);
//                        valueAETime = ae;
//                    }
                    break;
                case R.id.iso_seekbar:
                    Range<Integer> range = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
                    int max1 = range.getUpper();
                    int min1 = range.getLower();
                    int iso = ((i * (max1 - min1)) / 100 + min1);
                    mPreviewBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, iso);
                    valueISO = iso;
                    mSeekBarTextView.setText("Iso：" + iso);
                    break;

            }
            updatePreview();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mSeekBarTextView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mSeekBarTextView.setVisibility(View.INVISIBLE);
        }


    }

    private class CheckListener implements CompoundButton.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (mPreviewBuilder == null || getView() == null) {
                return;
            }
            switch (buttonView.getId()) {
                case R.id.focus_switch:
                    if (isChecked) {
                        mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
                        mLayoutAutoFoc.getChildAt(1).setEnabled(false);
                    } else {
                        mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);
                        makeToast("control mode off!");
                        mLayoutAutoFoc.getChildAt(1).setEnabled(true);
                    }
                    break;
                case R.id.autoexp_switch:
                    Switch switchISO = (Switch) getView().findViewById(R.id.iso_switch);
                    switchISO.setChecked(isChecked);
                    //exposure compensation
                    if (isChecked) {
                        mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                        mLayoutIso.getChildAt(1).setEnabled(false);
                    } else {
                        lastValueAEComp = mPreviewBuilder.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION);
                        lastValueAETime = mPreviewBuilder.get(CaptureRequest.SENSOR_EXPOSURE_TIME);

                        Log.d(TAG, "AEcomp: " + lastValueAEComp);
                        Log.d(TAG, "AEtime: " + lastValueAETime);

                        mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
                        mPreviewBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,lastValueAEComp);
                        mPreviewBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME,lastValueAETime);


                        mLayoutIso.getChildAt(1).setEnabled(true);

                    }
                    break;
                case R.id.iso_switch:
                    Switch switchAutoExp = (Switch) getView().findViewById(R.id.autoexp_switch);
                    switchAutoExp.setChecked(isChecked);
                    if (isChecked) {
                        mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                        mLayoutIso.getChildAt(1).setEnabled(false);
                    } else {
                        mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
                        mLayoutIso.getChildAt(1).setEnabled(true);
                    }
                    break;
            }
            updatePreview();
        }
    }








    //-------------------------------------------------------//
    //------------- CAMERA AND VIDEO MANAGEMENT--------------//
    //-------------------------------------------------------//

    private TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    Log.d(TAG, "onSurfaceTextAvailable");


                    if(!mCameraConfig) {
                        try {
                            openCamera(width, height);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }
                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                    Log.d(TAG, "onSurfaceTextChanged");

                    if (MainActivity.inPreview && !mCameraConfig) {
                        try {
                            openCamera(width, height);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }else if (MainActivity.inPreview && mCameraConfig) {
                        configureTransform(width, height);
                    }

                }
                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    Log.d(TAG, "onSurfaceTextDestroyed");

                    closeCamera();
                    releaseMediaRecorder();
                    return true;
                }
                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                }
            };
    /**
     * Sets up size configurations for camera
     *
     * @param width  The width of set size from camera preview
     * @param height The height of set size from camera preview
     */
    private void setUpCameraOutputs(int width, int height) throws CameraAccessException {
        mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);

        StreamConfigurationMap map = mCameraCharacteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        mLargestImageSize = Collections.max(
                Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                new CompareSizesByArea());


        // Don't want to have it as too large...bandwidth issues
        mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                width, height, mLargestImageSize);

        mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));

        mVideoPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width,
                height, mVideoSize);

//        int orientation = getResources().getConfiguration().orientation;
//        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            mTextureView.setAspectRatio(
//                    mPreviewSize.getWidth(), mPreviewSize.getHeight());
//        } else {
//            mTextureView.setAspectRatio(
//                    mPreviewSize.getHeight(), mPreviewSize.getWidth());
//        }


    }


    /**
     * Input choices of size for video recording
     * Size must be less than 1080 because of issues we had with reading 4k,
     * TODO can probably fix and force higher res
     *
     * @param choices     List of sizes from media recorder that are supported
     *
     * @return Best size given parameters, or arbitrary size
     */
    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 16 / 9 && size.getHeight() <= 1080) {
                return size;
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }
    /**
     * Input choices of size from cameraChar, chooses the smallest one whose
     * width and height are at least as large as the desired values, while maintaining
     * the aspect ratio
     *
     * @param choices     List of sizes from camera characteristics supported
     * @param width       Minimum desired width
     * @param height      Minimum desired height
     * @param aspectRatio Aspect ratio
     * @return Best size given parameters, or arbitrary size
     */
    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming found
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }
    /**
     * Configures transform to mTextureView
     * Call this after preview size is found and mTextureView is set
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Log.d(TAG, "ConfigTrans");
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }
    private Size getPreferredPreviewSize(Size[] mapSizes, int width, int height) {
        List<Size> collectorSizes = new ArrayList<>();
        //cycle through map sizes, width and height will be passed as if in landscape
        for(Size option : mapSizes) {
            if(width > height) {
                //check if bigger than texture view requested width
                if(option.getWidth() > width &&
                        option.getHeight() > height) {
                    collectorSizes.add(option);
                }
            } else {
                if(option.getWidth() > height &&
                        option.getHeight() > width) {
                    collectorSizes.add(option);
                }
            }
        }
        if(collectorSizes.size() > 0) {
            return Collections.min(collectorSizes, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return mapSizes[0];
    }


    /**
     * Setup camera to be openedas well as begin session to open camera based on
     * parameters. Handlded on background
     *
     * @param width       Width of surface
     * @param height      Height of surface
     */
    private void openCamera(int width, int height) throws  CameraAccessException, InterruptedException{
        Log.d(TAG, "Open Camera");
        mCameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);

        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        initOutputSurface();

        if (mCameraState != STATE_VIDEO) {
            mCameraState = STATE_CAMERA;
            mCameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
        } else {
            mMediaRecorder = new MediaRecorder();
            mCameraState = STATE_VIDEO;
            mCameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
        }
        mCameraConfig = true;

    }
    private void initOutputSurface() {

        mImageReader = ImageReader.newInstance(mLargestImageSize.getWidth(), mLargestImageSize.getHeight(),
                ImageFormat.JPEG,2);
        mImageReader.setOnImageAvailableListener(
                mOnImageAvailableListener, mBackgroundHandler);

        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        mSurface = new Surface(texture);
        mOutputSurfaces = new ArrayList<Surface>(2);
        mOutputSurfaces.add(mImageReader.getSurface());
        mOutputSurfaces.add(mSurface);
    }

    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onClosed(CameraDevice camera) {
            Log.i("CameraStateCallback", "onClosed");
        }

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            Log.i("Thread", "onOpened---->" + Thread.currentThread().getName());
            Log.i("CameraStateCallback", "onOpened");

            mCameraDevice = cameraDevice;

            if (mCameraState!=STATE_VIDEO) {
                startPreview();
            }else createVideoPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.i("Camera=StateCallback", "onDisconnected");
            Toast.makeText(getActivity(), "onDisconnected", Toast.LENGTH_SHORT).show();

            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            Log.i(TAG, "Camera=StateCallbackError");
            Toast.makeText(getActivity(), "onError", Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * Begins capture request for a preview from the camera onto selected surfaces
     * Capture session and callback handled on background
     */
    //TODO add in camera permission check
    //start camera preview
    private void startPreview() {
        Log.d(TAG, "startPreview");
        try {
            releaseVideoPreview();
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //TEMPLATE_PREVIEW
            mPreviewBuilder.addTarget(mSurface);
            initPreviewBuilder();
            mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_AUTO);
            mCameraState = STATE_CAMERA;
//            mCameraDevice.createCaptureSession(Arrays.asList(mSurface), mSessionPreviewStateCallback, mHandler);
            mCameraDevice.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface()), mSessionPreviewStateCallback, mBackgroundHandler);


            inPicturePreview = true;
            inVideoPreview = false;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void initPreviewBuilder() {
        Log.d(TAG, "initPrevBuilder");
        mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_OFF);
        mPreviewBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, valueAF);
        mPreviewBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, valueAETime);
        mPreviewBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, valueAE);
        mPreviewBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, valueISO);
    }


    private CameraCaptureSession.StateCallback mSessionPreviewStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            Log.i("Thread", "onConfigured---->" + Thread.currentThread().getName());
            Log.i(TAG, "mSessionStateCallback--->onConfigured");

            if (mCameraDevice == null) {
                return;
            }
            try {

                mPreviewCaptureRequest = mPreviewBuilder.build();
                mCameraCaptureSession = cameraCaptureSession;
                //continuous images from camera
                mCameraCaptureSession.setRepeatingRequest(
                        mPreviewCaptureRequest,
                        mSessionCaptureCallback,
                        mBackgroundHandler
                );
                mAutoWhiteBalSeek.setmOnAwbSeekBarChangeListener(new AutoWhiteBalSeekListener(getActivity(), mSeekBarTextView, mPreviewBuilder, mCameraCaptureSession,
                        mBackgroundHandler, mSessionCaptureCallback));
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
            Log.i(TAG, "mSessionStateCallback--->onConfigureFailed");
//            Toast.makeText(getApplicationContext(), "onConfigureFailed---Preview", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onReady(@NonNull CameraCaptureSession session) {
            super.onReady(session);
            Log.i(TAG, "mSessionStateCallback--->onReady");
        }

        @Override
        public void onActive(@NonNull CameraCaptureSession session) {
            super.onActive(session);
            Log.i(TAG, "mSessionStateCallback--->onActive");
        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
            super.onClosed(session);
            Log.i(TAG, "mSessionStateCallback--->onClosed");
        }
    };


    private void takePicture() throws CameraAccessException {
        Log.d(TAG, "TakePicture");


        mCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG);
        mCaptureBuilder.addTarget(mImageReader.getSurface());


        mCaptureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//
//
//        Range<Integer> fps[] = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
//        mCaptureBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fps[fps.length - 1]);
        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
        previewBuilder2CaptureBuilder();
        mCameraState = STATE_CAPTURE;

        mCameraDevice.createCaptureSession(mOutputSurfaces, mSessionCaptureStateCallback, mBackgroundHandler);
//        sendCameraBroadcast();
    }

    private void previewBuilder2CaptureBuilder() {
        //AUTO WHITE BALENCE

        if (mPreviewBuilder.get(CaptureRequest.CONTROL_AWB_MODE) == CameraMetadata.CONTROL_AWB_MODE_OFF) {
            mCaptureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_OFF);
            mCaptureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, mPreviewBuilder.get(CaptureRequest.CONTROL_AWB_MODE));
        }

//        //AUTO EXPOSURE
        if (mPreviewBuilder.get(CaptureRequest.CONTROL_AE_MODE) == CameraMetadata.CONTROL_AE_MODE_OFF) {
            mCaptureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
            mCaptureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, mPreviewBuilder.get(CaptureRequest.SENSOR_EXPOSURE_TIME));
        }else if (mPreviewBuilder.get(CaptureRequest.CONTROL_AE_MODE) == CameraMetadata.CONTROL_AE_MODE_ON) {
            mCaptureBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, mPreviewBuilder.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION));
        }

        //AUTO FOCUS
        if (mPreviewBuilder.get(CaptureRequest.CONTROL_AF_MODE) == CameraMetadata.CONTROL_AF_MODE_OFF) {
            mCaptureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);
            mCaptureBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, mPreviewBuilder.get(CaptureRequest.LENS_FOCUS_DISTANCE));

        }

//        mCaptureBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, mPreviewBuilder.get(CaptureRequest.CONTROL_EFFECT_MODE));

        //ISO
        mCaptureBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, mPreviewBuilder.get(CaptureRequest.SENSOR_SENSITIVITY));

        mCaptureBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, mPreviewBuilder.get(CaptureRequest.CONTROL_AF_REGIONS));
        mCaptureBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, mPreviewBuilder.get(CaptureRequest.CONTROL_AE_REGIONS));
//        mCaptureBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, mPreviewBuilder.get(CaptureRequest.CONTROL_SCENE_MODE));
//        //zoom
        mCaptureBuilder.set(CaptureRequest.SCALER_CROP_REGION, mPreviewBuilder.get(CaptureRequest.SCALER_CROP_REGION));

    }


    private void continuePreview() {
        mCameraState = STATE_CAMERA;
        try {
            mCameraDevice.createCaptureSession(Arrays.asList(mSurface), mSessionPreviewStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);


//            process(result);
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);

            makeToast("Focus failed");


        }
    };
    private CameraCaptureSession.StateCallback mSessionCaptureStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.d(TAG, "CaptureStateConfigured");
            try {
                CameraCaptureSession.CaptureCallback captureCallback =
                        new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                                super.onCaptureCompleted(session, request, result);

                            }
                        };


                session.capture(mCaptureBuilder.build(), captureCallback, null);


            } catch (CameraAccessException e) {
                Log.e(TAG, "CaptureStateError");
                e.printStackTrace();
            } catch (Exception e) {
                Log.e(TAG, "CaptureStateError");
//                Toast.makeText(getApplicationContext(), "onConfigured,Session closed", Toast.LENGTH_SHORT).show();
            }

        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.e(TAG, "CaptureSessionStateConfigFailed");
//            Toast.makeText(getApplicationContext(), "onConfigureFailed---Capture", Toast.LENGTH_SHORT).show();
        }
    };




    private void updatePreview() {

        try {
            if (mCameraState == STATE_VIDEO) {
                mVideoCaptureSession.setRepeatingRequest(mPreviewBuilder.build(),null, mBackgroundHandler);
            }else {
                mCameraCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), mSessionCaptureCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        mCameraConfig = false;

    }


    private void createVideoPreviewSession() {
        Log.d(TAG, "CreateVidPrev");
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            releaseCameraPreview();
            inVideoPreview = true;
            try {
                setupMediaRecorder();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
                e.printStackTrace();
            }
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mVideoPreviewSize.getWidth(), mVideoPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

            List<Surface> surfaces = new ArrayList<Surface>();
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);


            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewBuilder.addTarget(recorderSurface);

            startVideoPreview();

            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.d(TAG, "VideoOnConfig");
                    mVideoCaptureSession = cameraCaptureSession;

                    try {
                        mVideoCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
                        inVideoPreview = true;

                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(getActivity().getApplicationContext(), "Failed", Toast.LENGTH_SHORT).show();

                }

                @Override
                public void onClosed(@NonNull CameraCaptureSession session) {
                    super.onClosed(session);
                    Log.d(TAG, "onVideoClosed");

                }
            }, mBackgroundHandler);



        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void startVideoPreview() {
        Log.d(TAG, "startPreview");
            //TEMPLATE_PREVIEW
//            mPreviewBuilder.addTarget(mSurface);
            initPreviewBuilder();
            mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_AUTO);
            mCameraState = STATE_VIDEO;
            //create capture session


            inPicturePreview = false;
            inVideoPreview = true;

    }


    private void setupMediaRecorder() throws IOException {

        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        }


        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        //todo fix setup so temp file isn't created
        try {
            mVideoFile = getOutputMediaFile(MEDIA_TYPE_VIDEO);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        if(mVideoFile != null) {
            mMediaRecorder.setOutputFile(mVideoFile.getAbsolutePath());
        } else Log.d(TAG, "ErrorVideoFile");
//        mMediaRecorder.setOutputFile(getVideoFile(getActivity()).getAbsolutePath());
//        mMediaRecorder.setVideoEncodingBitRate(10000000);
//        mMediaRecorder.setVideoFrameRate(30);
//        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
//        mMediaRecorder.setVideoSize(3840,2160);
//        mMediaRecorder.setVideoSize(1920, 1080);
//        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        int orientation = ORIENTATIONS.get(rotation);
        mMediaRecorder.setOrientationHint(orientation);

        mMediaRecorder.prepare();

        Log.e(TAG, "MediaRecorderPrepare");

    }

    private void startRecordingVideo () {
        Log.d(TAG, "StartRecording");
        // UI
        recording = true;

//        try {
//            mCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//
//
//        mCaptureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//
//        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
//        mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
//        previewBuilder2CaptureBuilder();
//        mCameraState = STATE_CAMERA;


        // Start recording
        mMediaRecorder.start();

    }

    private void stopRecordingVideo() {
        Log.d(TAG, "StopRecording");
//        try {
//            //abort pending captures
//            mVideoCaptureSession.abortCaptures();
//        }catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
        recording = false;
        // Stop recording
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        scanFile(mVideoFile.getAbsolutePath());
        makeToast("Video saved: " + mVideoFile.getAbsolutePath());
        sendCameraBroadcast();


//        releaseMediaRecorder();
        createVideoPreviewSession();

    }


    private void releaseMediaRecorder(){
        if (recording) {
            mMediaRecorder.stop();
            recording = false;
        }

        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            //mCameraDevice.lock();           // lock camera for later use
            recording = false;
        }

    }

    private void releaseVideoPreview () {
        if (mVideoCaptureSession != null) {
            mVideoCaptureSession.close();
            mVideoCaptureSession = null;
        }
        inVideoPreview = false;
    }

    private void releaseCameraPreview () {
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader =  null;
        }
        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        inPicturePreview = false;

    }

    private void closeBackgroundThread() {
        //block UI thread until this is shutdown

        try {
            if (mBackgroundThread != null) {
                mBackgroundThread.quitSafely();
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void openBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera2 background thread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }


    private void scanFile(String path) {

        MediaScannerConnection.scanFile(getActivity(),
                new String[]{path}, null,
                new MediaScannerConnection.OnScanCompletedListener() {

                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("TAG", "Finished scanning " + path);
                    }
                });
    }

    public static Uri getOutputMediaFileUri(int type){
        try {
            return Uri.fromFile(getOutputMediaFile(type));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Create a File for saving an image or video */
    public static File getOutputMediaFile(int type) throws IOException{
        // using Environment.getExternalStorageState() before doing this.

        File directory = new File(Environment.getExternalStorageDirectory() + File.separator +
                "Maglev");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! directory.exists()){
            if (! directory.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(directory.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(directory.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        mImageFileLocation = mediaFile.getAbsolutePath();
        return mediaFile;
    }


    private void makeToast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
    }

    private void sendCameraBroadcast() {
        Intent i = new Intent(GalleryViewFrag.CAMERA_ACTION);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(i);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "OnAttach");


    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        closeCamera();
        closeBackgroundThread();
        releaseMediaRecorder();
    }

    @Override
    public void onPause() {
        super.onPause();

        closeCamera();
        closeBackgroundThread();
        releaseMediaRecorder();

    }

    @Override
    public void onResume() {
        super.onResume();

        openBackgroundThread();

    }
}
