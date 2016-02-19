package com.alexhart.maglev2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;

import com.alexhart.maglev2.Grapher.HistogramGenerator;
import com.alexhart.maglev2.ImageProcessor.ImageProcessor;
import com.androidplot.xy.XYPlot;
import com.github.mikephil.charting.charts.BarChart;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;


/**
 * Created by Chung on 12/28/2015.
 */
public class HistogramFrag extends Fragment implements View.OnClickListener {

    private MainActivity hostActivity;
    private Context context;
    private Button process_button;
    private Button cancel_button;
    private ImageProcessor imageP;
    private View v;
    private HistogramGenerator hg;
    private ProgressBar pgb;
    private Display display;
    private int detectionMethod;
    private final CharSequence[] options = {"FloodFill", "Hough Circle"};

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
        display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        v = inflater.inflate(R.layout.histogram_frag, container, false);
        pgb = (ProgressBar) v.findViewById(R.id.progressBar);
        pgb.setVisibility(View.INVISIBLE);
        alertDialog = new AlertDialog.Builder(hostActivity).create();
        process_button = (Button) v.findViewById(R.id.process_button);
        process_button.setOnClickListener(this);
        cancel_button = (Button) v.findViewById(R.id.stop_button);
        cancel_button.setOnClickListener(this);
        XYPlot plot = (XYPlot) v.findViewById(R.id.plot);
        //BarChart barChart = (BarChart) v.findViewById(R.id.barchart);
        hg = new HistogramGenerator(plot);
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
                pgb.setVisibility(View.VISIBLE);
                imageP = new ImageProcessor(hostActivity.getdatafile(),v,hostActivity.isVideo(),hg,display,detectionMethod);
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
                AlertDialog.Builder builder = new AlertDialog.Builder(hostActivity);
                builder.setTitle("Choose a Detection Method");
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (options[which].equals("FloodFill")) {
                            detectionMethod = 0;
                        } else if (options[which].equals("Hough Circle")) {
                            detectionMethod = 1;
                        }
                        if( hostActivity.getdatafile() != null){
                            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, hostActivity, process);
                        }
                        else{
                            alertDialog.setMessage("Please select a picture or video first");
                            alertDialog.show();
                        }
                    }
                });
                builder.show();

                break;
            case R.id.stop_button:
                if(imageP != null){
                    pgb.setVisibility(View.INVISIBLE);
                    imageP.stopThread();
                    imageP = null;
                }
                else{
                    alertDialog.setMessage("Process is not in progress");
                    alertDialog.show();
                }

        }

    }
}
