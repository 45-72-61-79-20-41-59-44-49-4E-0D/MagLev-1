package com.alexhart.maglev2.ImageProcessor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.alexhart.maglev2.Grapher.HistogramGenerator;
import com.alexhart.maglev2.R;

import com.androidplot.xy.XYPlot;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.Utils;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

/*
Note (0,0) is top left of the image in algorithm. In other words, the algorithm sees the image upside
down.
 */

public class ImageProcessor implements  Runnable{
    private Mat src;
    private Mat dst;
    private boolean stop = false;
    private boolean isVideo;
    private double windowStart;
    private double[] topLine_array;
    private double[] bottomLine_array;
    private double topLine;
    private double bottomLine;
    private double[] nearest_topline_array;
    private double[] nearest_bottomline_array;
    private double nearest_topline;
    private double nearest_bottomline;
    private Rect roi;
    private Bitmap bitmap;
    private Thread t;
    private ImageView targetImageView;
    private ProgressBar progressBar;
    private MediaMetadataRetriever retriever;
    private XYPlot plot;
    private byte[][] data_2D;
    boolean[][] isOccupied;
    boolean[][] isPrevFlooded;
    Object[] maxCandidates;
    Queue rangeQueue = new Queue();
    Queue prevFloodedQueue = new Queue();
    final static int[] DIR_X_OFFSET = new int[] {  0,  1,  1,  1,  0, -1, -1, -1 };
    final static int[] DIR_Y_OFFSET = new int[] { -1, -1,  0,  1,  1,  1,  0, -1 };
    private int height;
    private int width;
    private int center;
    private int detectionMethod;
    private HistogramGenerator histG;

    private final static String TAG = "ImageProcessor";

