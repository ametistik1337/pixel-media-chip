package com.pixel.mediachip;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

public class MediaExpandedWindow {
    private final Context mContext;
    private final WindowManager mWindowManager;
    private final MediaSessionController mSessionController;

    private FrameLayout mRootView;
    private FrameLayout mCardContainer;
    private CardView mArtCardView;
    private ImageView mIvAlbumArt;
    private TextView mTvTitle;
    private TextView mTvArtist;
    private SeekBar mSeekBar;
    private android.widget.LinearLayout mControlsLayout;
    private ImageView mBtnPrev;
    private ImageView mBtnPlayPause;
    private ImageView mBtnNext;
    private boolean mIsShowing = false;

    public MediaExpandedWindow(Context context, MediaSessionController sessionController) {
        this.mContext = context;
        this.mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.mSessionController = sessionController;
        buildView();
    }

    private void buildView() {
        mRootView = new FrameLayout(mContext) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    dismiss();
                    return true;
                }
                return super.onTouchEvent(event);
            }
        };
        mRootView.setBackgroundColor(Color.parseColor("#44000000")); // semi-transparent dim

        // Outer Card Container for media player
        mCardContainer = new FrameLayout(mContext);
        int cardWidth = convertDpToPx(340);
        int cardPadding = convertDpToPx(16);

        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(
                cardWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardLp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        cardLp.topMargin = convertDpToPx(56); // Below statusbar cutout

        mCardContainer.setLayoutParams(cardLp);
        mCardContainer.setPadding(cardPadding, cardPadding, cardPadding, cardPadding);

        // Internal layout structure
        android.widget.LinearLayout contentLayout = new android.widget.LinearLayout(mContext);
        contentLayout.setOrientation(android.widget.LinearLayout.VERTICAL);

        // Album Art with concentric radius (R_inner = max(0, R_outer - padding) -> 28dp - 16dp = 12dp)
        int concentricArtRadius = M3Motion.calculateConcentricRadius(28, 16);
        mArtCardView = new CardView(mContext);
        mArtCardView.setRadius(convertDpToPx(concentricArtRadius));
        mArtCardView.setElevation(convertDpToPx(8));
        android.widget.LinearLayout.LayoutParams artLp = new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                convertDpToPx(200)
        );
        artLp.bottomMargin = convertDpToPx(12);

        mIvAlbumArt = new ImageView(mContext);
        mIvAlbumArt.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mIvAlbumArt.setImageResource(android.R.drawable.ic_menu_gallery);
        mArtCardView.addView(mIvAlbumArt, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        contentLayout.addView(mArtCardView, artLp);

        // Title
        mTvTitle = new TextView(mContext);
        mTvTitle.setTextSize(18);
        mTvTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        mTvTitle.setTextColor(Color.WHITE);
        mTvTitle.setSingleLine(true);
        mTvTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        contentLayout.addView(mTvTitle);

        // Artist
        mTvArtist = new TextView(mContext);
        mTvArtist.setTextSize(14);
        mTvArtist.setTextColor(Color.parseColor("#CCFFFFFF"));
        mTvArtist.setSingleLine(true);
        mTvArtist.setEllipsize(android.text.TextUtils.TruncateAt.END);
        android.widget.LinearLayout.LayoutParams artistLp = new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        artistLp.bottomMargin = convertDpToPx(12);
        contentLayout.addView(mTvArtist, artistLp);

        // SeekBar
        mSeekBar = new SeekBar(mContext);
        mSeekBar.setPadding(0, 0, 0, 0);
        android.widget.LinearLayout.LayoutParams seekLp = new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        seekLp.bottomMargin = convertDpToPx(12);
        contentLayout.addView(mSeekBar, seekLp);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mSessionController.seekTo(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Media Controls (Prev, Play/Pause, Next)
        mControlsLayout = new android.widget.LinearLayout(mContext);
        mControlsLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        mControlsLayout.setGravity(Gravity.CENTER);

        mBtnPrev = createControlButton(android.R.drawable.ic_media_previous, v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            mSessionController.previous();
        });

        mBtnPlayPause = createControlButton(android.R.drawable.ic_media_pause, v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            mSessionController.playPause();
        });

        mBtnNext = createControlButton(android.R.drawable.ic_media_next, v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            mSessionController.next();
        });

        mControlsLayout.addView(mBtnPrev);
        mControlsLayout.addView(mBtnPlayPause);
        mControlsLayout.addView(mBtnNext);

        contentLayout.addView(mControlsLayout);
        mCardContainer.addView(contentLayout);
        mRootView.addView(mCardContainer);
    }

    private ImageView createControlButton(int iconRes, View.OnClickListener clickListener) {
        ImageView btn = new ImageView(mContext);
        btn.setImageResource(iconRes);
        btn.setColorFilter(Color.WHITE);
        int btnSize = convertDpToPx(48);
        int btnPadding = convertDpToPx(8);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(btnSize, btnSize);
        lp.setMargins(convertDpToPx(16), 0, convertDpToPx(16), 0);
        btn.setLayoutParams(lp);
        btn.setPadding(btnPadding, btnPadding, btnPadding, btnPadding);
        M3Motion.attachTactilePress(btn, clickListener, true);
        return btn;
    }

    public void updateState(MediaSessionController.MediaState state) {
        if (state == null) return;
        mTvTitle.setText(state.title != null ? state.title : "");
        mTvArtist.setText(state.artist != null ? state.artist : "");
        if (state.albumArt != null) {
            mIvAlbumArt.setImageBitmap(state.albumArt);
        } else {
            mIvAlbumArt.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        mBtnPlayPause.setImageResource(state.isPlaying ?
                android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);

        if (state.duration > 0) {
            mSeekBar.setMax((int) state.duration);
            mSeekBar.setProgress((int) state.position);
        }

        // Apply Monet background
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(convertDpToPx(28));
        bg.setColor(adjustMonetColorForExpandedBg(state.monetColor));
        mCardContainer.setBackground(bg);
    }

    private int adjustMonetColorForExpandedBg(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = Math.max(0.15f, Math.min(hsv[2], 0.35f));
        return Color.HSVToColor(0xF0, hsv);
    }

    private boolean mIsDismissing = false;

    public void show(MediaSessionController.MediaState currentState) {
        show(null, currentState);
    }

    public void show(View triggerView, MediaSessionController.MediaState currentState) {
        if (mIsShowing && !mIsDismissing) return;
        updateState(currentState);

        boolean needsWindowAdd = (mRootView.getParent() == null);
        if (needsWindowAdd) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
            );
            lp.gravity = Gravity.TOP;

            try {
                mWindowManager.addView(mRootView, lp);
            } catch (Throwable t) {
                return;
            }
        }
        mIsShowing = true;
        mIsDismissing = false;

        // Cancel running animators for interruptibility
        mRootView.animate().setListener(null).cancel();
        mCardContainer.animate().setListener(null).cancel();

        // Origin-aware setup: emerge as a fluid droplet from trigger position
        float startTransY = -convertDpToPx(16);
        if (triggerView != null) {
            int[] loc = new int[2];
            triggerView.getLocationOnScreen(loc);
            float triggerCX = loc[0] + (triggerView.getWidth() * triggerView.getScaleX()) / 2f;
            float triggerBottom = loc[1] + (triggerView.getHeight() * triggerView.getScaleY());
            int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
            int cardLeft = (screenWidth - convertDpToPx(340)) / 2;
            float localPivotX = Math.max(0, Math.min(convertDpToPx(340), triggerCX - cardLeft));
            mCardContainer.setPivotX(localPivotX);
            startTransY = Math.min(0, triggerBottom - convertDpToPx(56));
        } else {
            int containerW = mCardContainer.getWidth();
            mCardContainer.setPivotX(containerW > 0 ? containerW / 2f : convertDpToPx(170));
        }
        mCardContainer.setPivotY(0);
        mCardContainer.setScaleX(0.40f);
        mCardContainer.setScaleY(0.30f);
        mCardContainer.setTranslationY(startTransY);
        mCardContainer.setAlpha(0f);
        mRootView.setAlpha(0f);

        mRootView.animate()
                .setListener(null)
                .alpha(1f)
                .setDuration(M3Motion.DURATION_POPUP_ENTER)
                .setInterpolator(M3Motion.EASE_OUT)
                .start();

        mCardContainer.animate()
                .setListener(null)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .translationY(0f)
                .alpha(1.0f)
                .setDuration(M3Motion.DURATION_POPUP_ENTER)
                .setInterpolator(M3Motion.DROPLET_PULL_ENTER)
                .start();

        // Stagger entrance for internal items
        View[] staggerViews = new View[]{mArtCardView, mTvTitle, mTvArtist, mSeekBar, mControlsLayout};
        for (int i = 0; i < staggerViews.length; i++) {
            View v = staggerViews[i];
            if (v == null) continue;
            v.animate().cancel();
            v.setAlpha(0f);
            v.setTranslationY(convertDpToPx(8));
            v.animate()
                    .setListener(null)
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(35L + (long) i * M3Motion.STAGGER_DELAY_STEP)
                    .setDuration(M3Motion.STAGGER_DURATION)
                    .setInterpolator(M3Motion.EASE_OUT)
                    .start();
        }
    }

    public void dismiss() {
        if (!mIsShowing || mIsDismissing) return;
        mIsDismissing = true;

        // Cancel running stagger animations
        View[] staggerViews = new View[]{mArtCardView, mTvTitle, mTvArtist, mSeekBar, mControlsLayout};
        for (View v : staggerViews) {
            if (v != null) {
                v.animate().cancel();
            }
        }

        // Asymmetric exit timing with capillary absorption into chip
        int exitDuration = M3Motion.DURATION_POPUP_EXIT;

        mCardContainer.animate()
                .setListener(null)
                .scaleX(0.40f)
                .scaleY(0.30f)
                .translationY(-convertDpToPx(16))
                .alpha(0f)
                .setDuration(exitDuration)
                .setInterpolator(M3Motion.DROPLET_COALESCE_EXIT)
                .start();

        mRootView.animate()
                .setListener(null)
                .alpha(0f)
                .setDuration(exitDuration)
                .setInterpolator(M3Motion.FAST_OUT_EXIT)
                .setListener(new AnimatorListenerAdapter() {
                    private boolean mCanceled = false;

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        mCanceled = true;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (mCanceled) return;
                        try {
                            if (mRootView.isAttachedToWindow()) {
                                mWindowManager.removeView(mRootView);
                            }
                        } catch (Throwable ignored) {}
                        mIsShowing = false;
                        mIsDismissing = false;
                    }
                }).start();
    }

    public boolean isShowing() {
        return mIsShowing;
    }

    private int convertDpToPx(int dp) {
        return Math.round(dp * mContext.getResources().getDisplayMetrics().density);
    }
}
