package com.pixel.mediachip;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/**
 * Organic Droplet Metamorphosis & Coalescence Overlay.
 * Implements surface tension meniscus elongation, capillary neck thinning,
 * snap detachment with recoil, and elastic liquid bounce.
 */
public class LiquidBridgeOverlayView extends View {

    private static final float PINCH_THRESHOLD = 0.42f;
    private static final float RECOIL_DURATION = 0.13f;

    private final Paint mFluidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mFluidPath = new Path();
    private final Path mMeniscusPath = new Path();

    private final RectF mChipBounds = new RectF();
    private final RectF mCurrentCardBounds = new RectF();
    private final RectF mTargetCardBounds = new RectF();

    private float mProgress = 0f;
    private int mChipColor = Color.parseColor("#4C8BF5");
    private int mCardColor = Color.parseColor("#1E2024");

    public interface OnLiquidUpdateListener {
        void onUpdate(float progress, float cardX, float cardY, float scaleX, float scaleY, float alpha);
    }

    private OnLiquidUpdateListener mListener;
    private ValueAnimator mAnimator;

    public LiquidBridgeOverlayView(Context context) {
        super(context);
        mFluidPaint.setStyle(Paint.Style.FILL);
    }

    public void setColors(int chipColor, int cardColor) {
        this.mChipColor = chipColor;
        this.mCardColor = cardColor;
        updateShader();
        invalidate();
    }

    private void updateShader() {
        if (mTargetCardBounds.bottom > 0) {
            LinearGradient gradient = new LinearGradient(
                    0, mChipBounds.top,
                    0, mTargetCardBounds.bottom,
                    new int[]{mChipColor, mChipColor, mCardColor},
                    new float[]{0f, 0.25f, 1.0f},
                    Shader.TileMode.CLAMP
            );
            mFluidPaint.setShader(gradient);
        } else {
            mFluidPaint.setShader(null);
            mFluidPaint.setColor(mChipColor);
        }
    }

    public void setOnLiquidUpdateListener(OnLiquidUpdateListener listener) {
        this.mListener = listener;
    }

