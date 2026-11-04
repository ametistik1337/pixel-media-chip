package com.pixel.mediachip;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LiquidRippleEffectView extends View {

    private static class RippleRing {
        float cx;
        float cy;
        float radius;
        float maxRadius;
        float alpha;
        int color;
    }

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<RippleRing> mRipples = new ArrayList<>();

    public LiquidRippleEffectView(Context context) {
        super(context);
        mPaint.setStyle(Paint.Style.STROKE);
    }

    public void emitRipple(float cx, float cy, int color) {
        RippleRing ring = new RippleRing();
        ring.cx = cx;
        ring.cy = cy;
        ring.radius = 0f;
        ring.maxRadius = 140f * getResources().getDisplayMetrics().density;
        ring.alpha = 0.75f;
        ring.color = color;

        mRipples.add(ring);

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(480);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float frac = animation.getAnimatedFraction();
            ring.radius = ring.maxRadius * frac;
            ring.alpha = 0.75f * (1f - frac);
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mRipples.remove(ring);
                invalidate();
            }
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mRipples.isEmpty()) return;

        for (RippleRing ring : mRipples) {
            mPaint.setColor(ring.color);
            mPaint.setAlpha((int) (ring.alpha * 255));
            mPaint.setStrokeWidth(Math.max(2f, 8f * (1f - ring.radius / ring.maxRadius)));
            canvas.drawCircle(ring.cx, ring.cy, ring.radius, mPaint);
        }
    }
}
