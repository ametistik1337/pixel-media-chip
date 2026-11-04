package com.pixel.mediachip;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MediaChipView extends FrameLayout implements MediaSessionController.MediaListener {
    private static final String TAG = "PixelMediaChip_View";

    private final Context mContext;
    private MediaSessionController mSessionController;
    private MediaExpandedWindow mExpandedWindow;
    private MediaSessionController.MediaState mCurrentState;

    private LinearLayout mChipContainer;
    private ImageView mIvIcon;
    private TextView mTvTitle;
    private GestureDetector mGestureDetector;
    private Vibrator mVibrator;

    public MediaChipView(Context context) {
        super(context);
        this.mContext = context;
        init();
    }

    public MediaChipView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        init();
    }

    private void init() {
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        mSessionController = new MediaSessionController(mContext, this);
        mExpandedWindow = new MediaExpandedWindow(mContext, mSessionController);

        int height = convertDpToPx(26);
        int marginStart = convertDpToPx(4);
        int marginEnd = convertDpToPx(4);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                height
        );
        lp.gravity = Gravity.CENTER_VERTICAL;
        lp.setMarginStart(marginStart);
        lp.setMarginEnd(marginEnd);
        setLayoutParams(lp);

        mChipContainer = new LinearLayout(mContext);
        mChipContainer.setOrientation(LinearLayout.HORIZONTAL);
        mChipContainer.setGravity(Gravity.CENTER_VERTICAL);
        int paddingSide = convertDpToPx(8);
        mChipContainer.setPadding(paddingSide, 0, paddingSide, 0);

        // Icon / Album preview
        mIvIcon = new ImageView(mContext);
        int iconSize = convertDpToPx(16);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconLp.setMarginEnd(convertDpToPx(6));
        mIvIcon.setLayoutParams(iconLp);
        mIvIcon.setImageResource(android.R.drawable.ic_media_play);
        mIvIcon.setColorFilter(Color.WHITE);
        mChipContainer.addView(mIvIcon);

        // Title text
        mTvTitle = new TextView(mContext);
        mTvTitle.setTextSize(12);
        mTvTitle.setTextColor(Color.WHITE);
        mTvTitle.setSingleLine(true);
        mTvTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        mTvTitle.setSelected(true);
        mTvTitle.setMaxWidth(convertDpToPx(130));
        mTvTitle.setText("Media");

        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
        );
        mTvTitle.setLayoutParams(textLp);
        mChipContainer.addView(mTvTitle);

        addView(mChipContainer);

        setupGestures();

        // Default hidden
        setVisibility(View.GONE);
    }

    private void setupGestures() {
        mGestureDetector = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                doHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                mSessionController.playPause();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (Math.abs(velocityX) > Math.abs(velocityY)) {
                    if (velocityX > 200) { // Swipe Right -> Next
                        doHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        mSessionController.next();
                        return true;
                    } else if (velocityX < -200) { // Swipe Left -> Prev
                        doHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        mSessionController.previous();
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                doHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                if (mCurrentState != null) {
                    mExpandedWindow.show(MediaChipView.this, mCurrentState);
                }
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });

        setOnTouchListener((v, event) -> mGestureDetector.onTouchEvent(event));
    }

    private void doHapticFeedback(int feedbackConstant) {
        try {
            performHapticFeedback(feedbackConstant, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        } catch (Throwable t) {
            if (mVibrator != null && mVibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mVibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    mVibrator.vibrate(40);
                }
            }
        }
    }

    @Override
    public void onMediaUpdated(MediaSessionController.MediaState state) {
        this.mCurrentState = state;
        boolean hasTitle = state != null && state.title != null && !state.title.isEmpty();
        boolean isPlaying = state != null && state.isPlaying;

        if (state == null || (!hasTitle && !isPlaying)) {
            setVisibility(View.GONE);
            if (mExpandedWindow.isShowing()) {
                mExpandedWindow.dismiss();
            }
            return;
        }

        setVisibility(View.VISIBLE);

        String safeTitle = (state.title != null) ? state.title : "";
        String safeArtist = (state.artist != null) ? state.artist : "";
        String displayText = safeTitle;
        if (!safeArtist.isEmpty()) {
            displayText = safeTitle.isEmpty() ? safeArtist : (safeArtist + " - " + safeTitle);
        }
        mTvTitle.setText(displayText);

        if (state.albumArt != null) {
            mIvIcon.setImageBitmap(state.albumArt);
            mIvIcon.clearColorFilter();
        } else {
            mIvIcon.setImageResource(state.isPlaying ?
                    android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
            mIvIcon.setColorFilter(Color.WHITE);
        }

        // Apply Monet background
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(convertDpToPx(14));
        bg.setColor(adjustMonetColorForChip(state.monetColor));
        mChipContainer.setBackground(bg);

        if (mExpandedWindow.isShowing()) {
            mExpandedWindow.updateState(state);
        }
    }

    private int adjustMonetColorForChip(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = Math.max(0.25f, Math.min(hsv[2], 0.6f));
        return Color.HSVToColor(0xCC, hsv);
    }

    private int convertDpToPx(int dp) {
        return Math.round(dp * mContext.getResources().getDisplayMetrics().density);
    }
}