    public void startLiquidExpansion(RectF chipBoundsInOverlay, RectF targetCardBoundsInOverlay, Runnable onEnd) {
        float startVal = mProgress;
        if (mAnimator != null) {
            if (mAnimator.isRunning()) {
                startVal = (Float) mAnimator.getAnimatedValue();
            }
            mAnimator.removeAllListeners();
            mAnimator.cancel();
            mAnimator = null;
        }

        mChipBounds.set(chipBoundsInOverlay);
        mTargetCardBounds.set(targetCardBoundsInOverlay);
        updateShader();

        float remaining = Math.abs(1.0f - startVal);
        long duration = Math.max(70L, Math.round(M3Motion.DURATION_POPUP_ENTER * remaining));

        mAnimator = ValueAnimator.ofFloat(startVal, 1.0f);
        mAnimator.setDuration(duration);
        mAnimator.setInterpolator(M3Motion.DROPLET_PULL_ENTER);
        mAnimator.addUpdateListener(animation -> {
            mProgress = (float) animation.getAnimatedValue();
            computeAndNotify();
            invalidate();
        });
        mAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean mCanceled = false;

            @Override
            public void onAnimationCancel(Animator animation) {
                mCanceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mCanceled) return;
                mProgress = 1.0f;
                computeAndNotify();
                invalidate();
                if (onEnd != null) onEnd.run();
            }
        });
        mAnimator.start();
    }

    public void startLiquidCollapse(RectF chipBoundsInOverlay, RectF targetCardBoundsInOverlay, Runnable onEnd) {
        float startVal = mProgress;
        if (mAnimator != null) {
            if (mAnimator.isRunning()) {
                startVal = (Float) mAnimator.getAnimatedValue();
            }
            mAnimator.removeAllListeners();
            mAnimator.cancel();
            mAnimator = null;
        }

        mChipBounds.set(chipBoundsInOverlay);
        mTargetCardBounds.set(targetCardBoundsInOverlay);
        updateShader();

        float remaining = Math.abs(0.0f - startVal);
        long duration = Math.max(70L, Math.round(M3Motion.DURATION_POPUP_EXIT * remaining));

        mAnimator = ValueAnimator.ofFloat(startVal, 0.0f);
        mAnimator.setDuration(duration);
        mAnimator.setInterpolator(M3Motion.DROPLET_COALESCE_EXIT);
        mAnimator.addUpdateListener(animation -> {
            mProgress = (float) animation.getAnimatedValue();
            computeAndNotify();
            invalidate();
        });
        mAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean mCanceled = false;

            @Override
            public void onAnimationCancel(Animator animation) {
                mCanceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mCanceled) return;
                mProgress = 0.0f;
                computeAndNotify();
                invalidate();
                if (onEnd != null) onEnd.run();
            }
        });
        mAnimator.start();
    }

    private void computeAndNotify() {
        if (mProgress <= 0f) {
            mCurrentCardBounds.set(mChipBounds);
            if (mListener != null) {
                mListener.onUpdate(0f, mChipBounds.left, mChipBounds.top, 0.12f, 0.12f, 0f);
            }
            return;
        }
        if (mProgress >= 1f) {
            mCurrentCardBounds.set(mTargetCardBounds);
            if (mListener != null) {
                mListener.onUpdate(1f, mTargetCardBounds.left, mTargetCardBounds.top, 1.0f, 1.0f, 1.0f);
            }
            return;
        }

        // Fluid vertical descent under gravity and surface tension
        float yFrac = (float) Math.sin(mProgress * (Math.PI / 2.0));
        float chipCX = mChipBounds.centerX();
        float targetCX = mTargetCardBounds.centerX();
        float xFrac = (float) Math.pow(mProgress, 1.35);

        // Droplet lateral bloom: remains compact during droplet necking, blooms after pinch
        float density = getResources().getDisplayMetrics().density;
        float initialDropletW = Math.max(48f * density, mChipBounds.height() * 1.5f);
        float targetW = mTargetCardBounds.width();
        float bloomFactor = Math.max(0f, (mProgress - 0.20f) / 0.80f);
        float currentW = initialDropletW + (targetW - initialDropletW) * (float) Math.pow(bloomFactor, 1.4);

        float currentCX = chipCX + (targetCX - chipCX) * xFrac;
        float left = currentCX - currentW / 2f;
        float right = currentCX + currentW / 2f;

        float top = mChipBounds.bottom + (mTargetCardBounds.top - mChipBounds.bottom) * yFrac;
        float targetH = mTargetCardBounds.height();
        float initialDropletH = Math.max(36f * density, mChipBounds.height() * 1.2f);
        float currentH = initialDropletH + (targetH - initialDropletH) * (float) Math.pow(mProgress, 0.95);
        float bottom = top + currentH;

        // Apply elastic bounce to bounds when coalescing (p >= 0.75)
        float bounce = M3Motion.calculateDropletBounce(mProgress);
        if (bounce != 1.0f) {
            float bW = currentW * bounce;
            float bH = currentH * bounce;
            left = currentCX - bW / 2f;
            right = currentCX + bW / 2f;
            bottom = top + bH;
        }

        mCurrentCardBounds.set(left, top, right, bottom);

        float scaleX = Math.max(0.10f, (right - left) / mTargetCardBounds.width());
        float scaleY = Math.max(0.10f, (bottom - top) / mTargetCardBounds.height());
        // Content emerges after detachment snap (p >= 0.38)
        float alpha = Math.max(0f, Math.min(1f, (mProgress - 0.38f) / 0.50f));

        if (mListener != null) {
            mListener.onUpdate(mProgress, left, top, scaleX, scaleY, alpha);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mProgress <= 0f) return;

        mFluidPath.reset();
        mMeniscusPath.reset();

        float density = getResources().getDisplayMetrics().density;
        float chipRadius = mChipBounds.height() / 2f;
        float finalCardRadius = 28f * density;
        float currentCardRadius = M3Motion.calculateDropletCornerRadius(mProgress, chipRadius * 1.5f, finalCardRadius);

        // 1. Organic Meniscus & Capillary Neck (p < PINCH_THRESHOLD)
        if (mProgress < PINCH_THRESHOLD) {
            float initialWaist = (mChipBounds.right - mChipBounds.left) * 0.65f;
            float minWaist = 4f * density;
            float waistW = M3Motion.calculateMeniscusWaist(mProgress, initialWaist, minWaist, PINCH_THRESHOLD);

            float chipCX = mChipBounds.centerX();
            float chipBot = mChipBounds.bottom;
            float cardTop = mCurrentCardBounds.top;
            float cardCX = mCurrentCardBounds.centerX();
            float gap = Math.max(1f, cardTop - chipBot);

            float norm = mProgress / PINCH_THRESHOLD;
            float chipAttachW = Math.max(waistW * 1.35f, (mChipBounds.width() * 0.75f) * (1.0f - norm * 0.6f));
            float cardAttachW = Math.max(waistW * 1.6f, (mCurrentCardBounds.width() * 0.65f) * (1.0f - norm * 0.5f));

            float waistY = chipBot + gap * 0.40f;
            float waistCX = chipCX + (cardCX - chipCX) * 0.35f;

            float chipLeft = chipCX - chipAttachW / 2f;
            float chipRight = chipCX + chipAttachW / 2f;
            float cardLeft = cardCX - cardAttachW / 2f;
            float cardRight = cardCX + cardAttachW / 2f;

            mMeniscusPath.moveTo(chipLeft, chipBot);
            // Left cubic Bezier: chip -> waist -> card
            mMeniscusPath.cubicTo(
                    chipLeft, chipBot + gap * 0.22f,
                    waistCX - waistW / 2f, waistY - gap * 0.18f,
                    waistCX - waistW / 2f, waistY
            );
            mMeniscusPath.cubicTo(
                    waistCX - waistW / 2f, waistY + gap * 0.18f,
                    cardLeft, cardTop - gap * 0.15f,
                    cardLeft, cardTop + 4f
            );
            mMeniscusPath.lineTo(cardRight, cardTop + 4f);
            // Right cubic Bezier: card -> waist -> chip
            mMeniscusPath.cubicTo(
                    cardRight, cardTop - gap * 0.15f,
                    waistCX + waistW / 2f, waistY + gap * 0.18f,
                    waistCX + waistW / 2f, waistY
            );
            mMeniscusPath.cubicTo(
                    waistCX + waistW / 2f, waistY - gap * 0.18f,
                    chipRight, chipBot + gap * 0.22f,
                    chipRight, chipBot
            );
            mMeniscusPath.close();

            mFluidPath.addPath(mMeniscusPath);
        } else if (mProgress < PINCH_THRESHOLD + RECOIL_DURATION) {
            // Capillary Snap Recoil into source chip
            float recoil = M3Motion.calculateMeniscusRecoil(mProgress, PINCH_THRESHOLD, RECOIL_DURATION);
            if (recoil > 0.02f) {
                float recoilH = 8f * density * recoil;
                float recoilW = 20f * density * recoil;
                float chipCX = mChipBounds.centerX();
                float chipBot = mChipBounds.bottom;

                mMeniscusPath.moveTo(chipCX - recoilW / 2f, chipBot);
                mMeniscusPath.cubicTo(
                        chipCX - recoilW / 2f, chipBot + recoilH * 0.8f,
                        chipCX + recoilW / 2f, chipBot + recoilH * 0.8f,
                        chipCX + recoilW / 2f, chipBot
                );
                mMeniscusPath.close();
                mFluidPath.addPath(mMeniscusPath);
            }
        }

        // 2. Droplet Body / Expanding Card
        if (mProgress < 1f) {
            mFluidPath.addRoundRect(mCurrentCardBounds, currentCardRadius, currentCardRadius, Path.Direction.CW);
        }

        canvas.drawPath(mFluidPath, mFluidPaint);
    }
}
