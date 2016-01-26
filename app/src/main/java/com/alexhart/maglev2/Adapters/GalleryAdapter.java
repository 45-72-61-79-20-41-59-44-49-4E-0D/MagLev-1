package com.alexhart.maglev2.Adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;

import java.io.File;

/**
 * Created by Alex on 11/6/2015.
 */
public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder>{

    private File[] mGalleryFiles;
    private static RecyclerViewAdapterPositionInter mPositionInterface;

    private static int mImageWidth, mImageHeight;



    public GalleryAdapter(File[] directoryFile, int imageWidth, int imageHeight, RecyclerViewAdapterPositionInter positionInterface) {
        mImageWidth = imageWidth;
        mImageHeight = imageHeight;
        mGalleryFiles = directoryFile;
        mPositionInterface = positionInterface;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        ImageView imageView = new ImageView(parent.getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mImageWidth, mImageHeight);
        imageView.setLayoutParams(params);
        return new ViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        File viewFile = mGalleryFiles[position];


        //most efficient method
        Glide.with(holder.getImageView().getContext())
                .load(viewFile)
                .into(holder.getImageView());

    }

    @Override
    public int getItemCount() {
        return mGalleryFiles.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private ImageView imageView;

        public ViewHolder(View v) {
            super(v);
            v.setOnClickListener(this);
            imageView = (ImageView) v;

//            v.setOnTouchListener(new View.OnTouchListener() {
//                @Override
//                public boolean onTouch(View view, MotionEvent motionEvent) {
//                    return false;
//                }
//            });
        }

        public ImageView getImageView() {
            return imageView;
        }

        @Override
        public void onClick(View view) {
            mPositionInterface.getRecyclerViewAdapterPosition(this.getAdapterPosition());
        }


    }

}
