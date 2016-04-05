package com.alexhart.maglev2.Grapher;

import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.alexhart.maglev2.ImageProcessor.Object;
import com.androidplot.ui.SeriesRenderer;
import com.androidplot.xy.BarFormatter;
import com.androidplot.xy.BarRenderer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;


import org.opencv.core.Point;

/*
Depends on the histogram resolution (affected by variable domainInterval), The actual calculated
magnet lines will be different from how it is actual represented in the histogram. For example,
if the resolution is 5 pixels,and a magnet line is calculated as 658 pixels from the bottom of
the image, it will be interpreted as 111 in the algorithm that generates the histogram. Changing
the resolution is not recommended because it might potentially lead to different array index.
The current version does not take into account for different histogram resolution.
 */

public class HistogramGenerator{

    private final static String TAG = "HistogramGenerator";
    private XYPlot plot;
    private Number[] normalizeddata;
    private Number[] data;
    private Number[] gaussianFit;
    private int totalCount;
    private int topLine;
    private int bottomLine;
    private int domainInterval;
    private int centerLine;
    private int counter;
    private ArrayList<Integer> dataLocation;
    private MyBarFormatter barformatter1;
    private MyBarRenderer barRenderer1;
    private LineAndPointFormatter lineFormatter;

    public HistogramGenerator(XYPlot plot) {
        totalCount = 0;
        counter = 0;
        domainInterval = 5;
        barformatter1 = new MyBarFormatter(Color.rgb(0,255,0), Color.LTGRAY);
        this.plot = plot;
        this.plot.setTicksPerRangeLabel(1);
        this.plot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 0.1);
        this.plot.setRangeValueFormat(new DecimalFormat("0.0"));
        this.plot.setRangeLowerBoundary(0, BoundaryMode.FIXED);
        this.plot.getGraphWidget().getGridBox().setPadding(30, 10, 30, 0);
        this.plot.setDomainValueFormat(new DecimalFormat("0"));
        this.plot.setTicksPerDomainLabel(1);
        this.plot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 10);
        this.plot.getGraphWidget().setDomainLabelOrientation(-90);
        this.lineFormatter = new LineAndPointFormatter(Color.rgb(0,0,255),null,null,null);
        this.dataLocation = new ArrayList<Integer>();
    }


    public void update(Object detectedBeads){
        int location = detectedBeads.getY(); //actual location on the image
        if(location > topLine && location < bottomLine) {
            //location relative to center line with 5 pixels resolution
            location = 2 *  (((centerLine - location) / domainInterval) - data[0].intValue()) + 1;
            if(!dataLocation.isEmpty()){
                if(!dataLocation.contains(location)) {
                    dataLocation.add(location);
                }
            }
            else{
                dataLocation.add(location);
            }
            totalCount++;
            data[location ] = data[location].intValue() + 1;
//            System.out.println("data at " + location + "has data " + data[location].intValue());
//            System.out.println("dataLocation size is " + dataLocation.size());
            for(int i = 0; i < dataLocation.size(); i++){
                normalizeddata[dataLocation.get(i)] = (double) data[dataLocation.get(i)].intValue() / totalCount;
                if(normalizeddata[dataLocation.get(i) - 1].doubleValue() > 0.5){
                    this.plot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 0.2);
                }
            }
            // create our series from our array of nums:
            XYSeries series2 = new SimpleXYSeries(
                    Arrays.asList(normalizeddata),
                    SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED,
                    "");
            plot.clear();
            plot.addSeries(series2, barformatter1);
            barRenderer1 = ((MyBarRenderer) plot.getRenderer(MyBarRenderer.class));
            barRenderer1.setBarWidthStyle(BarRenderer.BarWidthStyle.FIXED_WIDTH);
            barRenderer1.setBarWidth(5);
            plot.redraw();
        }
    }

    public void update(Point center){
        int location = (int) center.y;
        if(location < topLine && location > bottomLine) {
            int array_data_location = 2 * ((location - bottomLine) / domainInterval - 1) + 1;
            int old_data = data[array_data_location].intValue();
            totalCount++;
            data[array_data_location] = old_data + 1;
            double new_data =data[array_data_location].doubleValue();
            if(!dataLocation.contains(array_data_location)){
                dataLocation.add(array_data_location);
            }
            for(int i = 0; i < dataLocation.size(); i++){
                normalizeddata[dataLocation.get(i)] = new_data / totalCount;
            }
            if(normalizeddata[array_data_location].doubleValue() > 0.5){
                this.plot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 0.2);
            }
            // create our series from our array of nums:
            XYSeries series2 = new SimpleXYSeries(
                    Arrays.asList(normalizeddata),
                    SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED,
                    "");
            plot.clear();
            plot.addSeries(series2, barformatter1);
            barRenderer1 = ((MyBarRenderer) plot.getRenderer(MyBarRenderer.class));
            barRenderer1.setBarWidthStyle(BarRenderer.BarWidthStyle.FIXED_WIDTH);
            barRenderer1.setBarWidth(5);
            plot.redraw();
        }
    }

    public void gaussianFit(){
        double mean = 0;
        double std = 0;
        for(int i = 0; i < data.length; i++){
            if(data[i] != null) {
                if(i % 2 != 0) {
                    mean += data[i].doubleValue() * data[i - 1].doubleValue();
                }
            }
        }
        mean /= totalCount;
        System.out.println("mean value is " + mean);
        int count = 0;
        for(int i = 0; i < data.length; i++){
            if(i % 2 != 0){
                if(data[i].intValue() > 0) {
                    System.out.println("normalized value is " + normalizeddata[i].doubleValue());
                    for(int j = 0; j < data[i].intValue();j++){
                        std += Math.pow(data[i - 1].doubleValue() - mean, 2);
                        count++;
                    }
                }
            }
        }
        std = Math.sqrt(std / count);
        System.out.println("std is " + std);
        for(int i = 0; i < gaussianFit.length; i++){
            System.out.print("gaussian location " + gaussianFit[i].intValue() + " ");
            if(i % 2 != 0) {
                gaussianFit[i] = 1 / (std * Math.sqrt(2 * Math.PI)) * Math.exp(-Math.pow(data[i-1].doubleValue() - mean, 2) / (2 * Math.pow(std, 2)));
                System.out.println("gaussian fit " + gaussianFit[i].doubleValue());
            }
        }
        XYSeries gaussianCurve = new SimpleXYSeries(
                Arrays.asList(gaussianFit),
                SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED,
                "Gaussian Fit");
        plot.addSeries(gaussianCurve, lineFormatter);
        plot.redraw();
    }

    public void resetData(){
        int counter = 0;
        for(int i = 0; i < data.length; i++){
            if(i % 2 != 0) {
                data[i] = 0;
                normalizeddata[i] = null;
                gaussianFit[i] = 0;
            }
            else{
                data[i] = counter + (centerLine - bottomLine) / domainInterval;
                normalizeddata[i] = counter + (centerLine - bottomLine) / domainInterval;
                gaussianFit[i] = counter + (centerLine - bottomLine) / domainInterval;
                counter++;
            }
        }
//        if(plot != null) {
//            plot.clear();
//        }
        totalCount = 0;
    }

    /*
    This method actually swaps the topline with bottomline because the algorithm used to calculate
    them sees the image upside down.
     */
    public void setDetectionArea(int topLine,int bottomLine, int nearest_topline, int nearest_bottomline){
        this.topLine = nearest_bottomline;
        this.bottomLine = nearest_topline;
        System.out.println("center " + centerLine);
        this.data = new Number[2 * (Math.abs(nearest_topline - nearest_bottomline) / domainInterval)];
        this.normalizeddata = new Number[2 * (Math.abs(nearest_topline - nearest_bottomline) / domainInterval)];
        this.gaussianFit = new Number[2 * (Math.abs(nearest_topline - nearest_bottomline) / domainInterval)];
        resetData();
    }

    public void setCenter(int centerLine){
        this.centerLine = centerLine;
    }

    class MyBarFormatter extends BarFormatter {
        public MyBarFormatter(int fillColor, int borderColor) {
            super(fillColor, borderColor);
        }

        @Override
        public Class<? extends SeriesRenderer> getRendererClass() {
            return MyBarRenderer.class;
        }

        @Override
        public SeriesRenderer getRendererInstance(XYPlot plot) {
            return new MyBarRenderer(plot);
        }
    }

    class MyBarRenderer extends BarRenderer<MyBarFormatter> {

        public MyBarRenderer(XYPlot plot) {
            super(plot);
        }

        public MyBarFormatter getFormatter(int index, XYSeries series) {
            return getFormatter(series);
        }
    }
}