package com.alexhart.maglev2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Gallery;
import android.widget.ImageView;

import com.alexhart.maglev2.GalleryViewFrag;

import java.io.File;
import java.lang.ref.WeakReference;
/**
 * Async task for viewing images
 * WIll freeze up UI thread if not posted
 */
public class SingleImageWorkerTask extends AsyncTask<File, Void, Bitmap> {
    private String TAG = "SingleImageWorkerTask";
    WeakReference<ImageView> imageViewReferences;
    final int TARGET_IMAGE_VIEW_WIDTH;
    final int TARGET_IMAGE_VIEW_HEIGHT;
    private File mImageFile;
    private GalleryViewFrag.OnFragmentInteractionListener mListener;
    public SingleImageWorkerTask(ImageView imageView,GalleryViewFrag.OnFragmentInteractionListener m, int width, int height) {
        TARGET_IMAGE_VIEW_WIDTH = width;
        TARGET_IMAGE_VIEW_HEIGHT = height;
        mListener = m;
        imageViewReferences = new WeakReference(imageView);
    }
    @Override
    protected Bitmap doInBackground(File... params) {
        // return BitmapFactory.decodeFile(params[0].getAbsolutePath());
        mImageFile = params[0];
        // return decodeBitmapFromFile(params[0]);
        Bitmap b = decodeBitmapFromFile(mImageFile);
        mListener.onFragmentInteraction(mImageFile,false);
        return b;
    }
    @Override
    protected void onPostExecute(Bitmap bitmap) {

        if(bitmap != null && imageViewReferences != null) {
            ImageView imageView = imageViewReferences.get();
            if(imageView != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }
    private int calculateInSampleSize(BitmapFactory.Options bmOptions) {
        final int photoWidth = bmOptions.outWidth;
        final int photoHeight = bmOptions.outHeight;
        int scaleFactor = 1;
        if(photoWidth > TARGET_IMAGE_VIEW_WIDTH || photoHeight > TARGET_IMAGE_VIEW_HEIGHT) {
            final int halfPhotoWidth = photoWidth/2;
            final int halfPhotoHeight = photoHeight/2;
            while(halfPhotoWidth/scaleFactor > TARGET_IMAGE_VIEW_WIDTH
                    || halfPhotoHeight/scaleFactor > TARGET_IMAGE_VIEW_HEIGHT) {
                scaleFactor *= 2;
            }
        }
        return scaleFactor;
    }
    private Bitmap decodeBitmapFromFile(File imageFile) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFile.getAbsolutePath(), bmOptions);
        bmOptions.inSampleSize = calculateInSampleSize(bmOptions);
        bmOptions.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(imageFile.getAbsolutePath(), bmOptions);
    }

    public File getImageFile() {
        return mImageFile;
    }
}