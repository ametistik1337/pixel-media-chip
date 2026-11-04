package com.pixel.mediachip;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

public class VanillaMediaPopup {
    private static final String TAG = "PixelMediaChip_Popup";

    private final Context mContext;
    private final WindowManager mWindowManager;
    private final MediaSessionController mSessionController;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    // Liquid bridge drawing
    private static final float PINCH_THRESHOLD = 0.38f;
    private static final float RECOIL_DURATION = 0.14f;
    private final Paint mBridgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mBridgePath = new Path();
    private final Path mChipClipPath = new Path();
    private final RectF mChipRect = new RectF();
    private final RectF mTargetCardRect = new RectF();
    private final RectF mCurCardVisualRect = new RectF();
    private final Paint mScrimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mDropletRect = new RectF();
    private float mCurrentCardRadius = 0f;
    private boolean mHasSnapped = false;
    private float mBridgeProgress = 0f;
    private ValueAnimator mExpansionAnimator;
    private static final TimeInterpolator LIQUID_CURVE = M3Motion.DROPLET_PULL_ENTER;
    private static final TimeInterpolator COLLAPSE_CURVE = M3Motion.DROPLET_COALESCE_EXIT;

    private FrameLayout mRootOverlay;
    private FrameLayout mPlayerCardContainer;
    private ConstraintLayout mPlayerCard;

    private ImageView mIvAlbumArt;
    private View mArtDimOverlay;
    private ImageView mIvAppIcon;
    private TextView mTvTitle;
    private TextView mTvExplicit;
    private TextView mTvArtist;
    private View mArtistRow;
    private SeekBar mSeekBar;
    private ImageButton mBtnPrev;
    private ImageButton mBtnPlayPause;
    private ImageButton mBtnNext;
    private ImageButton mBtnShuffle;
    private ImageButton mBtnLike;
    private ImageView mIvSpeaker;
    private TextView mTvSeamlessText;
    private LinearLayout mSeamlessButton;

    private boolean mIsShowing = false;
    private long mOpenedTimestamp = 0;
    private MediaSessionController.MediaState mCurrentState;
    private View mSourceChipView;

    private SquigglyProgress mSquigglyProgress;
    private SpectrumVisualizerView mSpectrumVisualizer;
    private GradientDrawable mCardBackgroundDrawable;
    private ValueAnimator mColorTransitionAnimator;
    private int mCurrentMonetColor = 0xFF1F2024;
    private String mLastTrackKey = "";
    private Bitmap mLastAlbumBitmap = null;
    private Boolean mLastIsPlaying = null;
    private Drawable mCurrentPlayPauseIconDrawable = null;
    private Drawable mCurrentPlayPauseContainerDrawable = null;
    private boolean mIsPlayPauseAnimating = false;

    private float mInitialScaleX = 1f;
    private float mInitialScaleY = 1f;
    private float mInitialTransX = 0f;
    private float mInitialTransY = 0f;

    public VanillaMediaPopup(Context context, MediaSessionController sessionController) {
        this.mContext = context;
        this.mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.mSessionController = sessionController;

        mBridgePaint.setStyle(Paint.Style.FILL);
        mBridgePaint.setColor(Color.parseColor("#FF1A1C1E"));

        buildOverlay();
    }

    private void buildOverlay() {
        mRootOverlay = new FrameLayout(mContext) {
            @Override
            protected void dispatchDraw(Canvas canvas) {
                // 1. Draw seamless fullscreen dimming across status bar and display
                drawSeamlessScrim(canvas);

                // 2. Draw organic liquid droplet metamorphosis & bridge
                if (mBridgeProgress > 0.005f && mBridgeProgress < 1.0f) {
                    drawDropletAnimation(canvas);
                }
                super.dispatchDraw(canvas);
            }

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (System.currentTimeMillis() - mOpenedTimestamp < 120) return true;
                if (event.getAction() == MotionEvent.ACTION_DOWN && mPlayerCardContainer != null && mIsShowing) {
                    int[] loc = new int[2];
                    mPlayerCardContainer.getLocationOnScreen(loc);
                    float curW = mPlayerCardContainer.getWidth() * mPlayerCardContainer.getScaleX();
                    float curH = mPlayerCardContainer.getHeight() * mPlayerCardContainer.getScaleY();
                    RectF rect = new RectF(loc[0], loc[1], loc[0] + curW, loc[1] + curH);
                    if (!rect.contains(event.getRawX(), event.getRawY())) {
                        dismiss();
                        return true;
                    }
                }
                return super.onTouchEvent(event);
            }
        };
        mRootOverlay.setWillNotDraw(false);

        int cardHeight = convertDpToPx(195);
        int cardMargin = convertDpToPx(16);
        int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        int targetCardWidth = screenWidth - cardMargin * 2;
        int targetTop = convertDpToPx(52);

        mTargetCardRect.set(cardMargin, targetTop, screenWidth - cardMargin, targetTop + cardHeight);

        mPlayerCardContainer = new FrameLayout(mContext);
        mPlayerCardContainer.setClipToOutline(true);
        GradientDrawable containerBg = new GradientDrawable();
        containerBg.setCornerRadius(convertDpToPx(28));
        containerBg.setColor(Color.parseColor("#FF1A1C1E"));
        mCardBackgroundDrawable = containerBg;
        mPlayerCardContainer.setBackground(containerBg);

        mPlayerCard = buildCardLayout();
        mPlayerCardContainer.addView(mPlayerCard, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(targetCardWidth, cardHeight);
        cardLp.gravity = Gravity.TOP | Gravity.START;
        cardLp.topMargin = targetTop;
        cardLp.leftMargin = cardMargin;
        mPlayerCardContainer.setLayoutParams(cardLp);
        mPlayerCardContainer.setOnTouchListener((v, event) -> true);
        mRootOverlay.addView(mPlayerCardContainer);
    }

    private void drawSeamlessScrim(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        float alphaFactor = Math.min(1.0f, mBridgeProgress / 0.75f);
        int baseAlpha = (int) (0x5A * alphaFactor);
        if (baseAlpha <= 0) return;

        mScrimPaint.setShader(null);
        mScrimPaint.setColor((baseAlpha << 24));
        canvas.drawRect(0, 0, w, h, mScrimPaint);

        int topFadeHeight = mTargetCardRect.top > 0 ? (int) (mTargetCardRect.top + convertDpToPx(36)) : convertDpToPx(88);
        int topAlpha1 = (int) (0x48 * alphaFactor);
        int topAlpha2 = (int) (0x18 * alphaFactor);
        LinearGradient topGradient = new LinearGradient(
                0, 0, 0, topFadeHeight,
                new int[]{(topAlpha1 << 24), (topAlpha2 << 24), Color.TRANSPARENT},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP
        );
        mScrimPaint.setShader(topGradient);
        canvas.drawRect(0, 0, w, topFadeHeight, mScrimPaint);
        mScrimPaint.setShader(null);
    }

    private boolean mIsExpanding = true;
    private boolean mIsDismissing = false;

