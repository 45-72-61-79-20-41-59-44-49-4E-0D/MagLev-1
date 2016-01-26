package com.alexhart.maglev2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.media.Image;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.FloatMath;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.alexhart.maglev2.Grapher.HistogramGenerator;
import com.alexhart.maglev2.ImageProcessor.ImageProcessor;
import com.alexhart.maglev2.MainActivity;
import com.androidplot.xy.XYPlot;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;


/**
 * Created by Chung on 12/28/2015.
 */
public class HistogramFrag extends Fragment implements View.OnClickListener {

    private Bitmap result;
    private MainActivity hostActivity;
    private Context context;
    private ImageView imageV;
    private Button process_button;
    private Button cancel_button;
    private ImageProcessor imageP;
    private View v;

    private AlertDialog alertDialog;

    private final static String TAG = "HistogramFrag";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        v = inflater.inflate(R.layout.histogram_frag, container, false);
        ImageView imgv = (ImageView) v.findViewById(R.id.resultView);
        imgv.getLayoutParams().width = width;
        imgv.getLayoutParams().height = height;
        imgv.requestLayout();
        alertDialog = new AlertDialog.Builder(hostActivity).create();
        process_button = (Button) v.findViewById(R.id.process_button);
        process_button.setOnClickListener(this);
        cancel_button = (Button) v.findViewById(R.id.stop_button);
        cancel_button.setOnClickListener(this);
        //XYPlot plot = (XYPlot) v.findViewById(R.id.plot);
        //HistogramGenerator hg = new HistogramGenerator(plot,1,context);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        if(context instanceof Activity){
            hostActivity = (MainActivity) context;
        }
    }

    private BaseLoaderCallback process = new BaseLoaderCallback(hostActivity) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                // now we can call opencv code !
                imageP = new ImageProcessor(hostActivity.getdatafile(),v,hostActivity.isVideo());
                imageP.start();
            }
            else {
                super.onManagerConnected(status);
            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.process_button:
                if( hostActivity.getdatafile() != null){
                    Log.d(TAG,"START 3");
                    OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, hostActivity, process);
                }
                else{
                    alertDialog.setMessage("Please select a picture or video first");
                    alertDialog.show();
                }
                break;
            case R.id.stop_button:
                if(imageP != null){
                    imageP.stopThread();
                }
                else{
                    alertDialog.setMessage("Process is not in progress");
                    alertDialog.show();
                }

        }

    }
}
