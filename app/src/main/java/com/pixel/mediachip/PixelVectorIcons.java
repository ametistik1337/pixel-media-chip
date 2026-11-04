package com.pixel.mediachip;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.content.Context;

public class PixelVectorIcons {

    public static Drawable getPlayIcon(Context context, int color, int sizePx) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);

        Path path = new Path();
        float p = sizePx * 0.22f;
        float h = sizePx;
        float w = sizePx;
        // Triangle Play
        path.moveTo(w * 0.32f, h * 0.22f);
        path.lineTo(w * 0.78f, h * 0.50f);
        path.lineTo(w * 0.32f, h * 0.78f);
        path.close();

        canvas.drawPath(path, paint);
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Drawable getPauseIcon(Context context, int color, int sizePx) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);

        float barW = sizePx * 0.16f;
        float barH = sizePx * 0.54f;
        float top = sizePx * 0.23f;
        float r = barW * 0.35f;

        // Left Bar
        canvas.drawRoundRect(sizePx * 0.30f, top, sizePx * 0.30f + barW, top + barH, r, r, paint);
        // Right Bar
        canvas.drawRoundRect(sizePx * 0.54f, top, sizePx * 0.54f + barW, top + barH, r, r, paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Drawable getPrevIcon(Context context, int color, int sizePx) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);

        float w = sizePx;
        float h = sizePx;

        // Bar on left
        float barW = w * 0.09f;
        canvas.drawRoundRect(w * 0.22f, h * 0.26f, w * 0.22f + barW, h * 0.74f, 2, 2, paint);

        // Triangle pointing left
        Path path = new Path();
        path.moveTo(w * 0.76f, h * 0.26f);
        path.lineTo(w * 0.36f, h * 0.50f);
        path.lineTo(w * 0.76f, h * 0.74f);
        path.close();
        canvas.drawPath(path, paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Drawable getNextIcon(Context context, int color, int sizePx) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);

        float w = sizePx;
        float h = sizePx;

        // Bar on right
        float barW = w * 0.09f;
        canvas.drawRoundRect(w * 0.78f - barW, h * 0.26f, w * 0.78f, h * 0.74f, 2, 2, paint);

        // Triangle pointing right
        Path path = new Path();
        path.moveTo(w * 0.24f, h * 0.26f);
        path.lineTo(w * 0.64f, h * 0.50f);
        path.lineTo(w * 0.24f, h * 0.74f);
        path.close();
        canvas.drawPath(path, paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Drawable getShuffleIcon(Context context, int color, int sizePx) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(sizePx * 0.09f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        float w = sizePx;
        float h = sizePx;

        // Top line crossing to bottom
        Path p1 = new Path();
        p1.moveTo(w * 0.22f, h * 0.32f);
        p1.cubicTo(w * 0.45f, h * 0.32f, w * 0.55f, h * 0.68f, w * 0.78f, h * 0.68f);
        canvas.drawPath(p1, paint);

        // Arrow at bottom right
        Path arrow1 = new Path();
        arrow1.moveTo(w * 0.68f, h * 0.58f);
        arrow1.lineTo(w * 0.78f, h * 0.68f);
        arrow1.lineTo(w * 0.68f, h * 0.78f);
        canvas.drawPath(arrow1, paint);

        // Bottom line crossing to top
        Path p2 = new Path();
        p2.moveTo(w * 0.22f, h * 0.68f);
        p2.cubicTo(w * 0.42f, h * 0.68f, w * 0.48f, h * 0.55f, w * 0.52f, h * 0.50f);
        canvas.drawPath(p2, paint);

        Path p3 = new Path();
        p3.moveTo(w * 0.58f, h * 0.42f);
        p3.cubicTo(w * 0.64f, h * 0.35f, w * 0.70f, h * 0.32f, w * 0.78f, h * 0.32f);
        canvas.drawPath(p3, paint);

        // Arrow at top right
        Path arrow2 = new Path();
        arrow2.moveTo(w * 0.68f, h * 0.22f);
        arrow2.lineTo(w * 0.78f, h * 0.32f);
        arrow2.lineTo(w * 0.68f, h * 0.42f);
        canvas.drawPath(arrow2, paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Drawable getCheckCircleIcon(Context context, int color, int sizePx) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);

        float w = sizePx;
        float h = sizePx;
        float cx = w / 2f;
        float cy = h / 2f;
        float r = w * 0.38f;

        // Circle outline
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(sizePx * 0.08f);
        canvas.drawCircle(cx, cy, r, paint);

        // Checkmark
        Path check = new Path();
        check.moveTo(w * 0.34f, h * 0.50f);
        check.lineTo(w * 0.46f, h * 0.62f);
        check.lineTo(w * 0.68f, h * 0.40f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        canvas.drawPath(check, paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Drawable getPhoneIcon(Context context, int color, int sizePx) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(sizePx * 0.08f);

        float w = sizePx;
        float h = sizePx;
        float left = w * 0.28f;
        float top = h * 0.16f;
        float right = w * 0.72f;
        float bottom = h * 0.84f;
        float cr = sizePx * 0.10f;

        canvas.drawRoundRect(left, top, right, bottom, cr, cr, paint);

        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(w / 2f, h * 0.74f, sizePx * 0.05f, paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Drawable getArtistIcon(Context context, int color, int sizePx) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);

        float w = sizePx;
        float h = sizePx;
        // Head
        canvas.drawCircle(w * 0.50f, h * 0.32f, w * 0.18f, paint);
        // Shoulders
        RectF body = new RectF(w * 0.20f, h * 0.56f, w * 0.80f, h * 0.94f);
        canvas.drawRoundRect(body, w * 0.22f, w * 0.22f, paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Drawable getMarqueeIcon(Context context, int color, int sizePx) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(sizePx * 0.08f);
        paint.setStrokeCap(Paint.Cap.ROUND);

        float w = sizePx;
        float h = sizePx;
        // Top text line
        canvas.drawLine(w * 0.20f, h * 0.32f, w * 0.80f, h * 0.32f, paint);
        // Middle moving line with arrow
        canvas.drawLine(w * 0.20f, h * 0.50f, w * 0.68f, h * 0.50f, paint);
        Path arrow = new Path();
        arrow.moveTo(w * 0.60f, h * 0.42f);
        arrow.lineTo(w * 0.70f, h * 0.50f);
        arrow.lineTo(w * 0.60f, h * 0.58f);
        canvas.drawPath(arrow, paint);
        // Bottom text line
        canvas.drawLine(w * 0.20f, h * 0.68f, w * 0.55f, h * 0.68f, paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Drawable getPaletteIcon(Context context, int color, int sizePx) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(sizePx * 0.08f);
        paint.setStrokeCap(Paint.Cap.ROUND);

        float w = sizePx;
        float h = sizePx;
        // Palette outline
        RectF r = new RectF(w * 0.18f, h * 0.18f, w * 0.82f, h * 0.82f);
        canvas.drawRoundRect(r, w * 0.25f, w * 0.25f, paint);

        // Color spots
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(w * 0.35f, h * 0.38f, w * 0.07f, paint);
        canvas.drawCircle(w * 0.60f, h * 0.38f, w * 0.07f, paint);
        canvas.drawCircle(w * 0.45f, h * 0.62f, w * 0.07f, paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Drawable getSwipeIcon(Context context, int color, int sizePx) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(sizePx * 0.08f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        float w = sizePx;
        float h = sizePx;

        // Top arrow right
        canvas.drawLine(w * 0.22f, h * 0.38f, w * 0.74f, h * 0.38f, paint);
        Path rightHead = new Path();
        rightHead.moveTo(w * 0.62f, h * 0.28f);
        rightHead.lineTo(w * 0.74f, h * 0.38f);
        rightHead.lineTo(w * 0.62f, h * 0.48f);
        canvas.drawPath(rightHead, paint);

        // Bottom arrow left
        canvas.drawLine(w * 0.26f, h * 0.62f, w * 0.78f, h * 0.62f, paint);
        Path leftHead = new Path();
        leftHead.moveTo(w * 0.38f, h * 0.52f);
        leftHead.lineTo(w * 0.26f, h * 0.62f);
        leftHead.lineTo(w * 0.38f, h * 0.72f);
        canvas.drawPath(leftHead, paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Drawable getHapticIcon(Context context, int color, int sizePx) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);

        float w = sizePx;
        float h = sizePx;
        // Central phone pill
        RectF phone = new RectF(w * 0.38f, h * 0.24f, w * 0.62f, h * 0.76f);
        canvas.drawRoundRect(phone, w * 0.07f, w * 0.07f, paint);

        // Left vibration waves
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(sizePx * 0.08f);
        paint.setStrokeCap(Paint.Cap.ROUND);

        Path leftWave = new Path();
        leftWave.moveTo(w * 0.22f, h * 0.34f);
        leftWave.quadTo(w * 0.14f, h * 0.50f, w * 0.22f, h * 0.66f);
        canvas.drawPath(leftWave, paint);

        // Right vibration waves
        Path rightWave = new Path();
        rightWave.moveTo(w * 0.78f, h * 0.34f);
        rightWave.quadTo(w * 0.86f, h * 0.50f, w * 0.78f, h * 0.66f);
        canvas.drawPath(rightWave, paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Drawable getCheckmarkIcon(Context context, int color, int sizePx) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(sizePx * 0.12f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        float w = sizePx;
        float h = sizePx;
        Path check = new Path();
        check.moveTo(w * 0.26f, h * 0.52f);
        check.lineTo(w * 0.44f, h * 0.70f);
        check.lineTo(w * 0.76f, h * 0.32f);
        canvas.drawPath(check, paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Drawable getHeartIcon(Context context, int color, int sizePx, boolean filled) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(filled ? Paint.Style.FILL : Paint.Style.STROKE);
        if (!filled) {
            paint.setStrokeWidth(sizePx * 0.09f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }

        float w = sizePx;
        float h = sizePx;
        Path heart = new Path();
        heart.moveTo(w * 0.5f, h * 0.78f);
        heart.cubicTo(w * 0.15f, h * 0.54f, w * 0.14f, h * 0.26f, w * 0.32f, h * 0.26f);
        heart.cubicTo(w * 0.43f, h * 0.26f, w * 0.48f, h * 0.36f, w * 0.5f, h * 0.41f);
        heart.cubicTo(w * 0.52f, h * 0.36f, w * 0.57f, h * 0.26f, w * 0.68f, h * 0.26f);
        heart.cubicTo(w * 0.86f, h * 0.26f, w * 0.85f, h * 0.54f, w * 0.5f, h * 0.78f);
        heart.close();

        canvas.drawPath(heart, paint);
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Drawable getCameraIcon(Context context, int color, int sizePx) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(sizePx * 0.08f);
        paint.setStrokeCap(Paint.Cap.ROUND);

        float w = sizePx;
        float h = sizePx;
        // Camera body
        canvas.drawRoundRect(w * 0.18f, h * 0.28f, w * 0.82f, h * 0.76f, w * 0.10f, w * 0.10f, paint);
        // Lens
        canvas.drawCircle(w * 0.50f, h * 0.52f, w * 0.15f, paint);
        // Flash bump
        canvas.drawRect(w * 0.36f, h * 0.20f, w * 0.48f, h * 0.28f, paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Drawable getEqualizerIcon(Context context, int color, int sizePx) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);

        float barW = sizePx * 0.14f;
        float r = barW * 0.45f;
        float bottom = sizePx * 0.78f;

        // Bar 1 (left)
        canvas.drawRoundRect(sizePx * 0.20f, sizePx * 0.42f, sizePx * 0.20f + barW, bottom, r, r, paint);
        // Bar 2 (center)
        canvas.drawRoundRect(sizePx * 0.43f, sizePx * 0.22f, sizePx * 0.43f + barW, bottom, r, r, paint);
        // Bar 3 (right)
        canvas.drawRoundRect(sizePx * 0.66f, sizePx * 0.34f, sizePx * 0.66f + barW, bottom, r, r, paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Drawable getPresetIcon(Context context, int color, int sizePx) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(sizePx * 0.08f);
        paint.setStrokeCap(Paint.Cap.ROUND);

        float w = sizePx;
        float h = sizePx;

        // Magic Wand shaft
        canvas.drawLine(w * 0.28f, h * 0.72f, w * 0.68f, h * 0.32f, paint);

        // Wand tip
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(w * 0.68f, h * 0.32f, sizePx * 0.07f, paint);

        // Stars / sparkles around wand
        canvas.drawCircle(w * 0.34f, h * 0.32f, sizePx * 0.04f, paint);
        canvas.drawCircle(w * 0.72f, h * 0.68f, sizePx * 0.04f, paint);
        canvas.drawCircle(w * 0.50f, h * 0.18f, sizePx * 0.05f, paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Drawable getRestartIcon(Context context, int color, int sizePx) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(sizePx * 0.09f);
        paint.setStrokeCap(Paint.Cap.ROUND);

        float w = sizePx;
        float h = sizePx;
        RectF oval = new RectF(w * 0.22f, h * 0.22f, w * 0.78f, h * 0.78f);
        canvas.drawArc(oval, -60, 280, false, paint);

        // Arrow head
        Path arrow = new Path();
        arrow.moveTo(w * 0.64f, h * 0.16f);
        arrow.lineTo(w * 0.80f, h * 0.24f);
        arrow.lineTo(w * 0.68f, h * 0.38f);
        canvas.drawPath(arrow, paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Drawable getSettingsIcon(Context context, int color, int sizePx) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(sizePx * 0.08f);
        paint.setStrokeCap(Paint.Cap.ROUND);

        float w = sizePx;
        float h = sizePx;
        // Sliders/Controls
        canvas.drawLine(w * 0.22f, h * 0.34f, w * 0.78f, h * 0.34f, paint);
        canvas.drawLine(w * 0.22f, h * 0.66f, w * 0.78f, h * 0.66f, paint);

        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(w * 0.40f, h * 0.34f, sizePx * 0.09f, paint);
        canvas.drawCircle(w * 0.62f, h * 0.66f, sizePx * 0.09f, paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Drawable getRadiusIcon(Context context, int color, int sizePx) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(sizePx * 0.08f);
        paint.setStrokeCap(Paint.Cap.ROUND);

        float w = sizePx;
        float h = sizePx;
        RectF r = new RectF(w * 0.22f, h * 0.22f, w * 0.78f, h * 0.78f);
        canvas.drawRoundRect(r, w * 0.20f, w * 0.20f, paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Drawable getFontIcon(Context context, int color, int sizePx) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(sizePx * 0.52f);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);

        float cx = sizePx * 0.50f;
        float cy = sizePx * 0.68f;
        canvas.drawText("Aa", cx, cy, paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Drawable getResetIcon(Context context, int color, int sizePx) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(sizePx * 0.08f);
        paint.setStrokeCap(Paint.Cap.ROUND);

        float w = sizePx;
        float h = sizePx;
        RectF oval = new RectF(w * 0.22f, h * 0.22f, w * 0.78f, h * 0.78f);
        canvas.drawArc(oval, 30, -260, false, paint);

        Path arrow = new Path();
        arrow.moveTo(w * 0.40f, h * 0.18f);
        arrow.lineTo(w * 0.22f, h * 0.24f);
        arrow.lineTo(w * 0.32f, h * 0.40f);
        canvas.drawPath(arrow, paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }
}
