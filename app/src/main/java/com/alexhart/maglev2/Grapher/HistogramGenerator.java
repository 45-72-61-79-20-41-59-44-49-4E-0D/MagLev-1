package com.alexhart.maglev2.Grapher;

import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.Log;

import com.alexhart.maglev2.ImageProcessor.Object;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepFormatter;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;

import java.text.DecimalFormat;
import java.util.Arrays;

/**
 * Created by Chung on 1/6/2016.
 */
public class HistogramGenerator{

    private final static String TAG = "HistogramGenerator";
    private XYPlot plot;
    private Number[] normalizeddata;
    private Number[] data;
    private int totalCount;
    private int center;
    private int topLine;
    private int bottomLine;
    LineAndPointFormatter series1Format;
    Paint lineFill;
    StepFormatter stepFormatter;

    public HistogramGenerator(XYPlot plot) {
        totalCount = 0;
        this.plot = plot;
    }
    private void graph(){

        // setup our line fill paint to be a slightly transparent gradient:
        lineFill = new Paint();
        lineFill.setAlpha(200);
        lineFill.setShader(new LinearGradient(0, 0, 0, 250, Color.WHITE, Color.BLUE, Shader.TileMode.MIRROR));

        stepFormatter  = new StepFormatter(Color.rgb(0, 0,0), Color.BLUE);
        stepFormatter.getLinePaint().setStrokeWidth(1);

        stepFormatter.getLinePaint().setAntiAlias(false);
        stepFormatter.setFillPaint(lineFill);
        this.plot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 0.1);
        this.plot.setRangeValueFormat(new DecimalFormat("0.0"));
        this.plot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 1);
        this.plot.setTicksPerDomainLabel(5);
    }
    public void update(Object detectedBeads){
        int location = detectedBeads.getY();
        if(location < topLine && location > bottomLine) {
            int data_location = (location - bottomLine) / data.length;
            int old_data = data[data_location].intValue();
            totalCount++;
            data[data_location] = old_data + 1;
            double new_data =data[data_location].doubleValue();
            normalizeddata[data_location] = new_data / totalCount;
            // create our series from our array of nums:
            XYSeries series2 = new SimpleXYSeries(
                    Arrays.asList(normalizeddata),
                    SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,
                    "1");
            plot.clear();
            graph();
            plot.addSeries(series2, stepFormatter);
            plot.redraw();
        }
    }

    public void resetData(){
        for(int i = 0; i < data.length; i++){
            data[i] = 0;
            normalizeddata[i] = 0;

        }
//        if(plot != null) {
//            plot.clear();
//        }
        totalCount = 0;
    }

    public void setCenter(int center){
        this.center = center;
    }
    public void setDetectionArea(int topLine,int bottomLine){
        this.topLine = topLine;
        this.bottomLine = bottomLine;
        this.data = new Number[20];
        this.normalizeddata = new Number[20];
        resetData();
    }
}

