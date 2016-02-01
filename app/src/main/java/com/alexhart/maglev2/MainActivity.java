package com.alexhart.maglev2;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;

import java.io.File;

/**
 * MainActivity that will swap out fragments through pagerAdapter
 */

public class MainActivity extends AppCompatActivity implements GalleryViewFrag.OnFragmentInteractionListener{

    private File file;
    private boolean isVideo;

    ViewPager mViewPager = null;
    public static boolean inPreview = false;        //What is the use of this?

    private final static String TAG = "MainActivity";

    ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            if (position == 0) {
                inPreview = false;
            }
            else{
                inPreview = true;
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        FragmentManager fm = getSupportFragmentManager();
        mViewPager.setAdapter(new pagerAdapter(fm));
        mViewPager.addOnPageChangeListener(mOnPageChangeListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);
        invalidateOptionsMenu();
        return true;
    }

    @Override
    public void onFragmentInteraction(File file,boolean isVideo){
        this.file = file;
        this.isVideo = isVideo;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(inPreview){
            getSupportActionBar().hide();
        }
        else{
            getSupportActionBar().show();
        }
        return super.onPrepareOptionsMenu(menu);
    }

    public File getdatafile(){
        return file;
    }

    public boolean isVideo(){
        return isVideo;
    }
}

//dont need state pager adapter
class pagerAdapter extends FragmentPagerAdapter {

    public pagerAdapter (FragmentManager fm) {
        super(fm);
    }

    //return fragment at given position
    @Override
    public Fragment getItem(int position) {
//        Log.d("Frag", "Position: "+position);
        Fragment fragment = null;

        switch (position){
            case 0:
                fragment = new MagLevControlFrag();
                break;
            case 1:
                fragment = new PreviewFrag();
                break;
            case 2:
                fragment = new GalleryViewFrag();
                break;
            case 3:
                fragment = new HistogramFrag();
        }
        return fragment;
    }

    @Override
    public int getCount() {
        //# pages
        return 4;
    }

    @Override
    public CharSequence getPageTitle(int position) {
//        Log.d("MainActivity", "GetPageTitle");
        switch (position){
            case 0:
                return "MagLev Control";
            case 1:
                return "Preview";
            case 2:
                return "GalleryView";
            case 3:
                return "Histogram";
        }
        return null;
    }
}