package com.alexhart.maglev2.ImageProcessor;

/**
 * Created by Chung on 1/6/2016.
 */
public class Object {
    //Class fields
    private int intensity;
    private int x_location;
    private int y_location;
    private boolean isFlagged;
    private Object nextObject;

    public Object(int i, int y, int x, boolean flag) {
        intensity = i;
        x_location = x;
        y_location = y;
        isFlagged = flag;
    }

    public void setNextObject(Object object) {
        nextObject = object;
    }

    public Object getNextObject(){
        return nextObject;
    }

    public int getIntensity(){
        return intensity;
    }

    public int getX(){
        return x_location;
    }

    public int getY(){
        return y_location;
    }

    public void setFlagged(){
        isFlagged = true;
    }

    public boolean isFlagged(){
        return isFlagged;
    }
}