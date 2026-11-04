package com.pixel.mediachip;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.animation.DecelerateInterpolator;

import androidx.core.graphics.ColorUtils;

public final class SquigglyProgress extends Drawable {
    public boolean animate = false;
    public boolean drawRemainingLine = true;
    public ValueAnimator heightAnimator = null;
    public float heightFraction = 0.0f;
    public long lastFrameTime = -1L;
    public float lineAmplitude;
    public final Paint linePaint;
    public final Paint mThumbPaint;
    public final RectF mThumbRect = new RectF();
    public final float mDensity;
    public final float matchedWaveEndpoint = 0.6f;
    public final float minWaveEndpoint = 0.2f;
    public final Path path = new Path();
    public float phaseOffset = 0.0f;
    public float phaseSpeed;
    public float strokeWidth;
    public boolean transitionEnabled = true;
    public final float transitionPeriods = 1.5f;
    public float waveLength;
    public final Paint wavePaint;

    private int mPrimaryColor = 0xFF8AB4F8;
    private float mProgressFraction = 0.0f;

    public SquigglyProgress(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        this.mDensity = density;
        this.waveLength = 22.0f * density;
        this.lineAmplitude = 2.0f * density;
        this.phaseSpeed = 26.0f * density;
        this.strokeWidth = 2.5f * density;

        Paint wp = new Paint(Paint.ANTI_ALIAS_FLAG);
        wp.setStrokeCap(Paint.Cap.ROUND);
        wp.setStyle(Paint.Style.STROKE);
        wp.setStrokeWidth(this.strokeWidth);
        this.wavePaint = wp;

        Paint lp = new Paint(Paint.ANTI_ALIAS_FLAG);
        lp.setStrokeCap(Paint.Cap.ROUND);
        lp.setStyle(Paint.Style.STROKE);
        lp.setStrokeWidth(this.strokeWidth);
        this.linePaint = lp;

        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setStyle(Paint.Style.FILL);
        this.mThumbPaint = tp;

        updateColors(mPrimaryColor, 255);
    }

    private ValueAnimator mWaveAnimator = null;

    public void setProgressFraction(float fraction) {
        this.mProgressFraction = Math.max(0.0f, Math.min(1.0f, fraction));
        setLevel((int) (this.mProgressFraction * 10000));
        invalidateSelf();
    }

    public float getProgressFraction() {
        return this.mProgressFraction;
    }

    public void setWaveColor(int color) {
        this.mPrimaryColor = color;
        updateColors(color, wavePaint.getAlpha());
        invalidateSelf();
    }

    public void setStrokeWidthDp(float dp, Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        this.strokeWidth = dp * density;
        this.wavePaint.setStrokeWidth(this.strokeWidth);
        this.linePaint.setStrokeWidth(this.strokeWidth);
        invalidateSelf();
    }

    public void updateColors(int color, int alpha) {
        this.wavePaint.setColor(ColorUtils.setAlphaComponent(color, alpha));
        this.linePaint.setColor(ColorUtils.setAlphaComponent(color, Math.round((alpha / 255.0f) * 77)));
        this.mThumbPaint.setColor(ColorUtils.setAlphaComponent(color, alpha));
    }

