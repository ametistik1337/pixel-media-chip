package com.pixel.mediachip;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RuntimeShader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.PathInterpolator;

import androidx.core.graphics.ColorUtils;

import java.util.ArrayList;
import java.util.List;

public class SystemUiRippleView extends View {
    private static final String SHADER_SRC = ""
            + "uniform vec2 in_center;\n"
            + "uniform vec2 in_size;\n"
            + "uniform float in_cornerRadius;\n"
            + "uniform float in_thickness;\n"
            + "uniform float in_time;\n"
            + "uniform float in_distort_radial;\n"
            + "uniform float in_distort_xy;\n"
            + "uniform float in_fadeSparkle;\n"
            + "uniform float in_fadeFill;\n"
            + "uniform float in_fadeRing;\n"
            + "uniform float in_blur;\n"
            + "uniform float in_pixelDensity;\n"
            + "layout(color) uniform vec4 in_color;\n"
            + "uniform float in_sparkle_strength;\n"
            + "\n"
            + "float triangleNoise(vec2 n) {\n"
            + "    n  = fract(n * vec2(5.3987, 5.4421));\n"
            + "    n += dot(n.yx, n.xy + vec2(21.5351, 14.3137));\n"
            + "    float xy = n.x * n.y;\n"
            + "    return fract(xy * 95.4307) + fract(xy * 75.04961) - 1.0;\n"
            + "}\n"
            + "\n"
            + "const float PI = 3.1415926535897932384626;\n"
            + "\n"
            + "float sparkles(vec2 uv, float t) {\n"
            + "    float n = triangleNoise(uv);\n"
            + "    float s = 0.0;\n"
            + "    for (float i = 0.0; i < 4.0; i += 1.0) {\n"
            + "        float l = i * 0.01;\n"
            + "        float h = l + 0.1;\n"
            + "        float o = smoothstep(n - l, h, n);\n"
            + "        o *= abs(sin(PI * o * (t + 0.55 * i)));\n"
            + "        s += o;\n"
            + "    }\n"
            + "    return s;\n"
            + "}\n"
            + "\n"
            + "vec2 distort(vec2 p, float time, float distort_amount_radial, float distort_amount_xy) {\n"
            + "    float angle = atan(p.y, p.x);\n"
            + "    return p + vec2(sin(angle * 8.0 + time * 0.003 + 1.641),\n"
            + "                    cos(angle * 5.0 + 2.14 + time * 0.00412)) * distort_amount_radial\n"
            + "             + vec2(sin(p.x * 0.01 + time * 0.00215 + 0.8123),\n"
            + "                    cos(p.y * 0.01 + time * 0.005931)) * distort_amount_xy;\n"
            + "}\n"
            + "\n"
            + "float soften(float d, float blur) {\n"
            + "    float blurHalf = blur * 0.5;\n"
            + "    return smoothstep(-blurHalf, blurHalf, d);\n"
            + "}\n"
            + "\n"
            + "float subtract(float outer, float inner) {\n"
            + "    return max(outer, -inner);\n"
            + "}\n"
            + "\n"
            + "float sdCircle(vec2 p, float r) {\n"
            + "    return (length(p) - r) / r;\n"
            + "}\n"
            + "\n"
            + "float circleRing(vec2 p, float radius) {\n"
            + "    float thicknessHalf = radius * 0.25;\n"
            + "    float outerCircle = sdCircle(p, radius + thicknessHalf);\n"
            + "    float innerCircle = sdCircle(p, radius);\n"
            + "    return subtract(outerCircle, innerCircle);\n"
            + "}\n"
            + "\n"
            + "vec4 main(vec2 p) {\n"
            + "    vec2 p_distorted = distort(p, in_time, in_distort_radial, in_distort_xy);\n"
            + "    float radius = in_size.x * 0.5;\n"
            + "    float sparkleRing = soften(circleRing(p_distorted - in_center, radius), in_blur);\n"
            + "    float inside = soften(sdCircle(p_distorted - in_center, radius * 1.25), in_blur);\n"
            + "    float sparkle = sparkles(p - mod(p, in_pixelDensity * 0.8), in_time * 0.00175)\n"
            + "        * (1.0 - sparkleRing) * in_fadeSparkle;\n"
            + "\n"
            + "    float rippleInsideAlpha = (1.0 - inside) * in_fadeFill;\n"
            + "    float rippleRingAlpha = (1.0 - sparkleRing) * in_fadeRing;\n"
            + "    float rippleAlpha = max(rippleInsideAlpha, rippleRingAlpha) * in_color.a;\n"
            + "    vec4 ripple = vec4(in_color.rgb, 1.0) * rippleAlpha;\n"
            + "    return mix(ripple, vec4(sparkle), sparkle * in_sparkle_strength);\n"
            + "}\n";

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<ActiveRipple> mActiveRipples = new ArrayList<>();
    private static final PathInterpolator STANDARD_INTERPOLATOR = new PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f);

    public SystemUiRippleView(Context context) {
        super(context);
        init();
    }

    public SystemUiRippleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setWillNotDraw(false);
    }

    public void playRipple(float centerX, float centerY, int color) {
        try {
            ActiveRipple ripple = new ActiveRipple(centerX, centerY, color, getWidth() * 2f, getResources().getDisplayMetrics().density);
            mActiveRipples.add(ripple);
            ripple.animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mActiveRipples.remove(ripple);
                    invalidate();
                }
            });
            ripple.animator.start();
            invalidate();
        } catch (Throwable ignored) {}
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!canvas.isHardwareAccelerated() || mActiveRipples.isEmpty()) {
            return;
        }
        boolean isRunning = false;
        for (int i = 0; i < mActiveRipples.size(); i++) {
            ActiveRipple r = mActiveRipples.get(i);
            if (r.shader != null) {
                mPaint.setShader(r.shader);
                canvas.drawPaint(mPaint);
                if (r.animator.isRunning()) {
                    isRunning = true;
                }
            }
        }
        if (isRunning) {
            invalidate();
        }
    }

    private static class ActiveRipple {
        final RuntimeShader shader;
        final ValueAnimator animator;
        final float centerX;
        final float centerY;
        final float maxRadius;
        final float density;
        final int color;

        ActiveRipple(float cx, float cy, int col, float maxR, float dens) {
            this.centerX = cx;
            this.centerY = cy;
            this.color = col;
            this.maxRadius = maxR > 100f ? maxR : 800f;
            this.density = dens;

            RuntimeShader s = null;
            try {
                s = new RuntimeShader(SHADER_SRC);
                s.setFloatUniform("in_center", centerX, centerY);
                s.setFloatUniform("in_pixelDensity", density);
                s.setColorUniform("in_color", ColorUtils.setAlphaComponent(color, 120));
                s.setFloatUniform("in_sparkle_strength", 0.35f);
            } catch (Throwable ignored) {}
            this.shader = s;

            this.animator = ValueAnimator.ofFloat(0f, 1f);
            this.animator.setDuration(1200L);
            this.animator.addUpdateListener(anim -> {
                if (shader == null) return;
                float progress = (float) anim.getAnimatedValue();
                long playTime = anim.getCurrentPlayTime();

                float interpolated = STANDARD_INTERPOLATOR.getInterpolation(progress);
                float curSize = maxRadius * interpolated;

                shader.setFloatUniform("in_size", curSize, curSize);
                shader.setFloatUniform("in_thickness", curSize * 0.25f);
                shader.setFloatUniform("in_cornerRadius", curSize * 0.5f);
                shader.setFloatUniform("in_blur", -0.75f * interpolated + 1.25f);
                shader.setFloatUniform("in_time", (float) playTime);

                float distort = progress * 75f * 0.05f;
                shader.setFloatUniform("in_distort_radial", distort);
                shader.setFloatUniform("in_distort_xy", 75f * 0.05f);

                // Fade curves from SystemUI
                float fadeSparkle = getFade(0.1f, 0.4f, 1.0f, progress);
                float fadeRing = getFade(0.1f, 0.3f, 1.0f, progress);
                float fadeFill = getFade(0.0f, 0.0f, 0.6f, progress);

                shader.setFloatUniform("in_fadeSparkle", fadeSparkle);
                shader.setFloatUniform("in_fadeRing", fadeRing);
                shader.setFloatUniform("in_fadeFill", fadeFill);
            });
        }

        private static float getFade(float fadeInEnd, float fadeOutStart, float fadeOutEnd, float progress) {
            float in = fadeInEnd > 0f ? Math.min(1f, progress / fadeInEnd) : 1f;
            float out = progress > fadeOutStart ? Math.max(0f, 1f - (progress - fadeOutStart) / (fadeOutEnd - fadeOutStart)) : 1f;
            return Math.min(in, out);
        }
    }
}