    private void computeDropletGeometry(float p) {
        float density = mContext.getResources().getDisplayMetrics().density;
        float chipCX = mChipRect.centerX();
        float chipTop = mChipRect.top;
        float chipBot = mChipRect.bottom;
        float chipW = mChipRect.width();
        float chipH = mChipRect.height();
        float chipRadius = chipH > 0 ? chipH / 2f : 14f * density;

        float targetCX = mTargetCardRect.centerX();
        float targetTop = mTargetCardRect.top;
        float targetW = mTargetCardRect.width();
        float targetH = mTargetCardRect.height();
        float finalCardRadius = convertDpToPx(28);

        if (p <= 0f) {
            // At p=0, droplet is EXACTLY inside the mini-chip itself
            mDropletRect.set(mChipRect);
            mCurrentCardRadius = chipRadius;
            return;
        }

        if (p >= 1f) {
            mDropletRect.set(mTargetCardRect);
            mCurrentCardRadius = finalCardRadius;
            return;
        }

        if (p < PINCH_THRESHOLD) {
            // Regime 1: Viscous stretch & necking from within the status bar chip
            float norm = p / PINCH_THRESHOLD; // 0..1
            float fallFrac = (float) Math.pow(norm, 1.25);

            float detachedW = Math.max(chipW * 0.9f, 84f * density);
            float detachedH = 52f * density;
            float detachedTop = targetTop - 6f * density;
            float detachedCX = chipCX + (targetCX - chipCX) * 0.20f;

            // Geometry transitions smoothly between mChipRect (norm=0) and detached droplet (norm=1)
            float curCX = chipCX + (detachedCX - chipCX) * norm;
            float curW = chipW + (detachedW - chipW) * norm;
            float curH = chipH + (detachedH - chipH) * norm;

            // curTop moves from chipTop directly into detached position (and returns into chipTop on close)
            float curTop = chipTop + (detachedTop - chipTop) * fallFrac;
            float curBot = curTop + curH;

            mDropletRect.set(curCX - curW / 2f, curTop, curCX + curW / 2f, curBot);
            mCurrentCardRadius = chipRadius + (detachedH / 2f - chipRadius) * norm;
        } else {
            // Regime 2: Detachment, bloom & elastic settle
            float bloom = (p - PINCH_THRESHOLD) / (1.0f - PINCH_THRESHOLD); // 0..1
            float bloomEase = (float) Math.sin(bloom * Math.PI * 0.5);

            float detachedCX = chipCX + (targetCX - chipCX) * 0.20f;
            float curCX = detachedCX + (targetCX - detachedCX) * (float) Math.pow(bloom, 1.2);

            float initialW = 84f * density;
            float initialH = 52f * density;
            float detachedTop = targetTop - 6f * density;

            float curW = initialW + (targetW - initialW) * (float) Math.pow(bloom, 1.35);
            float curH = initialH + (targetH - initialH) * (float) Math.pow(bloom, 0.95);
            float curTop = detachedTop + (targetTop - detachedTop) * bloomEase;

            // Elastic bounce on bounds when coalescing into rest position (p >= 0.75)
            float bounce = M3Motion.calculateDropletBounce(p);
            float bW = curW * bounce;
            float bH = curH * bounce;

            mDropletRect.set(curCX - bW / 2f, curTop, curCX + bW / 2f, curTop + bH);
            // Radius morphs from droplet bulb (pill) to concentric card radius (28dp)
            mCurrentCardRadius = (52f * density / 2f) + (finalCardRadius - (52f * density / 2f)) * bloomEase;
        }
    }

    private void updateCardMorph(float p) {
        if (mPlayerCardContainer == null) return;

        computeDropletGeometry(p);

        if (p < PINCH_THRESHOLD) {
            mPlayerCardContainer.setVisibility(View.INVISIBLE);
            mPlayerCardContainer.setAlpha(0f);
            mHasSnapped = false;
            return;
        }

        if (!mHasSnapped && mIsExpanding) {
            mHasSnapped = true;
            animateContentIn();
        }

        mPlayerCardContainer.setVisibility(View.VISIBLE);

        float targetW = mTargetCardRect.width();
        float targetH = mTargetCardRect.height();
        float curW = mDropletRect.width();
        float curH = mDropletRect.height();

        float scaleX = Math.max(0.20f, curW / targetW);
        float scaleY = Math.max(0.20f, curH / targetH);

        // Position card container so its visual bounds match mDropletRect exactly
        float curLeft = mDropletRect.left;
        float curTop = mDropletRect.top;

        mPlayerCardContainer.setPivotX(0);
        mPlayerCardContainer.setPivotY(0);
        mPlayerCardContainer.setTranslationX(curLeft - mTargetCardRect.left);
        mPlayerCardContainer.setTranslationY(curTop - mTargetCardRect.top);
        mPlayerCardContainer.setScaleX(scaleX);
        mPlayerCardContainer.setScaleY(scaleY);

        // Alpha ramp: begins at snap and reaches 1.0 at p=0.65
        float alpha = Math.min(1.0f, (p - PINCH_THRESHOLD) / 0.28f);
        mPlayerCardContainer.setAlpha(alpha);

        if (mCardBackgroundDrawable != null) {
            // Concentric corner radius compensated for scale
            int unscaledRadius = (int) Math.min(targetH / 2f, mCurrentCardRadius / Math.max(0.2f, scaleX));
            mCardBackgroundDrawable.setCornerRadius(unscaledRadius);
        }
        mPlayerCardContainer.invalidateOutline();
    }

    private void drawDropletAnimation(Canvas canvas) {
        float p = mBridgeProgress;
        if (p <= 0.005f || p >= 0.75f) return;

        float density = mContext.getResources().getDisplayMetrics().density;
        float chipCX = mChipRect.centerX();
        float chipBot = mChipRect.bottom;
        int cardBgColor = adjustMonetColorForCard(mCurrentMonetColor);
        mBridgePaint.setColor(cardBgColor);

        canvas.save();

        // Position droplet strictly on a lower Z-layer than the mini-chip:
        // Clip out the mini-chip's exact pill shape so the mini-chip remains in front!
        mChipClipPath.reset();
        int chipRadius = convertDpToPx(ChipSettings.get().cornerRadiusDp > 0 ? ChipSettings.get().cornerRadiusDp : 14);
        mChipClipPath.addRoundRect(mChipRect, chipRadius, chipRadius, Path.Direction.CW);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            canvas.clipOutPath(mChipClipPath);
        } else {
            canvas.clipPath(mChipClipPath, android.graphics.Region.Op.DIFFERENCE);
        }

        mBridgePath.reset();

