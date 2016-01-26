package com.alexhart.maglev2.ImageProcessor;

/**
 * Created by Chung on 1/6/2016.
 */
public class Range{
    private int y;
    private int startX;
    private int endX;
    private Range nextRange;


    public Range(int y, int startX, int endX){
        this.y = y;
        this.startX = startX;
        this.endX = endX;
    }

    public int getY(){
        return y;
    }
    public int getStartX(){
        return startX;
    }
    public int getEndX(){
        return endX;
    }
    public Range getNextRange(){
        return nextRange;
    }
    public void setNextRange(Range nextRange){
        this.nextRange = nextRange;
    }
}
