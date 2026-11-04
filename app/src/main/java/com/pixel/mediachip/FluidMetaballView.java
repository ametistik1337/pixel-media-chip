package com.pixel.mediachip;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;

public class FluidMetaballView extends View {

    public interface OnProgressUpdateListener {
        void onProgress(float progress, RectF cardBounds, float bridgeAlpha);
    }

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mPath = new Path();

    private final RectF mChipBounds = new RectF();
    private final RectF mTargetCardBounds = new RectF();
    private final RectF mCurrentCardBounds = new RectF();

    private float mProgress = 0f;
    private float mBridgeAlpha = 0f;
    private int mFluidColor = Color.parseColor("#222222");

    private ValueAnimator mAnimator;
    private OnProgressUpdateListener mListener;

    private static final PathInterpolator FLUID_EXPAND_INTERPOLATOR =
            new PathInterpolator(0.12f, 0.90f, 0.20f, 1.12f);
    private static final PathInterpolator FLUID_COLLAPSE_INTERPOLATOR =
            new PathInterpolator(0.35f, 0.0f, 0.20f, 1.0f);

    public FluidMetaballView(Context context) {
        super(context);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mFluidColor);
    }

    public void setFluidColor(int color) {
        this.mFluidColor = color;
        mPaint.setColor(color);
        invalidate();
    }

    public void setOnProgressUpdateListener(OnProgressUpdateListener listener) {
        this.mListener = listener;
    }

    public void startExpand(RectF chipScreenBounds, RectF targetCardBounds, Runnable onComplete) {
        if (mAnimator != null) mAnimator.cancel();

        mChipBounds.set(chipScreenBounds);
        mTargetCardBounds.set(targetCardBounds);

        mAnimator = ValueAnimator.ofFloat(0f, 1f);
        mAnimator.setDuration(460);
        mAnimator.setInterpolator(FLUID_EXPAND_INTERPOLATOR);
        mAnimator.addUpdateListener(animation -> {
            mProgress = (float) animation.getAnimatedValue();
            updateCalculations();
            invalidate();
            if (mListener != null) {
                mListener.onProgress(mProgress, mCurrentCardBounds, mBridgeAlpha);
            }
        });
        mAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgress = 1f;
                updateCalculations();
                invalidate();
                if (onComplete != null) onComplete.run();
            }
        });
        mAnimator.start();
    }

    public void startCollapse(RectF chipScreenBounds, RectF targetCardBounds, Runnable onComplete) {
        if (mAnimator != null) mAnimator.cancel();

        mChipBounds.set(chipScreenBounds);
        mTargetCardBounds.set(targetCardBounds);

        mAnimator = ValueAnimator.ofFloat(1f, 0f);
        mAnimator.setDuration(280);
        mAnimator.setInterpolator(FLUID_COLLAPSE_INTERPOLATOR);
        mAnimator.addUpdateListener(animation -> {
            mProgress = (float) animation.getAnimatedValue();
            updateCalculations();
            invalidate();
            if (mListener != null) {
                mListener.onProgress(mProgress, mCurrentCardBounds, mBridgeAlpha);
            }
        });
        mAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgress = 0f;
                updateCalculations();
                invalidate();
                if (onComplete != null) onComplete.run();
            }
        });
        mAnimator.start();
    }

    private void updateCalculations() {
        // Interpolate card bounds from chip bounds to target card bounds
        // In early phase (0..0.4), card stretches downwards fast (droplet elongation)
        // In later phase (0.3..1.0), card blooms horizontally
        float yFrac = Math.min(1f, mProgress * 1.25f);
        float xFrac = Math.max(0f, (mProgress - 0.15f) / 0.85f);

        float left = mChipBounds.left + (mTargetCardBounds.left - mChipBounds.left) * xFrac;
        float right = mChipBounds.right + (mTargetCardBounds.right - mChipBounds.right) * xFrac;
        float top = mChipBounds.top + (mTargetCardBounds.top - mChipBounds.top) * yFrac;
        float bottom = mChipBounds.bottom + (mTargetCardBounds.bottom - mChipBounds.bottom) * yFrac;

        mCurrentCardBounds.set(left, top, right, bottom);

        // Bridge is strongest at progress 0.15 .. 0.55, then pinches off and snaps at 0.75
        if (mProgress < 0.10f) {
            mBridgeAlpha = mProgress / 0.10f;
        } else if (mProgress <= 0.60f) {
            mBridgeAlpha = 1.0f;
        } else if (mProgress <= 0.82f) {
            mBridgeAlpha = 1.0f - ((mProgress - 0.60f) / 0.22f);
        } else {
            mBridgeAlpha = 0.0f;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mProgress <= 0f) return;

        mPath.reset();

        float chipRadius = mChipBounds.height() / 2f;
        float cardRadius = 28f * getResources().getDisplayMetrics().density;
        float currentCardRadius = chipRadius + (cardRadius - chipRadius) * mProgress;

        // Draw Liquid Connecting Bridge (Metaball) if active
        if (mBridgeAlpha > 0.01f && mCurrentCardBounds.top > mChipBounds.bottom - 4) {
            mPaint.setAlpha((int) (mBridgeAlpha * 255));

            float chipLeft = mChipBounds.left + 8;
            float chipRight = mChipBounds.right - 8;
            float chipBottom = mChipBounds.bottom - 2;

            float cardLeft = mCurrentCardBounds.left + 16;
            float cardRight = mCurrentCardBounds.right - 16;
            float cardTop = mCurrentCardBounds.top + 4;

            float midY = (chipBottom + cardTop) / 2f;
            // Waist pinch effect (inward tension)
            float pinch = (cardTop - chipBottom) * 0.35f;
            float midLeft = (chipLeft + cardLeft) / 2f + pinch;
            float midRight = (chipRight + cardRight) / 2f - pinch;

            if (midRight > midLeft) {
                Path bridgePath = new Path();
                bridgePath.moveTo(chipLeft, chipBottom);
                bridgePath.cubicTo(chipLeft, midY, midLeft, midY, cardLeft, cardTop);
                bridgePath.lineTo(cardRight, cardTop);
                bridgePath.cubicTo(midRight, midY, chipRight, midY, chipRight, chipBottom);
                bridgePath.close();

                canvas.drawPath(bridgePath, mPaint);
            }
        }

        // Draw Expanding Droplet Body
        mPaint.setAlpha(255);
        canvas.drawRoundRect(mCurrentCardBounds, currentCardRadius, currentCardRadius, mPaint);
    }
}
