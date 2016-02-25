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

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarEntry;

import org.opencv.core.Point;

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
    private int center;
    private MyBarFormatter barformatter1;
    private MyBarRenderer barRenderer1;
    private LineAndPointFormatter lineFormatter;
//    private ArrayList<Integer> XAxis;
//    private ArrayList<BarEntry> barEntries;
//    private BarChart barChart;

    public HistogramGenerator(XYPlot plot) {
        totalCount = 0;
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
    }

//    public HistogramGenerator(BarChart bc) {
//        this.barChart = bc;
//        populateXAxis();
//    }

    public void update(Object detectedBeads){
        int location = detectedBeads.getY();
        if(location < topLine && location > bottomLine) {
            int data_location = (location - bottomLine) / domainInterval - 1;
            int old_data = data[data_location].intValue();
            totalCount++;
            data[data_location] = old_data + 1;
            double new_data =data[data_location].doubleValue();
            normalizeddata[data_location] = new_data / totalCount;
            if(normalizeddata[data_location].doubleValue() > 0.5){
                this.plot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 0.2);
            }
            // create our series from our array of nums:
            XYSeries series2 = new SimpleXYSeries(
                    Arrays.asList(normalizeddata),
                    SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,
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
            int data_location = (location - bottomLine) / domainInterval - 1;
            int old_data = data[data_location].intValue();
            totalCount++;
            data[data_location] = old_data + 1;
            double new_data =data[data_location].doubleValue();
            normalizeddata[data_location] = new_data / totalCount;
            if(normalizeddata[data_location].doubleValue() > 0.5){
                this.plot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 0.2);
            }
            // create our series from our array of nums:
            XYSeries series2 = new SimpleXYSeries(
                    Arrays.asList(normalizeddata),
                    SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,
                    "");
            plot.clear();
            plot.addSeries(series2, barformatter1);
            plot.redraw();
        }
    }

    public void gaussianFit(){
        double mean = 0;
        double std = 0;
        for(int i = 0; i < data.length; i++){
            if(data[i] != null) {
                mean += data[i].doubleValue() * i;
            }
        }
        mean /= totalCount;
        int count = 0;
        for(int i = 0; i < data.length; i++){
            if(data[i].intValue() > 0) {
                for(int j = 0; j < data[i].intValue();j++){
                    std += Math.pow(i - mean, 2);
                    count++;
                }
            }
        }
        std = Math.sqrt(std / count);
        for(int i = 0; i < gaussianFit.length; i++){
            gaussianFit[i] = 1/(std*Math.sqrt(2*Math.PI))*Math.exp(-Math.pow(i-mean,2)/(2*Math.pow(std,2)));
    }
        XYSeries gaussianCurve = new SimpleXYSeries(
                Arrays.asList(gaussianFit),
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,
                "Gaussian Fit");
        plot.addSeries(gaussianCurve,lineFormatter);
        plot.redraw();
    }



//    public void getData(Object object){
//        int location = object.getY();
//        int relativeToCenter = location - center;
//        if(relativeToCenter < 150 && relativeToCenter > -150){
//
//        }
//    }

//    private int mapDataLocation(int rawLocation){
//        int dataLocation = 0;
//
//        return dataLocation;
//    }

    public void resetData(){
        for(int i = 0; i < data.length; i++){
            data[i] = 0;
            normalizeddata[i] = null;

        }
//        if(plot != null) {
//            plot.clear();
//        }
        totalCount = 0;
    }

    public void setDetectionArea(int topLine,int bottomLine){
        this.topLine = topLine;
        this.bottomLine = bottomLine;
        System.out.println("heyhey topline " + topLine);
        System.out.println("heyhey bottomline" + bottomLine);
        this.data = new Number[Math.abs(topLine - bottomLine) / domainInterval];
        this.normalizeddata = new Number[Math.abs(topLine - bottomLine) / domainInterval];
        this.gaussianFit = new Number[Math.abs(topLine - bottomLine) / domainInterval];
        resetData();
    }

    public void setCenter(int center){
        this.center = center;
    }

//    private void populateXAxis(){
//        for(int i = -140; i < 0; i += 10){
//            XAxis.add(i);
//        }
//        for(int i = 0; i <= 140; i += 10){
//            XAxis.add(i);
//        }
//    }

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