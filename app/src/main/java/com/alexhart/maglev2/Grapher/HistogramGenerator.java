package com.alexhart.maglev2.Grapher;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.Log;

import com.alexhart.maglev2.R;
import com.androidplot.xy.CatmullRomInterpolator;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepFormatter;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Arrays;

import android.graphics.DashPathEffect;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.*;

/**
 * Created by Chung on 1/6/2016.
 */
public class HistogramGenerator {

    private final static String TAG = "HistogramGenerator";

    public HistogramGenerator(XYPlot plot, int interval, Context c) {

        // y-vals to plot:
        Number[] series1Numbers = {1, 2, 3, 4, 2, 3, 16, 20, 35, 24, 10, 4, 2, 3, 2, 2};
        // create our series from our array of nums:
        XYSeries series2 = new SimpleXYSeries(
                Arrays.asList(series1Numbers),
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,
                "1");

        // Create a getFormatter to use for drawing a series using LineAndPointRenderer:
        LineAndPointFormatter series1Format = new LineAndPointFormatter(
                Color.rgb(0, 100, 0),                   // line color
                Color.rgb(0, 100, 0),                   // point color
                Color.rgb(100, 200, 0), null);                // fill color


        // setup our line fill paint to be a slightly transparent gradient:
        Paint lineFill = new Paint();
        lineFill.setAlpha(200);
        lineFill.setShader(new LinearGradient(0, 0, 0, 250, Color.WHITE, Color.BLUE, Shader.TileMode.MIRROR));

        StepFormatter stepFormatter  = new StepFormatter(Color.rgb(0, 0,0), Color.BLUE);
        stepFormatter.getLinePaint().setStrokeWidth(1);

        stepFormatter.getLinePaint().setAntiAlias(false);
        stepFormatter.setFillPaint(lineFill);
        plot.addSeries(series2, stepFormatter);

        // adjust the domain/range ticks to make more sense; label per tick for range and label per 5 ticks domain:
        plot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 1);
        plot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 2);
    }
}

