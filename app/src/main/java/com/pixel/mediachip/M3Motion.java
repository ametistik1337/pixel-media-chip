package com.pixel.mediachip;

import android.animation.TimeInterpolator;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.PathInterpolator;

/**
 * Material 3 Expressive and Emil Kowalski design engineering motion tokens.
 * Enforces smooth spring physics (damping ratio 0.85-1.0), tactile press feedback (scale 0.97),
 * bounds-aware touch handling, and standard M3 duration scales.
 */
public class M3Motion {

    public interface HapticSupplier {
        boolean isHapticEnabled();
    }

    // Official M3 Curve Easings
    public static final TimeInterpolator EMPHASIZED = new PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f);
    public static final TimeInterpolator EMPHASIZED_DECELERATE = new PathInterpolator(0.05f, 0.7f, 0.1f, 1.0f);
    public static final TimeInterpolator EMPHASIZED_ACCELERATE = new PathInterpolator(0.3f, 0.0f, 0.8f, 0.15f);

    // Emil Kowalski Design Engineering Curves
    public static final TimeInterpolator EASE_OUT = new PathInterpolator(0.23f, 1.0f, 0.32f, 1.0f);
    public static final TimeInterpolator EASE_IN_OUT = new PathInterpolator(0.77f, 0.0f, 0.175f, 1.0f);
    public static final TimeInterpolator DRAWER_CURVE = new PathInterpolator(0.32f, 0.72f, 0.0f, 1.0f);
    public static final TimeInterpolator SPRING_POPUP_ENTER = new PathInterpolator(0.18f, 1.0f, 0.22f, 1.02f);
    public static final TimeInterpolator FAST_OUT_EXIT = new PathInterpolator(0.35f, 0.0f, 0.15f, 1.0f);

    // Organic Droplet Metamorphosis & Coalescence Curves
    // Enter: Smooth tensile stretch -> necking -> snap detachment -> elastic fluid splash settle
    public static final TimeInterpolator DROPLET_PULL_ENTER = new PathInterpolator(0.16f, 0.92f, 0.20f, 1.08f);
    // Exit: Rapid fluid contraction -> capillary attraction -> absorption into chip
    public static final TimeInterpolator DROPLET_COALESCE_EXIT = new PathInterpolator(0.32f, 0.0f, 0.12f, 1.0f);

    // M3 Expressive Smooth Spring Curves (gentle overshoot without jarring jitter)
    public static final TimeInterpolator SPRING_SPATIAL = new PathInterpolator(0.25f, 1.15f, 0.35f, 1.0f);
    public static final TimeInterpolator SPRING_EFFECTS = new PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f);

    // M3 Duration Tokens (ms)
    public static final int DURATION_SHORT1 = 50;
    public static final int DURATION_SHORT2 = 100;
    public static final int DURATION_SHORT3 = 150;
    public static final int DURATION_SHORT4 = 200;
    public static final int DURATION_MEDIUM1 = 250;
    public static final int DURATION_MEDIUM2 = 300;
    public static final int DURATION_MEDIUM3 = 350;
    public static final int DURATION_MEDIUM4 = 400;

    // Asymmetric Popup Transitions (Exit faster than Enter - Emil Kowalski rule)
    public static final int DURATION_POPUP_ENTER = DURATION_MEDIUM2; // 300ms
    public static final int DURATION_POPUP_EXIT = DURATION_SHORT4;  // 200ms
    public static final int STAGGER_DELAY_STEP = 28;                // 28ms between elements
    public static final int STAGGER_DURATION = 160;                 // 160ms item entrance

    /**
     * Organic Droplet Elastic Bounce:
     * Damped surface tension bounce when droplet coalesces into a card.
     * Starts at p=0.75, peaks at p~0.88 (+2.5%), smoothly settles to 1.0 at p=1.0.
     */
    public static float calculateDropletBounce(float p) {
        if (p < 0.75f) return 1.0f;
        if (p >= 1.0f) return 1.0f;
        float t = (p - 0.75f) / 0.25f; // 0..1
        return 1.0f + 0.035f * (float) Math.sin(t * Math.PI) * (1.0f - 0.45f * t);
    }

    /**
     * Meniscus Liquid Waist Thickness:
     * Models Rayleigh-Plateau capillary neck thinning under surface tension.
     * Pinches off smoothly to 0 at pinchThreshold.
     */
    public static float calculateMeniscusWaist(float p, float initialWaist, float minWaist, float pinchThreshold) {
        if (p <= 0f) return initialWaist;
        if (p >= pinchThreshold) return 0f;
        float norm = p / pinchThreshold; // 0..1
        float tension = (float) Math.pow(1.0f - norm, 1.35);
        return Math.max(minWaist * (1.0f - norm), initialWaist * tension);
    }

    /**
     * Meniscus Recoil:
     * Models capillary snap-back of the broken liquid bridge into the source chip.
     */
    public static float calculateMeniscusRecoil(float p, float pinchThreshold, float recoilDuration) {
        if (p <= pinchThreshold || p >= pinchThreshold + recoilDuration) return 0f;
        float frac = (p - pinchThreshold) / recoilDuration; // 0..1
        return (1.0f - frac) * (float) Math.cos(frac * Math.PI * 0.5);
    }

    /**
     * Droplet Corner Radius Morphing:
     * Transitions from a circular/elliptical droplet bulb to the concentric card radius.
     */
    public static float calculateDropletCornerRadius(float p, float dropletInitialRadius, float cardFinalRadius) {
        if (p <= 0f) return dropletInitialRadius;
        if (p >= 1f) return cardFinalRadius;
        // Delayed radius flattening: stays rounded while in droplet phase (p < 0.45)
        float morphProgress = Math.max(0f, (p - 0.35f) / 0.65f);
        float curvedP = (float) Math.pow(morphProgress, 1.5);
        return dropletInitialRadius + (cardFinalRadius - dropletInitialRadius) * curvedP;
    }

    /**
     * Concentric Corner Radius Formula:
     * R_inner = max(0, R_outer - padding)
     */
    public static int calculateConcentricRadius(int outerRadius, int padding) {
        return Math.max(0, outerRadius - padding);
    }

    /**
     * Emil Kowalski tactile button press principle:
     * Adds transform: scale(0.97) on press (ACTION_DOWN) in 100ms, springs back to 1.0 on release in 150ms.
     * Incorporates bounds-aware gesture tracking to prevent clicks when user drags finger away to scroll or cancel.
     */
    public static void attachTactilePress(View view, View.OnClickListener clickListener, HapticSupplier hapticSupplier) {
        view.setOnTouchListener(new View.OnTouchListener() {
            private boolean mIsPressedInside = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mIsPressedInside = true;
                        if (hapticSupplier != null && hapticSupplier.isHapticEnabled()) {
                            try {
                                v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                            } catch (Throwable ignored) {}
                        }
                        v.animate()
                                .scaleX(0.97f)
                                .scaleY(0.97f)
                                .setDuration(DURATION_SHORT2)
                                .setInterpolator(EASE_OUT)
                                .start();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        if (mIsPressedInside) {
                            boolean inside = event.getX() >= 0 && event.getX() <= v.getWidth()
                                    && event.getY() >= 0 && event.getY() <= v.getHeight();
                            if (!inside) {
                                mIsPressedInside = false;
                                v.animate()
                                        .scaleX(1.0f)
                                        .scaleY(1.0f)
                                        .setDuration(DURATION_SHORT3)
                                        .setInterpolator(SPRING_SPATIAL)
                                        .start();
                            }
                        } else {
                            boolean inside = event.getX() >= 0 && event.getX() <= v.getWidth()
                                    && event.getY() >= 0 && event.getY() <= v.getHeight();
                            if (inside) {
                                mIsPressedInside = true;
                                v.animate()
                                        .scaleX(0.97f)
                                        .scaleY(0.97f)
                                        .setDuration(DURATION_SHORT2)
                                        .setInterpolator(EASE_OUT)
                                        .start();
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        v.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(DURATION_SHORT3)
                                .setInterpolator(SPRING_SPATIAL)
                                .start();
                        if (mIsPressedInside) {
                            mIsPressedInside = false;
                            v.performClick();
                            if (clickListener != null) {
                                clickListener.onClick(v);
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        mIsPressedInside = false;
                        v.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(DURATION_SHORT3)
                                .setInterpolator(SPRING_SPATIAL)
                                .start();
                        return true;
                }
                return false;
            }
        });
    }

    public static void attachTactilePress(View view, View.OnClickListener clickListener, boolean hapticEnabled) {
        attachTactilePress(view, clickListener, () -> hapticEnabled);
    }

    /**
     * Animate appearance without jarring pops.
     * Emil Kowalski rule: Never animate from scale(0) — always scale(0.95) with opacity.
     */
    public static void animateEntrance(View view, int duration) {
        view.setScaleX(0.95f);
        view.setScaleY(0.95f);
        view.setAlpha(0.0f);
        view.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(duration > 0 ? duration : DURATION_MEDIUM1)
                .setInterpolator(EMPHASIZED_DECELERATE)
                .start();
    }
}
