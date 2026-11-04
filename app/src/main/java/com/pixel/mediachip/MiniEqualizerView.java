package com.pixel.mediachip;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Compact 3-band Mini Equalizer indicator for the Pixel Status Bar media chip.
 * Displays live bouncing pills in place of the static media icon when playing.
 */
public class MiniEqualizerView extends View implements AudioVisualizerController.VisualizerListener {
    private static final int BARS_COUNT = 3;

    private final Paint mBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mBarRect = new RectF();
    private final float[] mBars = new float[BARS_COUNT];

    private int mColor = Color.WHITE;
    private float mBarWidthPx;
    private float mGapPx;
    private float mMinBarHeightPx;
    private boolean mIsActive = false;

    public MiniEqualizerView(Context context) {
        this(context, null);
    }

    public MiniEqualizerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MiniEqualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        mBarWidthPx = 2.2f * density;
        mGapPx = 2.0f * density;
        mMinBarHeightPx = 2.5f * density;
        mBarPaint.setStyle(Paint.Style.FILL);
        mBarPaint.setColor(mColor);
    }

    public void setColor(int color) {
        if (mColor != color) {
            mColor = color;
            mBarPaint.setColor(color);
            postInvalidateOnAnimation();
        }
    }

    public void setActive(boolean active) {
        if (mIsActive != active) {
            mIsActive = active;
            if (active) {
                AudioVisualizerController.get().registerListener(this);
            } else {
                AudioVisualizerController.get().unregisterListener(this);
                for (int i = 0; i < BARS_COUNT; i++) {
                    mBars[i] = 0f;
                }
                postInvalidateOnAnimation();
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mIsActive) {
            AudioVisualizerController.get().registerListener(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        AudioVisualizerController.get().unregisterListener(this);
    }

    @Override
    public void onFftData(float[] magnitudes) {
        if (magnitudes == null || magnitudes.length < 16) return;

        // Bar 0: Bass
        mBars[0] = (magnitudes[1] + magnitudes[2] + magnitudes[3]) / 3.0f;
        // Bar 1: Mid
        mBars[1] = (magnitudes[7] + magnitudes[8] + magnitudes[9] + magnitudes[10]) / 4.0f;
        // Bar 2: Treble
        mBars[2] = (magnitudes[15] + magnitudes[16] + magnitudes[17] + magnitudes[18]) / 4.0f;

        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        float totalW = BARS_COUNT * mBarWidthPx + (BARS_COUNT - 1) * mGapPx;
        float startX = (width - totalW) * 0.5f;
        float availableH = height - getPaddingTop() - getPaddingBottom();
        float bottomY = height - getPaddingBottom();
        float cornerRadius = mBarWidthPx * 0.5f;

        for (int i = 0; i < BARS_COUNT; i++) {
            float x = startX + i * (mBarWidthPx + mGapPx);
            float mag = mBars[i];
            float dynamicH = mMinBarHeightPx + mag * (availableH - mMinBarHeightPx);
            float topY = bottomY - dynamicH;

            mBarRect.set(x, topY, x + mBarWidthPx, bottomY);
            canvas.drawRoundRect(mBarRect, cornerRadius, cornerRadius, mBarPaint);
        }
    }
}
