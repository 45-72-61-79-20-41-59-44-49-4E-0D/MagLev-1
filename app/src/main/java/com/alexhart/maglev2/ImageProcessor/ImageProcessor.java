package com.alexhart.maglev2.ImageProcessor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;

import com.alexhart.maglev2.Grapher.HistogramGenerator;
import com.alexhart.maglev2.R;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

import org.opencv.core.Algorithm;
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
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by Chung on 12/28/2015.
 */
public class ImageProcessor implements  Runnable{

    private Mat src;
    private Mat dst;
    private boolean stop = false;
    private boolean isVideo;
    private Rect roi;
    private double windowStart;
    private Bitmap bitmap;
    private Thread t;
    private ImageView targetImageView;
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
    private Display display;


    private final static String TAG = "ImageProcessor";

    @Override
    public void run() {
            if (!isVideo) {
             //   final Mat dst = beadsDetection(src);
//                targetImageView.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
//                        bitmap = Bitmap.createBitmap(dst.cols(), dst.rows(), conf);
//                        Utils.matToBitmap(dst, bitmap);
//                        targetImageView.setImageBitmap(bitmap);
//                    }
//                });
            } else {
                String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                int timeInMilSeconds = Integer.parseInt(time);
                Mat currentFrame = new Mat();
                boolean gotROI = false;
                Log.d(TAG, "START");
                for (int i = 0; i < 25; i++) {
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
                    dst = beadDetection(currentFrame,roi);
                    Imgproc.resize(dst,dst,new Size(targetImageView.getWidth(),targetImageView.getHeight()));
                    targetImageView.post(new Runnable() {
                        @Override
                        public void run() {
                            bitmap = Bitmap.createScaledBitmap(bitmap, targetImageView.getWidth(), targetImageView.getHeight(), false);
                            Utils.matToBitmap(dst, bitmap);
                            targetImageView.setImageBitmap(bitmap);
                        }
                    });
                }
                Log.d(TAG, "START STOP");
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
    public ImageProcessor(File file,View v,boolean isVideo) {
        src = new Mat();
        dst = new Mat();
        this.isVideo = isVideo;
        this.targetImageView = (ImageView) v.findViewById(R.id.resultView);
        //this.plot = (XYPlot) v.findViewById(R.id.plot);
        if(isVideo) {
            Log.d(TAG, "START 4");
            retriever = new MediaMetadataRetriever();
            retriever.setDataSource(file.getAbsolutePath());
            Log.d(TAG, "START 5");
        }
        else{
            Log.d(TAG,"START 4");
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            Log.d(TAG,"START 5");
            bitmap = Bitmap.createScaledBitmap(bitmap,targetImageView.getWidth(),targetImageView.getHeight(),false);
            Log.d(TAG,"START 6");
            Utils.bitmapToMat(bitmap, src);
            Log.d(TAG, "START 7");
        }
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
        Log.d(TAG, "START 20");
        Imgproc.Canny(src_grey, edge, 40, 115);
        edge = new Mat(edge,roi);
        Mat kernal = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE,new Size(13,13));
        Imgproc.morphologyEx(edge, edge, Imgproc.MORPH_CLOSE, kernal);
        Log.d(TAG, "START 21");
        Imgproc.distanceTransform(edge, edge, Imgproc.CV_DIST_L1, 5);
        edge.convertTo(edge, CvType.CV_8U);
        findLocalMaxima(edge);
        Imgproc.line(src, new Point(windowStart + roi.width, 0), new Point(windowStart + roi.width, roi.height), new Scalar(255, 0, 0), 1);
        Imgproc.line(src, new Point(windowStart, 0), new Point(windowStart, roi.height), new Scalar(255, 0, 0), 1);
        for(int i = 0; i < maxCandidates.length; i ++){
            Object localMax = maxCandidates[i];
            if(!localMax.isFlagged()) {
                Point center = new Point(maxCandidates[i].getX() + windowStart , maxCandidates[i].getY());
                Imgproc.circle(src, center, maxCandidates[i].getIntensity(), new Scalar(0, 255, 0));
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
