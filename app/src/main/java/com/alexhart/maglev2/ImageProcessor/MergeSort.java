package com.alexhart.maglev2.ImageProcessor;

public class MergeSort {
    //Class fields
    private Object[] arrayToSort;

    //Merge sort for local max
    public MergeSort(Object[] objects){
        arrayToSort = objects;
    }

    public void mergeSort(){
        Object[] workspace = new Object[arrayToSort.length];
        recMergeSort(workspace,0,arrayToSort.length-1);
    }

    private void recMergeSort(Object[] workSpace,int lowerBound, int upperBound){
        if(lowerBound == upperBound){
            return;
        }
        else{
            int mid = (lowerBound + upperBound)/2;
            recMergeSort(workSpace,lowerBound,mid);
            recMergeSort(workSpace,mid+1,upperBound);
            merge(workSpace,lowerBound,mid+1,upperBound);
        }
    }

    private void merge(Object[] workspace, int lowPtr, int highPtr, int upperBound){
        int j = 0;
        int lowerBound = lowPtr;
        int mid = highPtr - 1;
        int n = upperBound - lowerBound + 1;

        while(lowPtr <= mid && highPtr <= upperBound){
            int lowPtrValue = arrayToSort[lowPtr].getIntensity();
            int highPtrValue = arrayToSort[highPtr].getIntensity();
            if(lowPtrValue < highPtrValue){
                workspace[j++] = arrayToSort[lowPtr++];
            }
            else{
                workspace[j++] = arrayToSort[highPtr++];
            }
        }

        while(lowPtr <= mid){
            workspace[j++] = arrayToSort[lowPtr++];
        }

        while(highPtr <= upperBound){
            workspace[j++] = arrayToSort[highPtr++];
        }

        for(j=0; j < n; j++){
            arrayToSort[lowerBound + j] = workspace[j];
        }
    }

    public Object[] getArrayToSort(){
        return  arrayToSort;
    }
}