        if (p < PINCH_THRESHOLD) {
            float curDropTop = mDropletRect.top;
            float gap = curDropTop - chipBot;

            // Draw organic liquid meniscus bridge only while droplet is physically below the chip
            if (gap > 2f * density) {
                float norm = p / PINCH_THRESHOLD;
                float initialWaist = Math.min(mChipRect.width() * 0.75f, 48f * density);
                float minWaist = 4f * density;
                float waistW = Math.max(minWaist, initialWaist * (float) Math.pow(1.0f - norm, 1.4));

                float curCX = mDropletRect.centerX();
                float curDropW = mDropletRect.width();

                float waistY = chipBot + gap * 0.42f;
                float waistCX = chipCX + (curCX - chipCX) * 0.35f;

                float chipAttachW = Math.max(waistW * 1.3f, mChipRect.width() * 0.70f * (1.0f - norm * 0.5f));
                float dropAttachW = Math.max(waistW * 1.5f, curDropW * (1.0f - norm * 0.4f));

                float chipL = chipCX - chipAttachW / 2f;
                float chipR = chipCX + chipAttachW / 2f;
                float dropL = curCX - dropAttachW / 2f;
                float dropR = curCX + dropAttachW / 2f;

                mBridgePath.moveTo(chipL, chipBot);
                // Left Bézier
                mBridgePath.cubicTo(
                        chipL, chipBot + gap * 0.22f,
                        waistCX - waistW / 2f, waistY - gap * 0.18f,
                        waistCX - waistW / 2f, waistY
                );
                mBridgePath.cubicTo(
                        waistCX - waistW / 2f, waistY + gap * 0.18f,
                        dropL, curDropTop - gap * 0.15f,
                        dropL, curDropTop + 2f * density
                );
                mBridgePath.lineTo(dropR, curDropTop + 2f * density);
                // Right Bézier
                mBridgePath.cubicTo(
                        dropR, curDropTop - gap * 0.15f,
                        waistCX + waistW / 2f, waistY + gap * 0.18f,
                        waistCX + waistW / 2f, waistY
                );
                mBridgePath.cubicTo(
                        waistCX + waistW / 2f, waistY - gap * 0.18f,
                        chipR, chipBot + gap * 0.22f,
                        chipR, chipBot
                );
                mBridgePath.close();
            }

            // 2. Add droplet body (morphs directly into mChipRect at p=0)
            mBridgePath.addRoundRect(mDropletRect, mCurrentCardRadius, mCurrentCardRadius, Path.Direction.CW);
            canvas.drawPath(mBridgePath, mBridgePaint);

        } else if (p < PINCH_THRESHOLD + RECOIL_DURATION) {
            // Capillary snap recoil at the chip only on expansion/detachment
            if (mIsExpanding) {
                float recoilFrac = (p - PINCH_THRESHOLD) / RECOIL_DURATION;
                float recoil = (1.0f - recoilFrac) * (float) Math.cos(recoilFrac * Math.PI * 0.5);
                if (recoil > 0.02f) {
                    float recoilH = 7f * density * recoil;
                    float recoilW = 18f * density * recoil;
                    mBridgePath.moveTo(chipCX - recoilW / 2f, chipBot);
                    mBridgePath.cubicTo(
                            chipCX - recoilW / 2f, chipBot + recoilH * 0.85f,
                            chipCX + recoilW / 2f, chipBot + recoilH * 0.85f,
                            chipCX + recoilW / 2f, chipBot
                    );
                    mBridgePath.close();
                }
            }
            // Add blooming droplet body until container fully takes over
            mBridgePath.addRoundRect(mDropletRect, mCurrentCardRadius, mCurrentCardRadius, Path.Direction.CW);
            canvas.drawPath(mBridgePath, mBridgePaint);

        } else if (p < 0.70f) {
            // Draw morphing droplet body during bloom handoff
            mBridgePath.addRoundRect(mDropletRect, mCurrentCardRadius, mCurrentCardRadius, Path.Direction.CW);
            canvas.drawPath(mBridgePath, mBridgePaint);
        }

