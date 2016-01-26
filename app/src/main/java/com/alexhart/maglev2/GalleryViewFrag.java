package com.alexhart.maglev2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;

import com.alexhart.maglev2.Adapters.GalleryAdapter;
import com.alexhart.maglev2.Adapters.RecyclerViewAdapterPositionInter;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;


/**
 * Fragment of pager to control showing user gallery of images/video
 * Immediately updates after image/video are taken
 */
public class GalleryViewFrag extends Fragment implements RecyclerViewAdapterPositionInter {

    private final static String TAG = "GalleryViewFrag";
    private File mGalleryDirectory;
    private RecyclerView mRecyclerView;
    private File mSelectedFile;

    private RelativeLayout mVideoFrame;
    private ImageView mSingleImageView;
    private VideoView mVideoView;
    private MediaController mMediaController;

    private static int mViewColumn = 3;
    private static int mImageWidth;
    private static int mImageHeight;

    public static final String CAMERA_ACTION = "com.alexhart.maglev2.galleryview.CAMERA_ACTION";

    private float mFingerDist;
    private double mPinchCount;

    private OnFragmentInteractionListener mListener;




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_gallery_view, container, false);



        createGallery();
        initViewAdapters(v);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mImageWidth = displayMetrics.widthPixels / mViewColumn;
        //16:9 ratio HARDCODED!!
        mImageHeight = mImageWidth * 16 / 9;

        mRecyclerView = (RecyclerView)v.findViewById(R.id.galleryRecycler);
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), mViewColumn);
        mRecyclerView.setLayoutManager(layoutManager);
        RecyclerView.Adapter galleryAdapter = new GalleryAdapter(sortFiles(mGalleryDirectory),
                mImageWidth, mImageHeight, this);
        mRecyclerView.setAdapter(galleryAdapter);


        return v;
    }

    private void initViewAdapters(View v) {

        mVideoFrame = (RelativeLayout) v.findViewById(R.id.videoFrame);
        mSingleImageView = (ImageView) v.findViewById(R.id.imageView);
        mVideoView = (VideoView) v.findViewById(R.id.videoView);


        mSingleImageView.setOnTouchListener(mFullViewTouchListener);
        mVideoView.setOnTouchListener(mFullViewTouchListener);
    }

    private View.OnTouchListener mFullViewTouchListener = new View.OnTouchListener(){

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            int action = event.getAction();

            if (event.getPointerCount() > 1) {
                if (action == MotionEvent.ACTION_POINTER_DOWN) {
                    mFingerDist = getFingerSpacing(event);
                } else if (action == MotionEvent.ACTION_MOVE) {
                    handlePinch(event);
                }
            } else {
                if (action == MotionEvent.ACTION_UP) {
//                    mMediaController.show();
                }
            }

            return true;
        }
    };

    private void handlePinch(MotionEvent event) {

        float newDist = getFingerSpacing(event);

        if (newDist > mFingerDist) {
            //handle zooming into picture
        }else if (newDist < mFingerDist) {
            //exit to recycler view
            mPinchCount++;
            //how far user has to zoom to switch ui (pix size)
            if (mPinchCount > 10) {
                mSingleImageView.setVisibility(View.GONE);
                mSingleImageView.setImageResource(0);
                mVideoView.stopPlayback();
                mVideoFrame.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
                mPinchCount = 0;
            }
        }
        mFingerDist = newDist;
    }

    /** Determine the space between the first two fingers */
    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }

    private void createGallery() {
        mGalleryDirectory = new File(Environment.getExternalStorageDirectory() + File.separator +
                "Maglev");

        if (!mGalleryDirectory.exists()) {
            mGalleryDirectory.mkdirs();
        }


    }

    private File[] sortFiles(File fileDirectory) {
        File[] files = fileDirectory.listFiles();
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                return Long.valueOf(rhs.lastModified()).compareTo(lhs.lastModified());
            }
        });
        return files;
    }

    @Override
    public void getRecyclerViewAdapterPosition(int position) {
        mSelectedFile = sortFiles(mGalleryDirectory)[position];
        openFullscreenImage(mSelectedFile);
    }

    private void openFullscreenImage (File file) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width,height);
        mSingleImageView.setLayoutParams(params);

        if (file == null) {
            Log.e(TAG, "File Read Error");
            return;
        }
        if (file.getName().contains("jpg") || file.getName().contains("jpeg")) {
            mRecyclerView.setVisibility(View.GONE);
            mSingleImageView.setVisibility(View.VISIBLE);

            SingleImageWorkerTask workerTask = new SingleImageWorkerTask(mSingleImageView,mListener, width,
                    height);
            workerTask.execute(file);

        }else {
            mRecyclerView.setVisibility(View.GONE);
            mVideoFrame.setVisibility(View.VISIBLE);

            try {

                //TODO fix media controller/make own
                mVideoView.setVideoURI(Uri.fromFile(file));
                mListener.onFragmentInteraction(file,true);
                mVideoView.requestFocus();
            } catch (Exception e) {
                e.printStackTrace();
                mRecyclerView.setVisibility(View.VISIBLE);
                mVideoFrame.setVisibility(View.GONE);
            }
            mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                // Close the progress bar and play the video
                public void onPrepared(MediaPlayer mp) {
                    try {
                        mVideoView.start();
                    } catch (Exception e) {
                        e.printStackTrace();

                        mRecyclerView.setVisibility(View.VISIBLE);
                        mVideoFrame.setVisibility(View.GONE);
                    }
                }
            });

            mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    try {
                        mediaPlayer.stop();
                        mediaPlayer.reset();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mVideoFrame.setVisibility(View.GONE);
                    mRecyclerView.setVisibility(View.VISIBLE);
                }
            });

            mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                    Log.d(TAG, "ERROR!!" + i + i1);

                    //file read error
                    if (i == 1 && i1 == 0) {
                        createPhotoDeleteAlart(mSelectedFile);
                    }

                    return true;
                }
            });
        }
    }

    private final BroadcastReceiver mCameraUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Broadcast Received");
            swapImageAdapter();
        }
    };


    private void swapImageAdapter () {

        if (mRecyclerView != null){

            RecyclerView.Adapter newGalleryAdapter = new GalleryAdapter(sortFiles(mGalleryDirectory),
                    mImageWidth, mImageHeight, this);
            mRecyclerView.swapAdapter(newGalleryAdapter, false);
        }

    }

    private void createPhotoDeleteAlart(final File file) {

        final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle("Delete File?");
        alert.setMessage("Error in reading file: " + file.getAbsolutePath());

        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //Handle click of the 'yes' button
                try {
                    file.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Toast.makeText(getActivity(), "File Deleted", Toast.LENGTH_SHORT).show();
                swapImageAdapter();

                mVideoFrame.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);

            }
        });
        alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mVideoFrame.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
            }
        });
        alert.show();
    }

    @Override
    public void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mCameraUpdateReceiver, new IntentFilter(CAMERA_ACTION));

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Activity activity;
        if(context instanceof Activity){
            activity = (Activity) context;
            try{
                mListener = (OnFragmentInteractionListener) activity;
            } catch(ClassCastException e) {
                throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mCameraUpdateReceiver);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    public interface OnFragmentInteractionListener{
        public void onFragmentInteraction(File file,boolean isVideo);
    }
}
