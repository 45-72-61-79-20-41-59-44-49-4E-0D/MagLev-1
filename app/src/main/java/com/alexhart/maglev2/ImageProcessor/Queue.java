package com.alexhart.maglev2.ImageProcessor;

/**
 * Created by Chung on 1/6/2016.
 */
public class Queue {
    private Range firstRange;
    private Range currentRange;

    public Queue(){
        firstRange = null;
    }

    public void enqueue(Range range){
        if(isEmpty()){
            firstRange = range;
            currentRange = range;
        }
        else{
            currentRange.setNextRange(range);
            currentRange = range;
        }
    }

    public Range dequeue(){
        Range temp = firstRange;
        firstRange = firstRange.getNextRange();
        return temp;
    }

    public boolean isEmpty(){
        return firstRange == null;
    }
}