        canvas.restore();
    }

    private ConstraintLayout buildCardLayout() {
        ConstraintLayout layout = new ConstraintLayout(mContext);
        layout.setId(View.generateViewId());

        // 1. Album Art
        mIvAlbumArt = new ImageView(mContext);
        mIvAlbumArt.setId(View.generateViewId());
        mIvAlbumArt.setScaleType(ImageView.ScaleType.CENTER_CROP);
        layout.addView(mIvAlbumArt);

        // Dark gradient overlay
        mArtDimOverlay = new View(mContext);
        mArtDimOverlay.setId(View.generateViewId());
        GradientDrawable grad = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.parseColor("#DD101418"), Color.parseColor("#78101418")}
        );
        mArtDimOverlay.setBackground(grad);
        layout.addView(mArtDimOverlay);

        // 2. App Icon
        mIvAppIcon = new ImageView(mContext);
        mIvAppIcon.setId(View.generateViewId());
        mIvAppIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        layout.addView(mIvAppIcon);

        // 3. Audio Output Chip
        mSeamlessButton = new LinearLayout(mContext);
        mSeamlessButton.setId(View.generateViewId());
        mSeamlessButton.setOrientation(LinearLayout.HORIZONTAL);
        mSeamlessButton.setGravity(Gravity.CENTER_VERTICAL);
        mSeamlessButton.setPadding(convertDpToPx(12), convertDpToPx(6), convertDpToPx(14), convertDpToPx(6));

        mIvSpeaker = new ImageView(mContext);
        mIvSpeaker.setImageDrawable(PixelVectorIcons.getPhoneIcon(mContext, Color.WHITE, convertDpToPx(16)));
        mSeamlessButton.addView(mIvSpeaker, new LinearLayout.LayoutParams(convertDpToPx(16), convertDpToPx(16)));

        mTvSeamlessText = new TextView(mContext);
        mTvSeamlessText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        mTvSeamlessText.setTextColor(Color.WHITE);
        mTvSeamlessText.setTypeface(Typeface.DEFAULT_BOLD);
        mTvSeamlessText.setSingleLine(true);
        LinearLayout.LayoutParams seamTxtLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        seamTxtLp.leftMargin = convertDpToPx(6);
        mSeamlessButton.addView(mTvSeamlessText, seamTxtLp);
        layout.addView(mSeamlessButton);

        // 4. Header Title
        mTvTitle = new TextView(mContext);
        mTvTitle.setId(View.generateViewId());
        mTvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        mTvTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        mTvTitle.setTextColor(Color.WHITE);
        mTvTitle.setSingleLine(true);
        mTvTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        mTvTitle.setMarqueeRepeatLimit(-1);
        mTvTitle.setSelected(true);
        layout.addView(mTvTitle);

        // 5. Artist Row
        LinearLayout artistRow = new LinearLayout(mContext);
        artistRow.setId(View.generateViewId());
        artistRow.setOrientation(LinearLayout.HORIZONTAL);
        artistRow.setGravity(Gravity.CENTER_VERTICAL);
        mArtistRow = artistRow;

        mTvExplicit = new TextView(mContext);
        mTvExplicit.setText("E");
        mTvExplicit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        mTvExplicit.setTypeface(Typeface.DEFAULT_BOLD);
        mTvExplicit.setTextColor(Color.WHITE);
        mTvExplicit.setPadding(convertDpToPx(4), convertDpToPx(1), convertDpToPx(4), convertDpToPx(1));
        GradientDrawable expBg = new GradientDrawable();
        expBg.setCornerRadius(convertDpToPx(3));
        expBg.setStroke(convertDpToPx(1), Color.WHITE);
        mTvExplicit.setBackground(expBg);
        LinearLayout.LayoutParams expLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        expLp.rightMargin = convertDpToPx(6);
        artistRow.addView(mTvExplicit, expLp);

        mTvArtist = new TextView(mContext);
        mTvArtist.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        mTvArtist.setTextColor(Color.parseColor("#EEFFFFFF"));
        mTvArtist.setSingleLine(true);
        mTvArtist.setEllipsize(TextUtils.TruncateAt.END);
        artistRow.addView(mTvArtist);
        layout.addView(artistRow);

        // 6. Play/Pause Button - single ImageButton with stock SystemUI resources
        mBtnPlayPause = new ImageButton(mContext);
        mBtnPlayPause.setId(View.generateViewId());
        mBtnPlayPause.setPadding(0, 0, 0, 0);
        mBtnPlayPause.setStateListAnimator(null);
        mBtnPlayPause.setScaleType(ImageView.ScaleType.CENTER);
        layout.addView(mBtnPlayPause);

        // 7. Controls
        mBtnPrev = createControlButton(PixelVectorIcons.getPrevIcon(mContext, Color.WHITE, convertDpToPx(24)));
        layout.addView(mBtnPrev);

        mSeekBar = new SeekBar(mContext);
        mSeekBar.setId(View.generateViewId());
        mSeekBar.setPadding(convertDpToPx(2), convertDpToPx(8), convertDpToPx(2), convertDpToPx(8));
        mSeekBar.setSplitTrack(false);
        mSeekBar.setThumb(null); // SquigglyProgress draws the custom 4dp x 16dp pillar ("столбик") directly
        mSquigglyProgress = new SquigglyProgress(mContext);
        mSeekBar.setProgressDrawable(mSquigglyProgress);
        layout.addView(mSeekBar);

        mBtnNext = createControlButton(PixelVectorIcons.getNextIcon(mContext, Color.WHITE, convertDpToPx(24)));
        layout.addView(mBtnNext);

        mBtnShuffle = createControlButton(PixelVectorIcons.getShuffleIcon(mContext, Color.WHITE, convertDpToPx(24)));
        layout.addView(mBtnShuffle);

        mBtnLike = createControlButton(PixelVectorIcons.getHeartIcon(mContext, Color.WHITE, convertDpToPx(24), false));
        layout.addView(mBtnLike);

        mSpectrumVisualizer = new SpectrumVisualizerView(mContext);
        mSpectrumVisualizer.setId(View.generateViewId());
        layout.addView(mSpectrumVisualizer);

        applyConstraints(layout, mBtnPlayPause, artistRow);
        setupListeners();
        return layout;
    }

    private ImageButton createControlButton(Drawable icon) {
        ImageButton btn = new ImageButton(mContext);
        btn.setId(View.generateViewId());
        btn.setImageDrawable(icon);
        btn.setPadding(0, 0, 0, 0);
        btn.setStateListAnimator(null);
        btn.setScaleType(ImageView.ScaleType.CENTER);

        // Circular ripple
        GradientDrawable mask = new GradientDrawable();
        mask.setShape(GradientDrawable.OVAL);
        mask.setColor(Color.WHITE);
        RippleDrawable ripple = new RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#44FFFFFF")),
                null,
                mask
        );
        btn.setBackground(ripple);
        return btn;
    }

    private void bindPlayPauseButton(boolean isPlaying, int monetColor) {
        try {
            if (mLastIsPlaying != null && mLastIsPlaying == isPlaying && mCurrentMonetColor == monetColor && mBtnPlayPause.getDrawable() != null) {
                return;
            }

            int iconRes = mContext.getResources().getIdentifier(
                    isPlaying ? "ic_media_pause_button" : "ic_media_play_button",
                    "drawable", "com.android.systemui"
            );
            int containerRes = mContext.getResources().getIdentifier(
                    isPlaying ? "ic_media_pause_button_container" : "ic_media_play_button_container",
                    "drawable", "com.android.systemui"
            );

            Drawable iconDrawable = iconRes != 0 ? mContext.getDrawable(iconRes) : null;
            Drawable containerDrawable = containerRes != 0 ? mContext.getDrawable(containerRes) : null;

            mCurrentPlayPauseIconDrawable = iconDrawable;
            mCurrentPlayPauseContainerDrawable = containerDrawable;

            int rippleColor = Color.parseColor("#26FFFFFF");
            if (containerDrawable != null) {
                containerDrawable.mutate().setTint(monetColor);
                Drawable mask = mContext.getDrawable(containerRes);
                RippleDrawable ripple = new RippleDrawable(
                        ColorStateList.valueOf(rippleColor),
                        containerDrawable,
                        mask
                );
                mBtnPlayPause.setBackground(ripple);
            } else {
                GradientDrawable pillBg = new GradientDrawable();
                pillBg.setCornerRadius(convertDpToPx(24));
                pillBg.setColor(monetColor);
                GradientDrawable pillMask = new GradientDrawable();
                pillMask.setCornerRadius(convertDpToPx(24));
                pillMask.setColor(Color.WHITE);
                RippleDrawable ripple = new RippleDrawable(
                        ColorStateList.valueOf(rippleColor),
                        pillBg,
                        pillMask
                );
                mBtnPlayPause.setBackground(ripple);
            }

            if (iconDrawable != null) {
                iconDrawable.mutate().setTint(Color.BLACK);
                mBtnPlayPause.setImageDrawable(iconDrawable);
            } else {
                Drawable targetIcon = isPlaying ?
                        PixelVectorIcons.getPauseIcon(mContext, Color.BLACK, convertDpToPx(26)) :
                        PixelVectorIcons.getPlayIcon(mContext, Color.BLACK, convertDpToPx(26));
                mBtnPlayPause.setImageDrawable(targetIcon);
            }
            mLastIsPlaying = isPlaying;
        } catch (Throwable t) {
            Log.e(TAG, "Error binding play/pause button: ", t);
        }
    }

    private void applyConstraints(ConstraintLayout layout, ImageButton playPauseBtn, LinearLayout artistRow) {
        ConstraintSet set = new ConstraintSet();
        set.clone(layout);
        int pad = convertDpToPx(16);

        // Fullscreen album art + overlay
        for (int id : new int[]{mIvAlbumArt.getId(), mArtDimOverlay.getId()}) {
            set.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            set.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            set.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            set.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
            set.constrainWidth(id, ConstraintSet.MATCH_CONSTRAINT);
            set.constrainHeight(id, ConstraintSet.MATCH_CONSTRAINT);
        }

        int appIconSize = convertDpToPx(32);
        set.constrainWidth(mIvAppIcon.getId(), appIconSize);
        set.constrainHeight(mIvAppIcon.getId(), appIconSize);
        set.connect(mIvAppIcon.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, pad);
        set.connect(mIvAppIcon.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, pad);

        set.connect(mSeamlessButton.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, pad);
        set.connect(mSeamlessButton.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, pad);

        // Play/Pause Pill
        set.constrainWidth(playPauseBtn.getId(), convertDpToPx(72));
        set.constrainHeight(playPauseBtn.getId(), convertDpToPx(48));
        set.connect(playPauseBtn.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, pad);
        set.connect(playPauseBtn.getId(), ConstraintSet.BOTTOM, mSeekBar.getId(), ConstraintSet.TOP, convertDpToPx(8));

        set.connect(mTvTitle.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, pad);
        set.connect(mTvTitle.getId(), ConstraintSet.END, playPauseBtn.getId(), ConstraintSet.START, convertDpToPx(12));
        set.connect(mTvTitle.getId(), ConstraintSet.BOTTOM, artistRow.getId(), ConstraintSet.TOP, convertDpToPx(2));
        set.constrainWidth(mTvTitle.getId(), ConstraintSet.MATCH_CONSTRAINT);

        set.connect(artistRow.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, pad);
        set.connect(artistRow.getId(), ConstraintSet.END, playPauseBtn.getId(), ConstraintSet.START, convertDpToPx(12));
        set.connect(artistRow.getId(), ConstraintSet.BOTTOM, mSeekBar.getId(), ConstraintSet.TOP, convertDpToPx(12));
        set.constrainWidth(artistRow.getId(), ConstraintSet.MATCH_CONSTRAINT);

        int btnSize = convertDpToPx(38);
        boolean visCardEnabled = ChipSettings.get().visualizerCardEnabled;
        int bottomMarginBtns = visCardEnabled ? convertDpToPx(24) : convertDpToPx(12);
        int bottomMarginSeek = visCardEnabled ? convertDpToPx(26) : convertDpToPx(14);

        for (ImageButton btn : new ImageButton[]{mBtnPrev, mBtnNext, mBtnShuffle, mBtnLike}) {
            set.constrainWidth(btn.getId(), btnSize);
            set.constrainHeight(btn.getId(), btnSize);
            set.connect(btn.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, bottomMarginBtns);
        }
        set.connect(mBtnPrev.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, convertDpToPx(10));
        set.connect(mBtnNext.getId(), ConstraintSet.END, mBtnShuffle.getId(), ConstraintSet.START, convertDpToPx(2));
        set.connect(mBtnShuffle.getId(), ConstraintSet.END, mBtnLike.getId(), ConstraintSet.START, convertDpToPx(2));
        set.connect(mBtnLike.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, convertDpToPx(10));

        set.connect(mSeekBar.getId(), ConstraintSet.START, mBtnPrev.getId(), ConstraintSet.END, convertDpToPx(2));
        set.connect(mSeekBar.getId(), ConstraintSet.END, mBtnNext.getId(), ConstraintSet.START, convertDpToPx(2));
        set.connect(mSeekBar.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, bottomMarginSeek);
        set.constrainWidth(mSeekBar.getId(), ConstraintSet.MATCH_CONSTRAINT);

        if (mSpectrumVisualizer != null) {
            set.constrainWidth(mSpectrumVisualizer.getId(), ConstraintSet.MATCH_CONSTRAINT);
            set.constrainHeight(mSpectrumVisualizer.getId(), convertDpToPx(16));
            set.connect(mSpectrumVisualizer.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, convertDpToPx(14));
            set.connect(mSpectrumVisualizer.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, convertDpToPx(14));
            set.connect(mSpectrumVisualizer.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, convertDpToPx(6));
            set.setVisibility(mSpectrumVisualizer.getId(), visCardEnabled ? View.VISIBLE : View.GONE);
        }

        set.applyTo(layout);
    }

    public void updateEqualizerConstraints(boolean isVisualizerEnabled, boolean animate) {
        if (mPlayerCard == null) return;

        if (animate && mIsShowing) {
            android.transition.TransitionManager.beginDelayedTransition(
                    mPlayerCard,
                    new android.transition.ChangeBounds()
                            .setDuration(240)
                            .setInterpolator(M3Motion.EASE_OUT)
            );
        }

        ConstraintSet set = new ConstraintSet();
        set.clone(mPlayerCard);

        int bottomMarginBtns = isVisualizerEnabled ? convertDpToPx(24) : convertDpToPx(12);
        int bottomMarginSeek = isVisualizerEnabled ? convertDpToPx(26) : convertDpToPx(14);

        for (ImageButton btn : new ImageButton[]{mBtnPrev, mBtnNext, mBtnShuffle, mBtnLike}) {
            if (btn != null) {
                set.connect(btn.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, bottomMarginBtns);
            }
        }
        if (mSeekBar != null) {
            set.connect(mSeekBar.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, bottomMarginSeek);
        }

        if (mSpectrumVisualizer != null) {
            if (isVisualizerEnabled) {
                set.setVisibility(mSpectrumVisualizer.getId(), View.VISIBLE);
                set.connect(mSpectrumVisualizer.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, convertDpToPx(6));
            } else {
                set.setVisibility(mSpectrumVisualizer.getId(), View.GONE);
            }
        }

        set.applyTo(mPlayerCard);
    }

    public void notifySettingsChanged() {
        boolean visCardEnabled = ChipSettings.get().visualizerCardEnabled;
        updateEqualizerConstraints(visCardEnabled, mIsShowing);
        if (mSpectrumVisualizer != null) {
            mSpectrumVisualizer.setActive(visCardEnabled && mIsShowing);
        }
        if (mCurrentState != null) {
            updateCardAndAccentColors(mCurrentState.monetColor);
        }
    }

    private void setupListeners() {
        M3Motion.attachTactilePress(mBtnPrev, v -> {
            mSessionController.previous();
        }, () -> ChipSettings.get().hapticEnabled);

        M3Motion.attachTactilePress(mBtnPlayPause, v -> {
            mSessionController.playPause();

            if (!mIsPlayPauseAnimating) {
                if (mCurrentPlayPauseIconDrawable instanceof Animatable) {
                    ((Animatable) mCurrentPlayPauseIconDrawable).start();
                }
                if (mCurrentPlayPauseContainerDrawable instanceof Animatable) {
                    ((Animatable) mCurrentPlayPauseContainerDrawable).start();
                }
                mIsPlayPauseAnimating = true;

                boolean nextIsPlaying = (mLastIsPlaying != null) ? !mLastIsPlaying : (mCurrentState != null && !mCurrentState.isPlaying);
                if (mSquigglyProgress != null) {
                    mSquigglyProgress.setAnimate(nextIsPlaying);
                }

                mBtnPlayPause.postDelayed(() -> {
                    mIsPlayPauseAnimating = false;
                    if (mCurrentState != null) {
                        bindPlayPauseButton(mCurrentState.isPlaying, mCurrentState.monetColor);
                    } else {
                        bindPlayPauseButton(nextIsPlaying, mCurrentMonetColor);
                    }
                }, 350);
            }
        }, () -> ChipSettings.get().hapticEnabled);

        M3Motion.attachTactilePress(mBtnNext, v -> {
            mSessionController.next();
        }, () -> ChipSettings.get().hapticEnabled);

        M3Motion.attachTactilePress(mBtnShuffle, v -> {
            if (mCurrentState != null && mCurrentState.shuffleAction != null) {
                mSessionController.triggerCustomAction(mCurrentState.shuffleAction);
                mCurrentState.isShuffleOn = !mCurrentState.isShuffleOn;
                updateCustomActions(mCurrentState);
            } else {
                mSessionController.triggerCustomAction("SHUFFLE", null);
            }
        }, () -> ChipSettings.get().hapticEnabled);

        M3Motion.attachTactilePress(mBtnLike, v -> {
            if (mCurrentState != null) {
                mCurrentState.isLiked = !mCurrentState.isLiked;
                updateCustomActions(mCurrentState);
                if (mCurrentState.likeAction != null) {
                    mSessionController.triggerCustomAction(mCurrentState.likeAction);
                } else {
                    mSessionController.triggerCustomAction("LIKE", null);
                }
            }
        }, () -> ChipSettings.get().hapticEnabled);

        if (mSeamlessButton != null) {
            M3Motion.attachTactilePress(mSeamlessButton, v -> {
                // Audio output feedback
            }, () -> ChipSettings.get().hapticEnabled);
        }

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mSquigglyProgress != null && seekBar.getMax() > 0) {
                    mSquigglyProgress.setProgressFraction((float) progress / seekBar.getMax());
                }
                if (fromUser) {
                    mSessionController.seekTo(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateCustomActions(MediaSessionController.MediaState state) {
        if (state == null) return;
        // Like Button - always use consistent Pixel Material 3 heart icon
        if (mBtnLike != null) {
            mBtnLike.setImageDrawable(PixelVectorIcons.getHeartIcon(
                    mContext,
                    state.isLiked ? state.monetColor : Color.parseColor("#B3FFFFFF"),
                    convertDpToPx(24),
                    state.isLiked
            ));
            mBtnLike.setImageTintList(null);
        }

        // Shuffle Button
        if (mBtnShuffle != null) {
            if (state.shuffleIcon != null) {
                mBtnShuffle.setImageDrawable(state.shuffleIcon);
                mBtnShuffle.setImageTintList(state.isShuffleOn ? ColorStateList.valueOf(state.monetColor) : ColorStateList.valueOf(Color.parseColor("#B3FFFFFF")));
            } else {
                mBtnShuffle.setImageDrawable(PixelVectorIcons.getShuffleIcon(
                        mContext,
                        state.isShuffleOn ? state.monetColor : Color.parseColor("#B3FFFFFF"),
                        convertDpToPx(24)
                ));
                mBtnShuffle.setImageTintList(null);
            }
        }
    }

    private void updateCardAndAccentColors(int color) {
        int cardBgColor = adjustMonetColorForCard(color);
        if (mCardBackgroundDrawable != null) {
            mCardBackgroundDrawable.setColor(cardBgColor);
        }
        mBridgePaint.setColor(cardBgColor);
        if (mSeamlessButton != null) {
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(convertDpToPx(14));
            bg.setColor(adjustMonetForSeamless(color));
            mSeamlessButton.setBackground(bg);
        }
        if (mSquigglyProgress != null) {
            mSquigglyProgress.setWaveColor(color);
        }
    }

    private int adjustMonetColorForCard(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = Math.min(0.35f, hsv[1] * 0.45f);
        hsv[2] = Math.max(0.12f, Math.min(hsv[2] * 0.35f, 0.22f));
        return Color.HSVToColor(0xFF, hsv);
    }

    public void updateState(MediaSessionController.MediaState state) {
        this.mCurrentState = state;
        if (state == null || mPlayerCard == null) return;
        try {
            String safePkg = (state.packageName != null) ? state.packageName : "";
            String safeTitle = (state.title != null) ? state.title : "";
            String safeArtist = (state.artist != null) ? state.artist : "";
            String safeAlbum = (state.album != null) ? state.album : "";
            String artistDisplay = !safeArtist.isEmpty() ? safeArtist : safeAlbum;

            String newTrackKey = safePkg + ":" + safeTitle + ":" + safeArtist;
            boolean trackChanged = !mLastTrackKey.isEmpty() && !mLastTrackKey.equals(newTrackKey);
            mLastTrackKey = newTrackKey;

            if (trackChanged && mIsShowing) {
                // Pixel SystemUI media_metadata_exit (167ms, translationX -32dp, alpha 1->0)
                int shiftPx = convertDpToPx(32);
                if (mTvTitle != null) {
                    mTvTitle.animate().setListener(null).translationX(-shiftPx).setDuration(167)
                            .setInterpolator(new DecelerateInterpolator()).start();
                    mTvTitle.animate().alpha(0f).setStartDelay(83).setDuration(84).start();
                }
                if (mArtistRow != null) {
                    mArtistRow.animate().setListener(null).translationX(-shiftPx).setDuration(167)
                            .setInterpolator(new DecelerateInterpolator()).start();
                    mArtistRow.animate().alpha(0f).setStartDelay(83).setDuration(84).withEndAction(() -> {
                        // Apply updated metadata
                        if (mTvTitle != null) {
                            mTvTitle.setText(safeTitle);
                            mTvTitle.setTranslationX(shiftPx);
                            mTvTitle.setAlpha(0f);
                            // Pixel SystemUI media_metadata_enter (600ms, translationX 32dp -> 0, alpha 0->1 in 167ms)
                            mTvTitle.animate().setListener(null).translationX(0f).setDuration(600)
                                    .setInterpolator(new DecelerateInterpolator(1.8f)).start();
                            mTvTitle.animate().alpha(1f).setDuration(167).start();
                        }
                        if (mTvArtist != null) {
                            mTvArtist.setText(artistDisplay);
                        }
                        if (mArtistRow != null) {
                            mArtistRow.setTranslationX(shiftPx);
                            mArtistRow.setAlpha(0f);
                            mArtistRow.animate().setListener(null).translationX(0f).setDuration(600)
                                    .setInterpolator(new DecelerateInterpolator(1.8f)).start();
                            mArtistRow.animate().alpha(1f).setDuration(167).start();
                        }
                    }).start();
                }
            } else {
                if (mTvTitle != null) {
                    mTvTitle.setText(safeTitle);
                    mTvTitle.setSelected(true);
                }
                if (mTvArtist != null) {
                    mTvArtist.setText(artistDisplay);
                }
            }

            if (mIvAppIcon != null) {
                if (state.appIcon != null) {
                    mIvAppIcon.setImageDrawable(state.appIcon);
                    mIvAppIcon.setVisibility(View.VISIBLE);
                } else {
                    mIvAppIcon.setVisibility(View.GONE);
                }
            }

            // Album art with smooth transition
            if (mIvAlbumArt != null && state.albumArt != null && !state.albumArt.isRecycled()) {
                if (trackChanged && mLastAlbumBitmap != null && mIsShowing) {
                    mIvAlbumArt.animate().setListener(null).alpha(0.2f).setDuration(150).withEndAction(() -> {
                        mIvAlbumArt.setImageDrawable(new BitmapDrawable(mContext.getResources(), state.albumArt));
                        mIvAlbumArt.animate().alpha(1.0f).setDuration(220).start();
                    }).start();
                } else {
                    mIvAlbumArt.setImageDrawable(new BitmapDrawable(mContext.getResources(), state.albumArt));
                }
                mLastAlbumBitmap = state.albumArt;
            }

            if (!mIsPlayPauseAnimating) {
                boolean playPauseChanged = mLastIsPlaying == null || (mLastIsPlaying != state.isPlaying) || (mCurrentMonetColor != state.monetColor);
                if (playPauseChanged) {
                    bindPlayPauseButton(state.isPlaying, state.monetColor);
                }
            }

            // Monet Color Transition (333ms ArgbEvaluator from SystemUI ColorSchemeTransition)
            ArgbEvaluator evaluator = new ArgbEvaluator();
            if (mCurrentMonetColor != state.monetColor) {
                int oldColor = mCurrentMonetColor;
                int targetColor = state.monetColor;
                mCurrentMonetColor = targetColor;
                if (mColorTransitionAnimator != null) {
                    mColorTransitionAnimator.cancel();
                }
                ValueAnimator ca = ValueAnimator.ofFloat(0f, 1f);
                ca.setDuration(333);
                ca.addUpdateListener(anim -> {
                    float f = anim.getAnimatedFraction();
                    int blended = (Integer) evaluator.evaluate(f, oldColor, targetColor);
                    updateCardAndAccentColors(blended);
                });
                ca.start();
                mColorTransitionAnimator = ca;
            } else {
                updateCardAndAccentColors(state.monetColor);
            }

            // Squiggly Progress
            if (mSquigglyProgress != null) {
                mSquigglyProgress.setWaveColor(state.monetColor);
                mSquigglyProgress.setAnimate(state.isPlaying);
            }

            if (mSeekBar != null && state.duration > 0) {
                mSeekBar.setMax((int) state.duration);
                mSeekBar.setProgress((int) state.position);
                mSeekBar.setThumbTintList(ColorStateList.valueOf(state.monetColor));
                if (mSquigglyProgress != null) {
                    mSquigglyProgress.setProgressFraction((float) state.position / state.duration);
                }
            }

            if (mTvSeamlessText != null) {
                mTvSeamlessText.setText(getActiveAudioDeviceName());
            }

            // Update Like & Shuffle dynamic custom actions
            updateCustomActions(state);

            // Update Visualizer
            AudioVisualizerController.get().setPlaying(state.isPlaying);
            AudioVisualizerController.get().setSensitivity(ChipSettings.get().visualizerSensitivity / 100.0f);
            boolean showCardVis = ChipSettings.get().visualizerCardEnabled;
            updateEqualizerConstraints(showCardVis, mIsShowing);
            if (mSpectrumVisualizer != null) {
                mSpectrumVisualizer.setMonetColor(state.monetColor);
                mSpectrumVisualizer.setActive(showCardVis && mIsShowing);
            }

        } catch (Throwable t) {
            Log.e(TAG, "Error in updateState: ", t);
        }
    }

    private int adjustMonetColorForBridge(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = Math.min(0.50f, hsv[1]);
        hsv[2] = Math.max(0.18f, Math.min(hsv[2], 0.35f));
        return Color.HSVToColor(0xFF, hsv);
    }

    private String getActiveAudioDeviceName() {
        try {
            AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                AudioDeviceInfo[] devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
                for (AudioDeviceInfo d : devices) {
                    if (d.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || d.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                        CharSequence name = d.getProductName();
                        return name != null ? name.toString() : "Bluetooth";
                    }
                }
            }
        } catch (Throwable ignored) {}
        return "Этот телефон";
    }

    private int adjustMonetForSeamless(int color) {
        float[] hsv = new float[3]; Color.colorToHSV(color, hsv); hsv[1] = Math.max(0.35f, hsv[1] * 0.75f); hsv[2] = Math.max(0.75f, hsv[2]); return Color.HSVToColor(0xDD, hsv);
    }

    public void showFromChip(View chipView, MediaSessionController.MediaState state) {
        if (mPlayerCardContainer == null) return;
        if (mIsShowing && !mIsDismissing) return;

        this.mSourceChipView = chipView;
        this.mHasSnapped = false;
        updateState(state);

        boolean needsWindowAdd = (mRootOverlay.getParent() == null);
        if (needsWindowAdd) {
            int windowType = 2017;
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                    windowType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
                    PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.TOP | Gravity.START;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                lp.setFitInsetsTypes(0);
                lp.setFitInsetsSides(0);
            }

            mRootOverlay.setFitsSystemWindows(false);
            mRootOverlay.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );

            try {
                mWindowManager.addView(mRootOverlay, lp);
            } catch (Throwable t) {
                lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                try {
                    mWindowManager.addView(mRootOverlay, lp);
                } catch (Throwable t2) {
                    Log.e(TAG, "Failed to add popup", t2);
                    return;
                }
            }
        }

        mIsShowing = true;
        mIsDismissing = false;
        mOpenedTimestamp = System.currentTimeMillis();
        if (mSpectrumVisualizer != null && ChipSettings.get().visualizerCardEnabled) {
            mSpectrumVisualizer.setActive(true);
        }

        // Get absolute chip screen bounds (accounting for any active scale)
        int[] chipLoc = new int[2];
        chipView.getLocationOnScreen(chipLoc);
        float w = chipView.getWidth() * chipView.getScaleX();
        float h = chipView.getHeight() * chipView.getScaleY();
        mChipRect.set(chipLoc[0], chipLoc[1], chipLoc[0] + w, chipLoc[1] + h);

        int cardMargin = convertDpToPx(16);
        int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        int targetCardWidth = screenWidth - cardMargin * 2;
        int cardHeight = convertDpToPx(195);
        int targetTop = Math.max(convertDpToPx(56), (int) mChipRect.bottom + convertDpToPx(8));

        mTargetCardRect.set(cardMargin, targetTop, screenWidth - cardMargin, targetTop + cardHeight);

        FrameLayout.LayoutParams cardLp = (FrameLayout.LayoutParams) mPlayerCardContainer.getLayoutParams();
        if (cardLp != null) {
            cardLp.width = targetCardWidth;
            cardLp.height = cardHeight;
            cardLp.leftMargin = cardMargin;
            cardLp.topMargin = targetTop;
            mPlayerCardContainer.setLayoutParams(cardLp);
        }

        // Emil Kowalski origin-aware popover rule:
        // Set pivot anchored horizontally towards the triggering chip
        float localPivotX = Math.max(0, Math.min(targetCardWidth, mChipRect.centerX() - mTargetCardRect.left));
        mPlayerCardContainer.setPivotX(localPivotX);
        mPlayerCardContainer.setPivotY(0);

        mHandler.removeCallbacks(mProgressRunnable);
        mHandler.post(mProgressRunnable);

        startExpansionAnimation(true, null);
    }

    private final Runnable mProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mIsShowing || mCurrentState == null) return;
            if (mCurrentState.isPlaying && mSeekBar != null && mCurrentState.duration > 0) {
                int cur = mSeekBar.getProgress() + 500;
                if (cur <= mCurrentState.duration) {
                    mSeekBar.setProgress(cur);
                    if (mSquigglyProgress != null) {
                        mSquigglyProgress.setProgressFraction((float) cur / mCurrentState.duration);
                    }
                }
            }
            if (mIsShowing) {
                mHandler.postDelayed(this, 500L);
            }
        }
    };

    private void startExpansionAnimation(boolean expand, Runnable onEnd) {
        this.mIsExpanding = expand;
        float startVal = mBridgeProgress;
        float targetVal = expand ? 1.0f : 0.0f;

        if (mExpansionAnimator != null) {
            if (mExpansionAnimator.isRunning()) {
                startVal = (Float) mExpansionAnimator.getAnimatedValue();
            }
            mExpansionAnimator.removeAllListeners();
            mExpansionAnimator.cancel();
            mExpansionAnimator = null;
        }

        float delta = Math.abs(targetVal - startVal);
        if (delta < 0.01f) {
            mBridgeProgress = targetVal;
            updateCardMorph(targetVal);
            mRootOverlay.invalidate();
            if (onEnd != null) onEnd.run();
            return;
        }

        // Organic Droplet Timing: 380ms enter, 240ms exit (Emil Kowalski / Material Expressive rule)
        int fullDuration = expand ? 380 : 240;
        long duration = Math.max(70L, Math.round(fullDuration * delta));

        mExpansionAnimator = ValueAnimator.ofFloat(startVal, targetVal);
        mExpansionAnimator.setDuration(duration);
        mExpansionAnimator.setInterpolator(expand ? M3Motion.DROPLET_PULL_ENTER : M3Motion.DROPLET_COALESCE_EXIT);

        mExpansionAnimator.addUpdateListener(anim -> {
            mBridgeProgress = (float) anim.getAnimatedValue();
            updateCardMorph(mBridgeProgress);
            mRootOverlay.invalidate();
        });

        mExpansionAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean mCanceled = false;

            @Override
            public void onAnimationCancel(Animator animation) {
                mCanceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mCanceled) return;
                mBridgeProgress = targetVal;
                updateCardMorph(targetVal);
                mRootOverlay.invalidate();
                if (onEnd != null) onEnd.run();
            }
        });
        mExpansionAnimator.start();
    }

    private View[][] getStaggerClusters() {
        return new View[][]{
                // Cluster 0: Media Header (Artwork & Dim & App icon & Audio chip)
                new View[]{mIvAlbumArt, mArtDimOverlay, mIvAppIcon, mSeamlessButton},
                // Cluster 1: Typography (Title & Artist)
                new View[]{mTvTitle, mArtistRow},
                // Cluster 2: Playback Controls (Play/Pause, Prev, Next, Shuffle, Like)
                new View[]{mBtnPlayPause, mBtnPrev, mBtnNext, mBtnShuffle, mBtnLike},
                // Cluster 3: Progress & Visualizer (SeekBar & Spectrum Visualizer)
                new View[]{mSeekBar, mSpectrumVisualizer}
        };
    }

    private void animateContentIn() {
        View[][] clusters = getStaggerClusters();
        for (int clusterIdx = 0; clusterIdx < clusters.length; clusterIdx++) {
            long delay = (long) clusterIdx * M3Motion.STAGGER_DELAY_STEP;
            View[] views = clusters[clusterIdx];
            for (View v : views) {
                if (v == null) continue;
                v.animate().cancel();
                v.setAlpha(0f);
                if (clusterIdx == 0) {
                    v.setScaleX(0.97f);
                    v.setScaleY(0.97f);
                    v.setTranslationY(0f);
                } else if (clusterIdx == 2) {
                    v.setScaleX(0.96f);
                    v.setScaleY(0.96f);
                    v.setTranslationY(convertDpToPx(6));
                } else {
                    v.setScaleX(1.0f);
                    v.setScaleY(1.0f);
                    v.setTranslationY(convertDpToPx(8));
                }

                v.animate()
                        .setListener(null)
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .translationY(0f)
                        .setStartDelay(delay)
                        .setDuration(M3Motion.STAGGER_DURATION)
                        .setInterpolator(M3Motion.EASE_OUT)
                        .start();
            }
        }
    }

    private void cancelContentAnimators() {
        View[][] clusters = getStaggerClusters();
        for (View[] cluster : clusters) {
            for (View v : cluster) {
                if (v != null) {
                    v.animate().cancel();
                    v.animate().alpha(0f).setDuration(120).start();
                }
            }
        }
    }

    public void dismiss() {
        if (!mIsShowing || mPlayerCardContainer == null || mIsDismissing) return;
        mIsDismissing = true;
        mHandler.removeCallbacks(mProgressRunnable);
        if (mSquigglyProgress != null) {
            mSquigglyProgress.setAnimate(false);
        }
        if (mSpectrumVisualizer != null) {
            mSpectrumVisualizer.setActive(false);
        }
        if (mColorTransitionAnimator != null) {
            mColorTransitionAnimator.cancel();
        }
        cancelContentAnimators();

        // Asymmetric exit timing: closing must be faster than opening (Emil Kowalski principle)
        startExpansionAnimation(false, () -> {
            try {
                if (mRootOverlay != null && mRootOverlay.isAttachedToWindow()) {
                    mWindowManager.removeView(mRootOverlay);
                }
            } catch (Throwable ignored) {}
            if (mSourceChipView != null) {
                // Organic capillary absorption pulse into mini-chip
                mSourceChipView.animate().setListener(null)
                        .scaleX(1.04f).scaleY(0.96f)
                        .setDuration(90)
                        .setInterpolator(M3Motion.EASE_OUT)
                        .withEndAction(() -> {
                            if (mSourceChipView != null) {
                                mSourceChipView.animate().setListener(null)
                                        .scaleX(1.0f).scaleY(1.0f)
                                        .setDuration(180)
                                        .setInterpolator(M3Motion.SPRING_SPATIAL)
                                        .start();
                            }
                        }).start();
            }
            mIsShowing = false;
            mIsDismissing = false;
            mIsPlayPauseAnimating = false;
        });
    }

    public boolean isShowing() { return mIsShowing; }

    private int convertDpToPx(int dp) {
        return Math.round(dp * mContext.getResources().getDisplayMetrics().density);
    }
}