    @Override
    public void run() {
            if (!isVideo) {
                final Mat dst = beadDetection(src,roi);
                targetImageView.post(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
                        bitmap = Bitmap.createBitmap(dst.cols(), dst.rows(), conf);
                        Utils.matToBitmap(dst, bitmap);
                        targetImageView.setImageBitmap(bitmap);
                    }
                });
            } else {
                String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                int timeInSeconds = Integer.parseInt(time) / 1000;
                Mat currentFrame = new Mat();
                boolean gotROI = false;
                boolean gotCenterLine = false;
                for (int i = 0; i < timeInSeconds; i++) {
                    if(stop){
                        break;
                    }
                    bitmap = retriever.getFrameAtTime(i * 1000000);
                    Utils.bitmapToMat(bitmap, currentFrame);
                    if(!gotROI) {
                        windowStart = (double) currentFrame.cols() / 2 - 80;
                        roi = new Rect((int) windowStart, 0, 150, currentFrame.rows());
                        gotROI = true;
                    }

                    if(!gotCenterLine){
                        int j;
                        for(j = 0; j < 9; j++){
                            bitmap = retriever.getFrameAtTime(j * 1000000);
                            Utils.bitmapToMat(bitmap, currentFrame);
                            centerDetection(currentFrame,j);
                        }
                        bitmap = retriever.getFrameAtTime(j * 1000000);
                        Utils.bitmapToMat(bitmap, currentFrame);
                        centerDetectionP(centerDetection(currentFrame,j));
                        gotCenterLine = true;
                    }
                    if(detectionMethod == 0){
                        dst = beadDetection(currentFrame, roi);
                    }
                    else if(detectionMethod == 1){
                        dst = beadsDetection_HoughCircle(currentFrame, roi);
                    }
                    Imgproc.resize(dst, dst, new Size(targetImageView.getWidth(), targetImageView.getHeight()));
                    targetImageView.post(new Runnable() {
                        @Override
                        public void run() {
                            targetImageView.setVisibility(View.VISIBLE);
                            bitmap = Bitmap.createScaledBitmap(bitmap, targetImageView.getWidth(), targetImageView.getHeight(), false);
                            Utils.matToBitmap(dst, bitmap);
                            targetImageView.setImageBitmap(bitmap);
                        }
                    });
                }
                targetImageView.post(new Runnable() {
                    @Override
                    public void run() {
                        targetImageView.setVisibility(View.INVISIBLE);
                    }
                });
                histG.gaussianFit();
                histG.resetData();
            }
    }

    public void start(){
        if(t == null){
            t = new Thread(this);
            stop = false;
            t.start();
        }
    }

    //Constructor for ImageProcessor
    public ImageProcessor(File file,View v,boolean isVideo, HistogramGenerator histG, Display display, int detectionMethod) {
        src = new Mat();
        dst = new Mat();
        android.graphics.Point size = new android.graphics.Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        this.topLine_array = new double[10];
        this.bottomLine_array = new double[10];
        this.nearest_topline_array = new double[10];
        this.nearest_bottomline_array = new double[10];
        this.isVideo = isVideo;
        this.targetImageView = (ImageView) v.findViewById(R.id.resultView);
        targetImageView.getLayoutParams().width = width;
        targetImageView.getLayoutParams().height = height;
        targetImageView.requestLayout();
        this.progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        this.histG = histG;
        this.detectionMethod = detectionMethod;
        if(isVideo) {
            retriever = new MediaMetadataRetriever();
            retriever.setDataSource(file.getAbsolutePath());
        }
        else{
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            bitmap = Bitmap.createScaledBitmap(bitmap,targetImageView.getWidth(),targetImageView.getHeight(),false);
            Utils.bitmapToMat(bitmap, src);
        }
    }

    //This method uses simple probability to determine most likely top line, bottom line and cetner
    private void centerDetectionP(Mat src){
        double mean_top = 0;
        double mean_bottom = 0;
        double mean_nearesttop = 0;
        double mean_nearestbottom = 0;
        double avg_top_diff = 0;
        double avg_bottom_diff = 0;
        double avg_nearesttop_diff = 0;
        double avg_nearestbottom_diff = 0;
        ArrayList<Double> possible_top = new ArrayList<Double>();
        ArrayList<Double> possible_bottom = new ArrayList<Double>();
        ArrayList<Double> possible_nearesttop = new ArrayList<Double>();
        ArrayList<Double> possible_nearestbottom = new ArrayList<Double>();
        mean_top = this.topLine / 10;
        mean_bottom = this.bottomLine / 10;
        mean_nearestbottom = this.nearest_topline / 10;
        mean_nearesttop /= this.nearest_bottomline / 10;
        for(int i = 0; i < topLine_array.length; i++){
            avg_top_diff += Math.abs(topLine_array[i] - mean_top);
            avg_bottom_diff += Math.abs(bottomLine_array[i] - mean_bottom);
            avg_nearesttop_diff += Math.abs(nearest_topline_array[i] - mean_nearesttop);
            avg_nearestbottom_diff += Math.abs(nearest_bottomline_array[i] - mean_nearestbottom);
        }
        avg_top_diff /= 10;
        avg_nearestbottom_diff /= 10;
        avg_nearesttop_diff /= 10;
        avg_bottom_diff /= 10;
        for(int i = 0; i < topLine_array.length; i++){
            if(Math.abs(topLine_array[i] - mean_top) < avg_top_diff){
                possible_top.add(topLine_array[i]);
            }
            if(Math.abs(bottomLine_array[i] - mean_bottom) < avg_bottom_diff){
                possible_bottom.add(bottomLine_array[i]);
            }
            if(Math.abs(nearest_topline_array[i] - mean_nearesttop) < avg_nearesttop_diff){
                possible_nearesttop.add(nearest_topline_array[i]);
            }
            if(Math.abs(nearest_bottomline_array[i] - mean_nearestbottom) < avg_nearestbottom_diff){
                possible_nearestbottom.add(nearest_bottomline_array[i]);
            }
        }
        this.topLine = 0;
        this.bottomLine = 0;
        this.nearest_topline = 0;
        this.nearest_bottomline = 0;
        for(int i = 0; i < possible_top.size(); i++){
            topLine += possible_top.get(i);
        }
        topLine /= possible_top.size();
        for(int i = 0; i < possible_bottom.size(); i++){
            bottomLine += possible_bottom.get(i);
        }
        bottomLine /= possible_bottom.size();
        for(int i = 0; i < possible_nearesttop.size(); i++){
            nearest_topline += possible_nearesttop.get(i);
        }
        nearest_topline /= possible_nearesttop.size();
        for(int i = 0; i < possible_nearestbottom.size(); i++){
            nearest_bottomline += possible_nearestbottom.get(i);
        }
        nearest_bottomline /= possible_nearestbottom.size();
        int offset_i = 1;
        int offset_j = -1;
        Mat current_top_line = new Mat(src,new Rect(0,(int) topLine,150,1));
        Mat current_bot_line = new Mat(src,new Rect(0,(int) bottomLine,150,1));
        while(true){
            Rect bot_roi = new Rect(0,(int) bottomLine + offset_j,150,1);
            Mat next_bot_line = new Mat(src,bot_roi);
            if(Core.sumElems(next_bot_line).val[0] < Core.sumElems(current_bot_line).val[0]){
                bottomLine = bottomLine + offset_j;
                break;
            }
            offset_j--;
        }
        while(true){
            Rect top_roi = new Rect(0,(int) topLine + offset_i ,150,1);
            Mat next_top_line = new Mat(src,top_roi);
            if(Core.sumElems(next_top_line).val[0] < Core.sumElems(current_top_line).val[0]){
                topLine = topLine + offset_i;
                break;
            }
            offset_i++;
        }
        center = (int) ((topLine - bottomLine) / 2 + bottomLine);
        histG.setCenter(center);
        histG.setDetectionArea((int) topLine, (int) bottomLine, (int) nearest_topline, (int) nearest_bottomline);
    }

    private Mat centerDetection(Mat src,int ithframe){
        Mat edge = new Mat();
        Mat lines = new Mat();
        Mat temp;
        double centerLine = src.rows() / 2;
        double topLine = 0;
        double bottomLine = 0;
        double topDifference = 0;
        double bottomDifference = 0;
        double nearest_topline = 0;
        double nearest_bottomline = 0;
        double nearest_topDifference = Double.POSITIVE_INFINITY;
        double nearest_bottomDifference = Double.POSITIVE_INFINITY;
        Mat grey_src = rgb2grey(src);
        temp = new Mat(grey_src,roi);
        Imgproc.GaussianBlur(temp,temp,new Size(3,3),1);
        Imgproc.Canny(temp, edge, 10, 30);
        Imgproc.HoughLinesP(edge, lines, 1, Math.PI / 180, 25, 30, 5);
        for(int i = 0; i < lines.rows() ; i++){
            double vec[] = lines.get(i,0);
            double y = vec[1];
//            Imgproc.line(src,new Point(0,y), new Point(src.cols(),y),new Scalar(0,255,0),2);
            if(y > centerLine && (Math.abs(centerLine - y) > topDifference)){
                topDifference = Math.abs(centerLine - y);
                topLine = y;
            }
            if(y > centerLine && (Math.abs(centerLine - y) < nearest_topDifference)){
                nearest_topDifference = Math.abs(centerLine - y);
                nearest_topline = y;
            }
            if(y < centerLine && (Math.abs(centerLine - y) > bottomDifference)){
                bottomDifference = Math.abs(centerLine - y);
                bottomLine = y;
            }
            if(y < centerLine && (Math.abs(centerLine - y) < nearest_bottomDifference)){
                nearest_bottomDifference = Math.abs(centerLine - y);
                nearest_bottomline = y;
            }
        }
        if(topLine == 0 && bottomLine > 0){
            topLine = src.rows() - bottomLine;
        }
        if(topLine > 0 && bottomLine == 0){
            bottomLine = src.rows() - topLine;
        }
//        Imgproc.line(src,new Point(0,topLine), new Point(src.cols(),topLine),new Scalar(255,0,0),4);
//        Imgproc.line(src,new Point(0,bottomLine), new Point(src.cols(),bottomLine),new Scalar(0,0,255),4);
        topLine_array[ithframe] = topLine;
        this.topLine += topLine;
        bottomLine_array[ithframe] = bottomLine;
        this.bottomLine += bottomLine;
        nearest_bottomline_array[ithframe] = nearest_bottomline;
        this.nearest_bottomline += nearest_bottomline;
        nearest_topline_array[ithframe] = nearest_topline;
        this.nearest_topline += nearest_topline;
    return grey_src;
    }

    // bwAreaOpen is used to remove objects that have less than lowerThresh
    private void bwAreaOpen(Mat binary_src, int lowerThresh){
        if(binary_src.type() == CvType.CV_8U) {
            Mat hierarchy = new Mat();
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Imgproc.findContours(binary_src.clone(), contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_NONE);
            for(int i = 0; i <contours.size(); i++){
                // Calculate contour area
                Mat contour = contours.get(i);
                double area = Imgproc.contourArea(contour);
                // Remove small objects
                if(area < lowerThresh){
                    Imgproc.drawContours(binary_src,contours,i,new Scalar(0,0,0),-1);
                }
            }
        }
    }

    private Mat beadDetection(Mat src, Rect roi){
        Mat edge = new Mat();
        Mat src_grey = rgb2grey(src);
        Imgproc.Canny(src_grey, edge, 5, 30);
//        edge = new Mat(edge,roi);
//        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE,new Size(13,13));
//        Imgproc.morphologyEx(edge, edge, Imgproc.MORPH_CLOSE, kernel);
        //==============Idea testing=================\\
//        src_grey = new Mat(src_grey,roi);
//        Mat blackhat_kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE,new Size(7,7));
//        Imgproc.resize(src_grey, src_grey, new Size(src.cols() / 4, src.rows() / 4));
//        Imgproc.morphologyEx(src_grey, src_grey, Imgproc.MORPH_BLACKHAT, blackhat_kernel);
//        Imgproc.resize(src_grey, src_grey, new Size(src.cols(), src.rows()));
//        Imgproc.threshold(src_grey, src_grey, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);
////        Imgproc.distanceTransform(src_grey,src_grey, Imgproc.CV_DIST_L1, 5);
//        src_grey.convertTo(src_grey, CvType.CV_8U);
//        findLocalMaxima(src_grey);
        //============================================\\
//        Imgproc.distanceTransform(edge, edge, Imgproc.CV_DIST_L1, 5);
//        edge.convertTo(edge, CvType.CV_8U);
//        findLocalMaxima(edge);
        Imgproc.line(src, new Point(0, center), new Point(src.cols(), center), new Scalar(0, 0, 255), 1);
        Imgproc.line(src,new Point(0,topLine), new Point(src.cols(),topLine),new Scalar(0,255,0),2);
        Imgproc.line(src,new Point(0,bottomLine), new Point(src.cols(),bottomLine),new Scalar(127,249,245),2);
        Imgproc.line(src, new Point(windowStart + roi.width, 0), new Point(windowStart + roi.width, roi.height), new Scalar(255, 0, 0), 1);
        Imgproc.line(src, new Point(windowStart, 0), new Point(windowStart, roi.height), new Scalar(255, 0, 0), 1);
//        for(int i = 0; i < maxCandidates.length; i ++){
//            Object localMax = maxCandidates[i];
//            if(!localMax.isFlagged()) {
//                Point center = new Point(maxCandidates[i].getX() + windowStart , maxCandidates[i].getY());
//                Imgproc.circle(src, center, maxCandidates[i].getIntensity(), new Scalar(0, 255, 0));
//                histG.update(localMax);
//            }
//        }
        return src;
    }

    private Mat beadsDetection_HoughCircle(Mat src, Rect roi){
        Mat src_grey = rgb2grey(src);
        Mat circles = new Mat();
        Imgproc.line(src, new Point(0, center), new Point(src.cols(), center), new Scalar(0, 0, 255), 2);
        Imgproc.line(src,new Point(0,topLine), new Point(src.cols(),topLine),new Scalar(0, 255, 0), 2);
        Imgproc.line(src, new Point(0, bottomLine), new Point(src.cols(), bottomLine),new Scalar(127, 249, 245), 2);
        Imgproc.line(src, new Point(windowStart + roi.width, 0), new Point(windowStart + roi.width, roi.height), new Scalar(255, 0, 0), 1);
        Imgproc.line(src, new Point(windowStart, 0), new Point(windowStart, roi.height), new Scalar(255, 0, 0), 1);
        Imgproc.HoughCircles(src_grey, circles, Imgproc.HOUGH_GRADIENT, 1, 10, 30, 10, 5, 20);
        if(circles.cols() > 0) {
            for (int i = 0; i < circles.cols(); i++) {
                double vCircle[] = circles.get(0, i);
                if(Math.round(vCircle[0]) > windowStart && Math.round(vCircle[0]) < windowStart + 150) {
                    Point center = new Point(Math.round(vCircle[0]), Math.round(vCircle[1]));
                    int radius = (int) Math.round(vCircle[2]);
                    // draw the found circle
                    Imgproc.circle(src, center, radius, new Scalar(0, 255, 0));
                    //histG.update(center);
                }
            }
        }
        return src;
    }

    // Convert RGB image to grey scale image
    private Mat rgb2grey(Mat src){
        Mat dst = new Mat();
        Imgproc.cvtColor(src, dst, Imgproc.COLOR_RGB2GRAY);
        return dst;
    }


    // Convert Grey image to binary image
    private Mat grey2binary(Mat grey_src){
        Mat temp = new Mat();
        Imgproc.threshold(grey_src, temp, 0 , 255, Imgproc.THRESH_BINARY+Imgproc.THRESH_OTSU);
        return temp;
    }

    // find all local maximum
    private void findLocalMaxima(Mat grey_src){
        // Extract size of the image
        Size size = grey_src.size();
        height = (int) size.height;
        width = (int) size.width;
        // counter for converting 1D to 2D
        int counter = 0;
        // Initialize a primitive java type array for pixel manipulation
        byte[] data_1D = new byte[height*width];
        data_2D = new byte[height][width];
        // Store the image in 1D array
        grey_src.get(0, 0, data_1D);
        // Convert 1D array to 2D array
        for(int row = 0; row < height; row++){
            for(int col = 0; col < width; col++){
                data_2D[row][col] = data_1D[counter];
                counter++;
            }
        }
        //1D byte array to store all local max pixels, the local max found here will be
        //potential candidates and be passed to floodFill, which analyzes if the max
        //candidate is a true local max.
        Stack localMaxList = new Stack();
        // Find all the local maximum and put them in a sorted array with descending order
        for(int row = 1; row < height - 1; row++){
            for(int col = 1; col < width -1; col++) {
                //Check if the pixel is smaller than any of 8-neighbor pixel
                int value = (int) data_2D[row][col];
                if(value > 1){
                    boolean isLocalMax = true;
                    for (int i = 0; i < 8; i++) {
                        int neighbor = (int) data_2D[row + DIR_X_OFFSET[i]][col + DIR_Y_OFFSET[i]];
                        //Current pixel value is smaller than at least 1 neighbor pixel, not a local max
                        if (neighbor > value) {
                            isLocalMax = false;
                        }
                    }
                    if (isLocalMax) {
                        Object maxCandidate = new Object(value, row, col,false);
                        localMaxList.push(maxCandidate);
                    }
                }
            }
        }
        //Put the local max in an array and sort it
        maxCandidates = new Object[localMaxList.numOfObject()];
        int i = 0;
        while(!localMaxList.isEmpty()){
            maxCandidates[i] = localMaxList.pop();
            i++;
        }
        if(maxCandidates.length > 0) {
            MergeSort mergesort = new MergeSort(maxCandidates);
            mergesort.mergeSort();
            maxCandidates = mergesort.getArrayToSort();
            floodFill(1, maxCandidates);
        }
    }

    //
    private void floodFill(int tolerance, Object[] maxCandidates) {
        //isOccupied checks if the pixel is visited
        isOccupied = new boolean[height][width];
        //isPrevFlooded checks if the pixel is previously visited after we are done analyzing
        //previous local max. Note if a pixel is previously flooded, it means it is also
        //occupied. Therefore its corresponding location in isOccupied will also be true.
        //The converse is not necessarily true.
        isPrevFlooded = new boolean[height][width];
        //Algorithm terminates when all candidates are analyzed
        for(int i = maxCandidates.length - 1; i >= 0; i--) {
            int localmax_x = maxCandidates[i].getX();
            int localmax_y = maxCandidates[i].getY();
            int value = maxCandidates[i].getIntensity();
            //This local max location is previously flooded, we flag it as false local maxima
            if (isPrevFlooded[localmax_y][localmax_x]) {
                maxCandidates[i].setFlagged();
            }
            //This local max is not previously flooded, we start flood the pixels on the same line
            else {
                fillHorizontal(localmax_x, localmax_y, value, tolerance, i);
                //Loop until rangequeue is empty. When it is empty, it means the current maxima has
                //flooded all the area it can reach with a given tolerance
                while (!rangeQueue.isEmpty()) {
                    //Dequeue a range object
                    Range currentRange = rangeQueue.dequeue();
                    int startX = currentRange.getStartX();
                    int endX = currentRange.getEndX();
                    int y = currentRange.getY();
                    int range = endX - startX + 1;
                    for (int j = 0; j < range; j++) {
                        //If it is previously flooded, it is also occupied, but being occupied doesn't mean
                        //is is previously flooded
                        if(y > 0 && y < height - 1){
                            if (isPrevFlooded[y - 1][startX] || isPrevFlooded[y + 1][startX]) {
                                maxCandidates[i].setFlagged();
                                startX++;
                            } else {
                                //Check if pixel above is occupied
                                if (!isOccupied[y - 1][startX]) {
                                    if (Math.abs(value - data_2D[y - 1][startX]) <= tolerance) {
                                        fillHorizontal(startX, y - 1, value, tolerance, i);
                                    }
                                }
                                //Check if pixel below is occupied
                                if (!isOccupied[y + 1][startX]) {
                                    if (Math.abs(value - data_2D[y + 1][startX]) <= tolerance) {
                                        fillHorizontal(startX, y + 1, value, tolerance, i);
                                    }
                                }
                                startX++;
                            }
                        }
                    }
                }
                //Go through prevFloodedQueue and mark the pixels as previously flooded
                while (!prevFloodedQueue.isEmpty()) {
                    Range currentRange = prevFloodedQueue.dequeue();
                    int startX = currentRange.getStartX();
                    int endX = currentRange.getEndX();
                    int y = currentRange.getY();
                    int range = endX - startX + 1;
                    for (int j = 0; j < range; j++) {
                        isPrevFlooded[y][startX] = true;
                        startX++;
                    }
                }
            }
        }
    }
    //This method fills the horizontal line of pixels that are within the tolerance.
    // Meaning changing the corresponding location of isOccupied to true.Note while filling
    // the line, any encounter of occupied pixels implies that encountered pixel is also
    // previously flooded because there is no pixel that can be reached by the current local max
    // can be processed before the currently processed pixel.
    private void fillHorizontal(int x, int y, int value, int tolerance, int nMax){
        int tempX = x;
        int startX;
        int endX;
        isOccupied[y][x] = true;
        //Fill right
        while(true){
            x++;
            // x is within the image boundary, excluding pixels on the edges
            if(x < width - 1) {
                if (!isOccupied[y][x]) {
                    if (Math.abs(value - data_2D[y][x]) <= tolerance) {
                        isOccupied[y][x] = true;
                    } else {
                        endX = x - 1;
                        break;
                    }
                } else {
                    maxCandidates[nMax].setFlagged();
                    endX = x - 1;
                    break;
                }
            } else{
                endX = width - 1;
                break;
            }
        }
        x = tempX;
        //Fill left
        while(true){
            x--;
            // x is within the image boundary, excluding pixels on the edges
            if(x > 0) {
                //If the pixel is not occupied, we check if it is within the tolerance
                if (!isOccupied[y][x]) {
                    if (Math.abs(value - data_2D[y][x]) <= tolerance) {
                        isOccupied[y][x] = true;
                    } else {
                        startX = x + 1;
                        break;
                    }
                } else {
                    maxCandidates[nMax].setFlagged();
                    startX = x + 1;
                    break;
                }
            } else{ // x is not within the image boundary, stops filling and break the loop
                startX = 0;
                break;
            }
        }
        //Enqueue the new range
        Range newRange = new Range(y,startX,endX);
        rangeQueue.enqueue(newRange);
        prevFloodedQueue.enqueue(newRange);
    }

    public void stopThread(){
        this.stop = true;
    }
}