    public void setAnimate(boolean z) {
        if (this.animate == z && mWaveAnimator != null && mWaveAnimator.isRunning()) return;
        this.animate = z;
        if (z) {
            this.lastFrameTime = SystemClock.uptimeMillis();
            startWaveLoop();
        } else {
            stopWaveLoop();
        }

        if (this.heightAnimator != null) {
            this.heightAnimator.cancel();
        }

        ValueAnimator va = ValueAnimator.ofFloat(this.heightFraction, this.animate ? 1.0f : 0.0f);
        if (this.animate) {
            va.setStartDelay(40L);
            va.setDuration(600L);
            va.setInterpolator(new DecelerateInterpolator(1.8f));
        } else {
            va.setDuration(400L);
            va.setInterpolator(new DecelerateInterpolator(1.5f));
        }

        va.addUpdateListener(animation -> {
            this.heightFraction = (float) animation.getAnimatedValue();
            invalidateSelf();
        });
        va.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                heightAnimator = null;
            }
        });
        va.start();
        this.heightAnimator = va;
        invalidateSelf();
    }

    private void startWaveLoop() {
        if (mWaveAnimator != null) mWaveAnimator.cancel();
        mWaveAnimator = ValueAnimator.ofFloat(0f, 1f);
        mWaveAnimator.setDuration(1000L);
        mWaveAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mWaveAnimator.setInterpolator(new android.view.animation.LinearInterpolator());
        mWaveAnimator.addUpdateListener(anim -> {
            if (!this.animate) return;
            long uptime = SystemClock.uptimeMillis();
            if (this.lastFrameTime > 0) {
                float dt = (uptime - this.lastFrameTime) / 1000.0f;
                this.phaseOffset = (this.phaseOffset + dt * this.phaseSpeed) % this.waveLength;
            }
            this.lastFrameTime = uptime;
            invalidateSelf();
        });
        mWaveAnimator.start();
    }

    private void stopWaveLoop() {
        if (mWaveAnimator != null) {
            mWaveAnimator.cancel();
            mWaveAnimator = null;
        }
        this.lastFrameTime = -1L;
        invalidateSelf();
    }

    private float computeWaveY(float x, float direction, float f3) {
        float fLerpInvSat;
        float f;
        if (this.transitionEnabled) {
            float halfTransition = (this.transitionPeriods * this.waveLength) / 2.0f;
            float minVal = f3 - halfTransition;
            float maxVal = f3 + halfTransition;
            float norm = (x - maxVal) / (minVal - maxVal);
            fLerpInvSat = Math.max(0.0f, Math.min(1.0f, norm));
            f = direction * this.heightFraction * this.lineAmplitude;
        } else {
            f = direction * this.heightFraction;
            fLerpInvSat = this.lineAmplitude;
        }
        return f * fLerpInvSat;
    }

    @Override
    public void draw(Canvas canvas) {
        if (this.animate) {
            invalidateSelf();
            long uptime = SystemClock.uptimeMillis();
            if (this.lastFrameTime > 0) {
                this.phaseOffset = ((((uptime - this.lastFrameTime) / 1000.0f) * this.phaseSpeed) + this.phaseOffset) % this.waveLength;
            }
            this.lastFrameTime = uptime;
        }

        float level = getLevel() / 10000.0f;
        float fWidth = getBounds().width();
        if (fWidth <= 0) return;

        float f = fWidth * level;
        float effectiveLevel = level;
        if (this.transitionEnabled) {
            float f2 = this.matchedWaveEndpoint;
            if (effectiveLevel <= f2 && f2 > 0) {
                float norm = effectiveLevel / f2;
                effectiveLevel = this.minWaveEndpoint + (f2 - this.minWaveEndpoint) * norm;
            }
        }
        float f3 = effectiveLevel * fWidth;
        float f4 = (-this.phaseOffset) - (this.waveLength / 2.0f);
        float f5 = this.transitionEnabled ? fWidth : f3;

        this.path.rewind();
        this.path.moveTo(f4, 0.0f);

        float f6 = 1.0f;
        float f8 = computeWaveY(f4, 1.0f, f3);
        float f7 = this.waveLength / 2.0f;
        float f9 = f4;

        while (f9 < f5 + f7) {
            f6 = -f6;
            float f10 = f9 + f7;
            float f11 = (f7 / 2.0f) + f9;
            float fFloatValue2 = computeWaveY(f10, f6, f3);
            this.path.cubicTo(f11, f8, f11, fFloatValue2, f10, fFloatValue2);
            f8 = fFloatValue2;
            f9 = f10;
        }

        float f12 = this.lineAmplitude + this.strokeWidth + 4.0f;
        canvas.save();
        canvas.translate(getBounds().left, getBounds().centerY());

        // 1. Draw played squiggly wave
        canvas.save();
        float f13 = -f12;
        canvas.clipRect(0.0f, f13, f, f12);
        canvas.drawPath(this.path, this.wavePaint);
        canvas.restore();

        // 2. Draw unplayed remaining track (faded flat or transitioning)
        if (this.drawRemainingLine && f < fWidth) {
            canvas.save();
            canvas.clipRect(f, f13, fWidth, f12);
            if (this.transitionEnabled && this.heightFraction > 0.01f) {
                canvas.drawPath(this.path, this.linePaint);
            } else {
                canvas.drawLine(f, 0.0f, fWidth, 0.0f, this.linePaint);
            }
            canvas.restore();
        }

        // 3. Draw vertical thumb pillar ("столбик") at f
        // width = 4dp, height = 16dp, corner radius = 2dp
        float thumbW = 4.0f * this.mDensity;
        float thumbH = 16.0f * this.mDensity;
        float thumbR = 2.0f * this.mDensity;
        this.mThumbRect.set(f - thumbW / 2.0f, -thumbH / 2.0f, f + thumbW / 2.0f, thumbH / 2.0f);
        canvas.drawRoundRect(this.mThumbRect, thumbR, thumbR, this.mThumbPaint);

        canvas.restore();
    }

    @Override
    public int getAlpha() {
        return this.wavePaint.getAlpha();
    }

    @Override
    public void setAlpha(int alpha) {
        updateColors(this.mPrimaryColor, alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.wavePaint.setColorFilter(colorFilter);
        this.linePaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public boolean onLevelChange(int level) {
        this.mProgressFraction = level / 10000.0f;
        invalidateSelf();
        return true;
    }

    @Override
    public void setTint(int tintColor) {
        setWaveColor(tintColor);
    }

    @Override
    public void setTintList(ColorStateList tint) {
        if (tint != null) {
            setWaveColor(tint.getDefaultColor());
        }
    }
}
