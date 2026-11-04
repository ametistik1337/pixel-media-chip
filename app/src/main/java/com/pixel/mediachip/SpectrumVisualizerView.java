package com.pixel.mediachip;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * 24-band Pill Spectrum Visualizer for VanillaMediaPopup card.
 * Features organic rounded pills, Monet dynamic vertical gradient, and smooth spring physics.
 */
public class SpectrumVisualizerView extends View implements AudioVisualizerController.VisualizerListener {
    private static final int BARS_COUNT = AudioVisualizerController.NUM_BINS;

    private final Paint mBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mBarRect = new RectF();
    private final float[] mMagnitudes = new float[BARS_COUNT];

    private int mMonetColor = Color.parseColor("#D0BCFF");
    private float mBarWidthPx;
    private float mMinBarHeightPx;
    private boolean mIsActive = false;

    public SpectrumVisualizerView(Context context) {
        this(context, null);
    }

    public SpectrumVisualizerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpectrumVisualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        mBarWidthPx = 3.5f * density;
        mMinBarHeightPx = 3.5f * density;
    }

    public void setMonetColor(int color) {
        if (mMonetColor != color) {
            mMonetColor = color;
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
                    mMagnitudes[i] = 0f;
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
        if (magnitudes == null) return;
        int len = Math.min(magnitudes.length, BARS_COUNT);
        System.arraycopy(magnitudes, 0, mMagnitudes, 0, len);
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        float availableH = height - getPaddingTop() - getPaddingBottom();
        if (availableH <= mMinBarHeightPx) return;

        float maxDynamicH = availableH - mMinBarHeightPx;
        float totalBarsW = BARS_COUNT * mBarWidthPx;
        float gap = (width - getPaddingLeft() - getPaddingRight() - totalBarsW) / (BARS_COUNT - 1);
        if (gap < 0) gap = 1.0f;

        float cornerRadius = mBarWidthPx * 0.5f;
        float startX = getPaddingLeft();
        float bottomY = height - getPaddingBottom();

        // Top and bottom Monet gradient colors
        int colorTop = mMonetColor;
        int colorBottom = Color.argb(
                70,
                Color.red(mMonetColor),
                Color.green(mMonetColor),
                Color.blue(mMonetColor)
        );

        for (int i = 0; i < BARS_COUNT; i++) {
            float x = startX + i * (mBarWidthPx + gap);
            float mag = mMagnitudes[i];
            float barH = mMinBarHeightPx + mag * maxDynamicH;
            float topY = bottomY - barH;

            mBarRect.set(x, topY, x + mBarWidthPx, bottomY);

            LinearGradient grad = new LinearGradient(
                    x, topY, x, bottomY,
                    colorTop, colorBottom,
                    Shader.TileMode.CLAMP
            );
            mBarPaint.setShader(grad);

            canvas.drawRoundRect(mBarRect, cornerRadius, cornerRadius, mBarPaint);
        }
    }
}
