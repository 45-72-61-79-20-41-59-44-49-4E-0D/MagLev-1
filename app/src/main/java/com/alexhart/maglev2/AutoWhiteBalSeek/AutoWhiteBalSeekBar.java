package com.alexhart.maglev2.AutoWhiteBalSeek;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;

/**
 *
 */

//TODO fix auto white bal
public class AutoWhiteBalSeekBar extends SeekBar {

    private int mProgress;
    private AutoWhiteBalSeekBar mAutoWhiteBalSeekBar = this;
    private OnAwbSeekBarChangeListener mOnAwbSeekBarChangeListener;

    public AutoWhiteBalSeekBar(Context context) {
        super(context);
        init();
    }

    public AutoWhiteBalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AutoWhiteBalSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public AutoWhiteBalSeekBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void setmOnAwbSeekBarChangeListener(OnAwbSeekBarChangeListener mOnAwbSeekBarChangeListener) {
        this.mOnAwbSeekBarChangeListener = mOnAwbSeekBarChangeListener;
    }

    private void init() {
        this.setMax(70);
        this.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mProgress = progress;
                if (mOnAwbSeekBarChangeListener != null) {
                    if (0 <= mProgress && mProgress < 5) {
                        mOnAwbSeekBarChangeListener.handleProgress1();
                    } else if (5 <= mProgress && mProgress < 15) {
                        mOnAwbSeekBarChangeListener.handleProgress2();
                    } else if (15 <= mProgress && mProgress < 25) {
                        mOnAwbSeekBarChangeListener.handleProgress3();
                    } else if (25 <= mProgress && mProgress < 35) {
                        mOnAwbSeekBarChangeListener.handleProgress4();
                    } else if (35 <= mProgress && mProgress < 45) {
                        mOnAwbSeekBarChangeListener.handleProgress5();
                    } else if (45 <= mProgress && mProgress < 55) {
                        mOnAwbSeekBarChangeListener.handleProgress6();
                    } else if (55 <= mProgress && mProgress < 65) {
                        mOnAwbSeekBarChangeListener.handleProgress7();
                    } else if (65 <= mProgress && mProgress < 70) {
                        mOnAwbSeekBarChangeListener.handleProgress8();
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mOnAwbSeekBarChangeListener.onStartTrackingTouch(seekBar);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int num = 0;
                if (0 <= mProgress && mProgress < 5) {
                    mAutoWhiteBalSeekBar.setProgress(0);
                    num = 0;
                } else if (5 <= mProgress && mProgress < 15) {
                    mAutoWhiteBalSeekBar.setProgress(10);
                    num = 10;
                } else if (15 <= mProgress && mProgress < 25) {
                    mAutoWhiteBalSeekBar.setProgress(20);
                    num = 20;
                } else if (25 <= mProgress && mProgress < 35) {
                    mAutoWhiteBalSeekBar.setProgress(30);
                    num = 30;
                } else if (35 <= mProgress && mProgress < 45) {
                    mAutoWhiteBalSeekBar.setProgress(40);
                    num = 40;
                } else if (45 <= mProgress && mProgress < 55) {
                    mAutoWhiteBalSeekBar.setProgress(50);
                    num = 50;
                } else if (55 <= mProgress && mProgress < 65) {
                    mAutoWhiteBalSeekBar.setProgress(60);
                    num = 60;
                } else if (65 <= mProgress && mProgress < 70) {
                    mAutoWhiteBalSeekBar.setProgress(70);
                    num = 70;
                }
                if (mOnAwbSeekBarChangeListener != null) {
                    mOnAwbSeekBarChangeListener.onStopTrackingTouch(num);
                }
            }
        });
    }

    public interface OnAwbSeekBarChangeListener {
        public abstract void handleProgress1();

        public abstract void handleProgress2();

        public abstract void handleProgress3();

        public abstract void handleProgress4();

        public abstract void handleProgress5();

        public abstract void handleProgress6();

        public abstract void handleProgress7();

        public abstract void handleProgress8();

        public abstract void onStopTrackingTouch(int num);

        public abstract void onStartTrackingTouch(SeekBar seekBar);
    }

}
