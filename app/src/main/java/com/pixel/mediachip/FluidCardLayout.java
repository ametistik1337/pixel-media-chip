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
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Fluid container layout with organic droplet metamorphosis and coalescence.
 * Clips child views to the evolving droplet/card silhouette during stretch and bloom.
 */
public class FluidCardLayout extends FrameLayout {

    private static final float PINCH_THRESHOLD = 0.42f;
    private static final float RECOIL_DURATION = 0.13f;

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mClipPath = new Path();
    private final Path mMetaballPath = new Path();
    private final Path mMeniscusPath = new Path();

    private final RectF mChipBounds = new RectF();
    private final RectF mTargetCardBounds = new RectF();
    private final RectF mCurrentCardBounds = new RectF();

    private float mProgress = 0f;
    private boolean mIsAnimating = false;
    private int mBackgroundColor = Color.parseColor("#1A1C1E");

    private ValueAnimator mAnimator;

    public FluidCardLayout(Context context) {
        super(context);
        init();
    }

    public FluidCardLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        mPaint.setColor(mBackgroundColor);
        mPaint.setStyle(Paint.Style.FILL);
    }

    public void setFluidColor(int color) {
        this.mBackgroundColor = color;
        mPaint.setColor(color);
        invalidate();
    }

    public void startExpandAnimation(RectF chipBoundsInOverlay, RectF targetCardBoundsInOverlay, Runnable onEnd) {
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
        mIsAnimating = true;

        float remaining = Math.abs(1.0f - startVal);
        long duration = Math.max(70L, Math.round(M3Motion.DURATION_POPUP_ENTER * remaining));

        mAnimator = ValueAnimator.ofFloat(startVal, 1.0f);
        mAnimator.setDuration(duration);
        mAnimator.setInterpolator(M3Motion.DROPLET_PULL_ENTER);
        mAnimator.addUpdateListener(animation -> {
            mProgress = (float) animation.getAnimatedValue();
            updateFluidGeometry();
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
                mIsAnimating = false;
                updateFluidGeometry();
                invalidate();
                if (onEnd != null) onEnd.run();
            }
        });
        mAnimator.start();
    }

    public void startCollapseAnimation(RectF chipBoundsInOverlay, RectF targetCardBoundsInOverlay, Runnable onEnd) {
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
        mIsAnimating = true;

        float remaining = Math.abs(0.0f - startVal);
        long duration = Math.max(70L, Math.round(M3Motion.DURATION_POPUP_EXIT * remaining));

        mAnimator = ValueAnimator.ofFloat(startVal, 0.0f);
        mAnimator.setDuration(duration);
        mAnimator.setInterpolator(M3Motion.DROPLET_COALESCE_EXIT);
        mAnimator.addUpdateListener(animation -> {
            mProgress = (float) animation.getAnimatedValue();
            updateFluidGeometry();
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
                mIsAnimating = false;
                updateFluidGeometry();
                invalidate();
                if (onEnd != null) onEnd.run();
            }
        });
        mAnimator.start();
    }

    private void updateFluidGeometry() {
        if (mProgress <= 0f) {
            mCurrentCardBounds.set(mChipBounds);
            setChildrenAlpha(0f);
            return;
        }
        if (mProgress >= 1f) {
            mCurrentCardBounds.set(mTargetCardBounds);
            setChildrenAlpha(1f);
            return;
        }

        float yFrac = (float) Math.sin(mProgress * (Math.PI / 2.0));
        float chipCX = mChipBounds.centerX();
        float targetCX = mTargetCardBounds.centerX();
        float xFrac = (float) Math.pow(mProgress, 1.35);

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

        float bounce = M3Motion.calculateDropletBounce(mProgress);
        if (bounce != 1.0f) {
            float bW = currentW * bounce;
            float bH = currentH * bounce;
            left = currentCX - bW / 2f;
            right = currentCX + bW / 2f;
            bottom = top + bH;
        }

        mCurrentCardBounds.set(left, top, right, bottom);

        // Content reveals only after droplet detachment snap (p >= 0.38)
        float contentAlpha = Math.max(0f, Math.min(1.0f, (mProgress - 0.38f) / 0.50f));
        setChildrenAlpha(contentAlpha);
    }

    private void setChildrenAlpha(float alpha) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child != null) {
                child.setAlpha(alpha);
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (mProgress <= 0f && mIsAnimating) return;

        mClipPath.reset();
        mMetaballPath.reset();
        mMeniscusPath.reset();

        float density = getResources().getDisplayMetrics().density;
        float chipRadius = mChipBounds.height() / 2f;
        float finalCardRadius = 28f * density;
        float currentCardRadius = M3Motion.calculateDropletCornerRadius(mProgress, chipRadius * 1.5f, finalCardRadius);

        if (mProgress >= 1f && !mIsAnimating) {
            mClipPath.addRoundRect(mTargetCardBounds, finalCardRadius, finalCardRadius, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(mClipPath);
            canvas.drawColor(mBackgroundColor);
            super.dispatchDraw(canvas);
            canvas.restore();
            return;
        }

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

            mMetaballPath.addPath(mMeniscusPath);
        } else if (mProgress < PINCH_THRESHOLD + RECOIL_DURATION) {
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
                mMetaballPath.addPath(mMeniscusPath);
            }
        }

        // 2. Lower Droplet Body
        mMetaballPath.addRoundRect(mCurrentCardBounds, currentCardRadius, currentCardRadius, Path.Direction.CW);

        // Draw and Clip
        canvas.save();
        canvas.clipPath(mMetaballPath);
        canvas.drawColor(mBackgroundColor);
        super.dispatchDraw(canvas);
        canvas.restore();
    }
}
