package com.alexhart.maglev2.ImageProcessor;

/**
 * Created by Chung on 1/6/2016.
 */
public class Stack{
    private Object currentObject;
    private int nObject;


    //constructor
    public Stack(){
        currentObject = null;
        nObject = 0;
    }
    //Method to push an object into stack
    public void push(Object newObject){
        newObject.setNextObject(currentObject);
        currentObject = newObject;
        nObject++;
    }
    //Method to pop an object out of stack
    public Object pop(){
        Object temp = currentObject;
        currentObject = currentObject.getNextObject();
        nObject--;
        return temp;
    }
    //Method to check if stack is empty
    public boolean isEmpty(){
        return currentObject == null;
    }
    //Method to check number of object in the stack
    public int numOfObject(){
        return nObject;
    }
}