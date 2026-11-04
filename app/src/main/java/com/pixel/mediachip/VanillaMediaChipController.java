package com.pixel.mediachip;

import android.animation.LayoutTransition;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedBridge;

public class VanillaMediaChipController implements MediaSessionController.MediaListener, View.OnTouchListener {
    private static final String TAG = "PixelMediaChip_Vanilla";
    private static final List<WeakReference<VanillaMediaChipController>> sInstances = new ArrayList<>();

    public static volatile boolean sIsScreenRecordingActive = false;
    public static volatile boolean sIsCallActive = false;

    private final Context mContext;
    private final ClassLoader mClassLoader;
    private final MediaSessionController mSessionController;
    private final VanillaMediaPopup mMediaPopup;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ChipSettings mSettings = ChipSettings.get();

    private ViewGroup mParentContainer;
    private FrameLayout mChipRootView;
    private LinearLayout mBgContainer;
    private ImageView mIvIcon;
    private MiniEqualizerView mMiniEqualizer;
    private LinearLayout mTextContainer;
    private MarqueeTextView mTvTitle;
    private MarqueeTextView mTvArtist;
    private MarqueeTextView mTvSingleLine;
    private Vibrator mVibrator;
    private MediaSessionController.MediaState mCurrentState;

    private View mNativePrimaryChip;
    private View mNativeSecondaryChip;
    private View mNotificationIconArea;
    private View mNotificationIcons;

    private static final long PAUSE_TIMEOUT_MS = 2 * 60 * 1000L; // 2 minutes
    private boolean mPauseTimeoutScheduled = false;
    private boolean mIsTimeoutHidden = false;
    private String mLastTrackTitle = null;

    private final Runnable mPauseTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (mCurrentState != null && mCurrentState.isPlaying) {
                mPauseTimeoutScheduled = false;
                return;
            }
            if (mMediaPopup != null && mMediaPopup.isShowing()) {
                mHandler.postDelayed(this, PAUSE_TIMEOUT_MS);
                return;
            }
            mPauseTimeoutScheduled = false;
            mIsTimeoutHidden = true;
            if (mMiniEqualizer != null) {
                mMiniEqualizer.setActive(false);
            }
            AudioVisualizerController.get().setPlaying(false);
            if (mChipRootView != null) {
                mChipRootView.setVisibility(View.GONE);
            }
            updateNotificationIconsVisibility();
            XposedBridge.log("PixelMediaChip: 2-minute pause timeout reached, hidden chip and restored notification icons");
        }
    };

    private void schedulePauseTimeout(boolean reset) {
        if (!reset && mPauseTimeoutScheduled) return;
        mPauseTimeoutScheduled = true;
        mHandler.removeCallbacks(mPauseTimeoutRunnable);
        mHandler.postDelayed(mPauseTimeoutRunnable, PAUSE_TIMEOUT_MS);
    }

    private void cancelPauseTimeout() {
        mPauseTimeoutScheduled = false;
        mHandler.removeCallbacks(mPauseTimeoutRunnable);
    }

    public static boolean isAnyChipShowing() {
        for (WeakReference<VanillaMediaChipController> ref : new ArrayList<>(sInstances)) {
            VanillaMediaChipController c = ref.get();
            if (c != null && c.isChipShowing()) {
                return true;
            }
        }
        return false;
    }

    public boolean isChipShowing() {
        boolean hasTitle = mCurrentState != null && mCurrentState.title != null && !mCurrentState.title.isEmpty();
        boolean isPlaying = mCurrentState != null && mCurrentState.isPlaying;
        return mChipRootView != null
                && mChipRootView.getVisibility() == View.VISIBLE
                && !mIsTimeoutHidden
                && mCurrentState != null
                && (hasTitle || isPlaying);
    }

    private float mStartX;
    private float mStartY;
    private boolean mIsLongPressed = false;
    private boolean mIsSquished = false;

    private final BroadcastReceiver mSettingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ChipSettings.ACTION_SETTINGS_CHANGED.equals(intent.getAction())) {
                if (intent.getExtras() != null) {
                    mSettings.fromBundle(intent.getExtras());
                    mSettings.saveToLocalPrefs(context);
                } else {
                    mSettings.load(context);
                }
                applySettings();
            }
        }
    };

    private final Runnable mLongPressRunnable = new Runnable() {
        @Override
        public void run() {
            mIsLongPressed = true;
            if (mSettings.hapticEnabled) {
                doHaptic(HapticFeedbackConstants.LONG_PRESS);
            }
            animateDropletDetach();
            if (mCurrentState != null && mBgContainer != null) {
                mMediaPopup.showFromChip(mBgContainer, mCurrentState);
            }
        }
    };

    private final ViewTreeObserver.OnPreDrawListener mPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
            checkNativeChipsAndAdapt();
            updateNotificationIconsVisibility();
            return true;
        }
    };

    public static void setScreenRecordingActive(boolean active) {
        sIsScreenRecordingActive = active;
        notifyStateChanged();
    }

    public static void setCallActive(boolean active) {
        sIsCallActive = active;
        notifyStateChanged();
    }

    private static void notifyStateChanged() {
        new Handler(Looper.getMainLooper()).post(() -> {
            for (WeakReference<VanillaMediaChipController> ref : new ArrayList<>(sInstances)) {
                VanillaMediaChipController c = ref.get();
                if (c != null) {
                    c.checkNativeChipsAndAdapt();
                }
            }
        });
    }

    public VanillaMediaChipController(Context context, ClassLoader classLoader, ViewGroup container, int insertIndex) {
        this.mContext = context;
        this.mClassLoader = classLoader;
        this.mParentContainer = container;
        this.mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        mSettings.load(context);
        sInstances.add(new WeakReference<>(this));

        buildCustomChip(container, insertIndex);
        applySettings();

        this.mSessionController = new MediaSessionController(context, this);
        this.mMediaPopup = new VanillaMediaPopup(context, this.mSessionController);

        try {
            IntentFilter filter = new IntentFilter(ChipSettings.ACTION_SETTINGS_CHANGED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mContext.registerReceiver(mSettingsReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                mContext.registerReceiver(mSettingsReceiver, filter);
            }
        } catch (Throwable ignored) {}

        if (mChipRootView != null) {
            mChipRootView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mSettings.load(mContext);
                    applySettings();
                }
            }, 1500);
        }
    }

    private void buildCustomChip(ViewGroup container, int insertIndex) {
        try {
            mChipRootView = new FrameLayout(mContext);
            mChipRootView.setId(View.generateViewId());
            mChipRootView.setMinimumWidth(0);

            LinearLayout.LayoutParams rootLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rootLp.gravity = Gravity.CENTER_VERTICAL;
            rootLp.leftMargin = convertDpToPx(4);
            mChipRootView.setLayoutParams(rootLp);

            mBgContainer = new LinearLayout(mContext);
            mBgContainer.setOrientation(LinearLayout.HORIZONTAL);
            mBgContainer.setGravity(Gravity.CENTER_VERTICAL);
            mBgContainer.setMinimumWidth(0);

            FrameLayout.LayoutParams bgLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, convertDpToPx(28)
            );
            bgLp.gravity = Gravity.CENTER_VERTICAL;
            if (mSettings.eatCamera) {
                bgLp.rightMargin = -convertDpToPx(16);
            }
            mBgContainer.setLayoutParams(bgLp);
            mBgContainer.setElevation(convertDpToPx(8));
            mBgContainer.setTranslationZ(convertDpToPx(4));

            GradientDrawable pillBg = new GradientDrawable();
            pillBg.setCornerRadius(convertDpToPx(mSettings.cornerRadiusDp));
            pillBg.setColor(Color.parseColor("#FF1A1C1E"));
            mBgContainer.setBackground(pillBg);
            int rightPad = mSettings.eatCamera ? convertDpToPx(24) : convertDpToPx(8);
            mBgContainer.setPadding(convertDpToPx(5), 0, rightPad, 0);

            if (mSettings.eatCamera) {
                mChipRootView.setClipChildren(false);
                mChipRootView.setClipToPadding(false);
                if (container != null) {
                    container.setClipChildren(false);
                    container.setClipToPadding(false);
                }
            }

            LayoutTransition lt = new LayoutTransition();
            lt.setDuration(150);
            lt.enableTransitionType(LayoutTransition.CHANGING);
            mBgContainer.setLayoutTransition(lt);

            // Mini Equalizer (3-band live audio visualizer)
            mMiniEqualizer = new MiniEqualizerView(mContext);
            LinearLayout.LayoutParams eqLp = new LinearLayout.LayoutParams(convertDpToPx(16), convertDpToPx(16));
            eqLp.gravity = Gravity.CENTER_VERTICAL;
            eqLp.rightMargin = convertDpToPx(5);
            mMiniEqualizer.setLayoutParams(eqLp);
            mMiniEqualizer.setVisibility(View.GONE);
            mBgContainer.addView(mMiniEqualizer);

            // Icon
            mIvIcon = new ImageView(mContext);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(convertDpToPx(18), convertDpToPx(18));
            iconLp.gravity = Gravity.CENTER_VERTICAL;
            iconLp.rightMargin = convertDpToPx(5);
            mIvIcon.setLayoutParams(iconLp);
            mIvIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            mBgContainer.addView(mIvIcon);

            // Two-line layout container
            mTextContainer = new LinearLayout(mContext);
            mTextContainer.setOrientation(LinearLayout.VERTICAL);
            mTextContainer.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);

            LinearLayout.LayoutParams textContainerLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            textContainerLp.gravity = Gravity.CENTER_VERTICAL;
            mTextContainer.setLayoutParams(textContainerLp);

            // Title (top)
            mTvTitle = new MarqueeTextView(mContext);
            mTvTitle.setTextColor(Color.WHITE);
            mTvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, mSettings.titleSizeSp);
            mTvTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            mTvTitle.setIncludeFontPadding(false);
            mTvTitle.setPadding(0, 0, 0, 0);
            int initTextMax = Math.max(convertDpToPx(mSettings.maxWidthDp), convertDpToPx(mSettings.maxTextWidthDp));
            mTvTitle.setMaxWidth(initTextMax);
            mTvTitle.setMarqueeEnabled(mSettings.marqueeOneLine);

            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            mTvTitle.setLayoutParams(titleLp);
            mTextContainer.addView(mTvTitle);

            // Artist (bottom)
            mTvArtist = new MarqueeTextView(mContext);
            mTvArtist.setTextColor(Color.parseColor("#B3FFFFFF"));
            mTvArtist.setTextSize(TypedValue.COMPLEX_UNIT_SP, mSettings.artistSizeSp);
            mTvArtist.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            mTvArtist.setIncludeFontPadding(false);
            mTvArtist.setPadding(0, 0, 0, 0);
            mTvArtist.setMaxWidth(convertDpToPx(mSettings.maxWidthDp));
            mTvArtist.setMarqueeEnabled(mSettings.marqueeOneLine);

            LinearLayout.LayoutParams artistLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            artistLp.topMargin = convertDpToPx(1);
            mTvArtist.setLayoutParams(artistLp);
            mTextContainer.addView(mTvArtist);

            mBgContainer.addView(mTextContainer);

            // Single line layout (Marquee / Strip)
            mTvSingleLine = new MarqueeTextView(mContext);
            mTvSingleLine.setTextColor(Color.WHITE);
            mTvSingleLine.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.0f);
            mTvSingleLine.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            mTvSingleLine.setIncludeFontPadding(false);
            mTvSingleLine.setMaxWidth(convertDpToPx(mSettings.maxWidthDp));
            mTvSingleLine.setMarqueeEnabled(mSettings.marqueeOneLine);

            LinearLayout.LayoutParams singleLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            singleLp.gravity = Gravity.CENTER_VERTICAL;
            mTvSingleLine.setLayoutParams(singleLp);
            mTvSingleLine.setVisibility(View.GONE);
            mBgContainer.addView(mTvSingleLine);

            mBgContainer.setOnTouchListener(this);

            mChipRootView.addView(mBgContainer);
            mChipRootView.setVisibility(View.GONE);

            findNativeChips(container);

            if (insertIndex >= 0 && insertIndex <= container.getChildCount()) {
                container.addView(mChipRootView, insertIndex);
            } else {
                container.addView(mChipRootView);
            }

            container.getViewTreeObserver().addOnPreDrawListener(mPreDrawListener);

            applySettings();
            XposedBridge.log("PixelMediaChip: Custom chip initialized with real-time settings support!");
        } catch (Throwable t) {
            Log.e(TAG, "Error building custom chip: ", t);
        }
    }

    public void applySettings() {
        if (mBgContainer == null) return;

        GradientDrawable bg = (GradientDrawable) mBgContainer.getBackground();
        if (bg != null) {
            bg.setCornerRadius(convertDpToPx(mSettings.cornerRadiusDp));
            if (mSettings.monetTint && mCurrentState != null && mCurrentState.monetColor != 0) {
                bg.mutate().setTint(adjustMonetColorForChip(mCurrentState.monetColor));
            } else {
                bg.mutate().setTintList(null);
                bg.setColor(Color.parseColor("#FF141518"));
            }
        }

        int textMaxPx = Math.max(convertDpToPx(mSettings.maxTextWidthDp), convertDpToPx(mSettings.maxWidthDp));
        if (mTvTitle != null) {
            mTvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, mSettings.titleSizeSp);
            mTvTitle.setMaxWidth(textMaxPx);
            mTvTitle.setMarqueeEnabled(mSettings.marqueeOneLine);
        }
        if (mTvArtist != null) {
            mTvArtist.setTextSize(TypedValue.COMPLEX_UNIT_SP, mSettings.artistSizeSp);
            mTvArtist.setMaxWidth(textMaxPx);
            mTvArtist.setVisibility(mSettings.showArtist ? View.VISIBLE : View.GONE);
            mTvArtist.setMarqueeEnabled(mSettings.marqueeOneLine);
        }
        if (mTvSingleLine != null) {
            mTvSingleLine.setMaxWidth(textMaxPx);
            mTvSingleLine.setMarqueeEnabled(mSettings.marqueeOneLine);
        }

        if (mBgContainer != null) {
            int depth = mSettings.eatCamera ? mSettings.cameraEatDepthDp : 0;
            int rightPad = mSettings.eatCamera ? convertDpToPx(depth + 8) : convertDpToPx(8);
            mBgContainer.setPadding(convertDpToPx(5), 0, rightPad, 0);

            FrameLayout.LayoutParams bgLp = (FrameLayout.LayoutParams) mBgContainer.getLayoutParams();
            if (bgLp != null) {
                bgLp.rightMargin = mSettings.eatCamera ? -convertDpToPx(depth) : 0;
                mBgContainer.setLayoutParams(bgLp);
            mBgContainer.setElevation(convertDpToPx(8));
            mBgContainer.setTranslationZ(convertDpToPx(4));
            }
        }
        if (mParentContainer != null && mSettings.eatCamera) {
            mParentContainer.setClipChildren(false);
            mParentContainer.setClipToPadding(false);
        }
        if (mChipRootView != null && mSettings.eatCamera) {
            mChipRootView.setClipChildren(false);
            mChipRootView.setClipToPadding(false);
        }

        AudioVisualizerController.get().setSensitivity(mSettings.visualizerSensitivity / 100.0f);
        boolean showMiniEqualizer = (mCurrentState != null && mCurrentState.isPlaying) && mSettings.visualizerChipEnabled;
        if (mMiniEqualizer != null) {
            mMiniEqualizer.setColor(Color.WHITE);
            mMiniEqualizer.setVisibility(showMiniEqualizer ? View.VISIBLE : View.GONE);
            mMiniEqualizer.setActive(showMiniEqualizer);
        }
        if (mIvIcon != null) {
            mIvIcon.setVisibility(showMiniEqualizer ? View.GONE : View.VISIBLE);
        }

        if (mTextContainer != null) {
            mTextContainer.requestLayout();
        }
        if (mBgContainer != null) {
            mBgContainer.requestLayout();
        }
        if (mChipRootView != null) {
            mChipRootView.requestLayout();
        }

        if (mCurrentState != null) {
            updateTextContent(mCurrentState);
            if (mMediaPopup != null) {
                mMediaPopup.updateState(mCurrentState);
            }
        } else if (mMediaPopup != null) {
            mMediaPopup.notifySettingsChanged();
        }
        updateLayoutMode();
        checkNativeChipsAndAdapt();
    }

    private void findNativeChips(ViewGroup container) {
        try {
            Resources res = mContext.getResources();
            int primaryId = res.getIdentifier("ongoing_activity_chip_primary", "id", "com.android.systemui");
            int secondaryId = res.getIdentifier("ongoing_activity_chip_secondary", "id", "com.android.systemui");

            View root = container.getRootView();
            if (root != null) {
                if (primaryId != 0) {
                    View found = root.findViewById(primaryId);
                    if (found != mChipRootView) mNativePrimaryChip = found;
                }
                if (secondaryId != 0) {
                    View found = root.findViewById(secondaryId);
                    if (found != mChipRootView) mNativeSecondaryChip = found;
                }
            }
        } catch (Throwable ignored) {}
        findNotificationIconsView();
    }

    private void findNotificationIconsView() {
        try {
            Resources res = mContext.getResources();
            int areaId = res.getIdentifier("notification_icon_area", "id", "com.android.systemui");
            int iconsId = res.getIdentifier("notificationIcons", "id", "com.android.systemui");

            View root = (mParentContainer != null) ? mParentContainer.getRootView() : null;
            if (root != null) {
                if (areaId != 0 && mNotificationIconArea == null) {
                    mNotificationIconArea = root.findViewById(areaId);
                }
                if (iconsId != 0 && mNotificationIcons == null) {
                    mNotificationIcons = root.findViewById(iconsId);
                }
            }
            if (mNotificationIconArea == null && mParentContainer != null) {
                if (areaId != 0) {
                    mNotificationIconArea = mParentContainer.findViewById(areaId);
                }
            }
            if (mNotificationIcons == null && mNotificationIconArea instanceof ViewGroup) {
                if (iconsId != 0) {
                    mNotificationIcons = mNotificationIconArea.findViewById(iconsId);
                }
            }

            if (mNotificationIconArea != null) {
                mNotificationIconArea.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
                    if (isChipShowing() && v.getVisibility() != View.GONE) {
                        v.post(() -> {
                            if (isChipShowing() && v.getVisibility() != View.GONE) {
                                v.setVisibility(View.GONE);
                                v.setAlpha(0f);
                            }
                        });
                    }
                });
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error finding notification icon views: ", t);
        }
    }

    public void updateNotificationIconsVisibility() {
        if (mNotificationIconArea == null && mParentContainer != null) {
            findNotificationIconsView();
        }

        boolean chipActive = isChipShowing();

        if (mNotificationIconArea != null) {
            int targetVis = chipActive ? View.GONE : View.VISIBLE;
            if (mNotificationIconArea.getVisibility() != targetVis) {
                mNotificationIconArea.setVisibility(targetVis);
            }
            float targetAlpha = chipActive ? 0.0f : 1.0f;
            if (mNotificationIconArea.getAlpha() != targetAlpha) {
                mNotificationIconArea.setAlpha(targetAlpha);
            }
        }

        if (mNotificationIcons != null) {
            int targetVis = chipActive ? View.GONE : View.VISIBLE;
            if (mNotificationIcons.getVisibility() != targetVis) {
                mNotificationIcons.setVisibility(targetVis);
            }
            float targetAlpha = chipActive ? 0.0f : 1.0f;
            if (mNotificationIcons.getAlpha() != targetAlpha) {
                mNotificationIcons.setAlpha(targetAlpha);
            }
        }
    }

    private void checkNativeChipsAndAdapt() {
        boolean hasTitle = mCurrentState != null && mCurrentState.title != null && !mCurrentState.title.isEmpty();
        boolean isPlaying = mCurrentState != null && mCurrentState.isPlaying;
        if (mCurrentState == null || (!hasTitle && !isPlaying) || mIsTimeoutHidden) {
            if (mChipRootView != null && mChipRootView.getVisibility() != View.GONE) {
                mChipRootView.setVisibility(View.GONE);
            }
            updateNotificationIconsVisibility();
            return;
        }

        boolean isNativeActive = sIsScreenRecordingActive || sIsCallActive || isNativeChipActive();

        if (isNativeActive) {
            if (mSettings.squishMode == ChipSettings.SQUISH_HIDE) {
                if (mChipRootView != null && mChipRootView.getVisibility() != View.GONE) {
                    mChipRootView.setVisibility(View.GONE);
                }
            } else if (mSettings.squishMode == ChipSettings.SQUISH_TO_ICON) {
                if (mChipRootView != null && mChipRootView.getVisibility() != View.VISIBLE) {
                    mChipRootView.setVisibility(View.VISIBLE);
                }
                setSquishedMode(true);
            } else {
                // Disabled squish
                if (mChipRootView != null && mChipRootView.getVisibility() != View.VISIBLE) {
                    mChipRootView.setVisibility(View.VISIBLE);
                }
                setSquishedMode(false);
            }
        } else {
            if (mChipRootView != null && mChipRootView.getVisibility() != View.VISIBLE) {
                mChipRootView.setVisibility(View.VISIBLE);
            }
            setSquishedMode(mSettings.layoutMode == ChipSettings.MODE_ICON_ONLY);
        }

        updateNotificationIconsVisibility();
    }

    private boolean isViewActuallyVisible(View v) {
        if (v == null || v == mChipRootView) return false;
        if (v.getVisibility() != View.VISIBLE || v.getAlpha() <= 0.1f) return false;
        if (v.getWidth() <= 0 || v.getHeight() <= 0) return false;

        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            if (vg.getChildCount() == 0) return true;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                if (isViewActuallyVisible(child)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private boolean isNativeChipActive() {
        if (mNativePrimaryChip == null || mNativeSecondaryChip == null) {
            if (mParentContainer != null) {
                findNativeChips(mParentContainer);
            }
        }

        if (isViewActuallyVisible(mNativePrimaryChip)) {
            return true;
        }

        if (isViewActuallyVisible(mNativeSecondaryChip)) {
            return true;
        }

        if (mParentContainer != null) {
            for (int i = 0; i < mParentContainer.getChildCount(); i++) {
                View child = mParentContainer.getChildAt(i);
                if (child == mChipRootView) continue;
                if (isClockView(child)) continue;

                int id = child.getId();
                String entryName = "";
                try {
                    entryName = mContext.getResources().getResourceEntryName(id);
                } catch (Throwable ignored) {}
                String clsName = child.getClass().getName();

                if (entryName.contains("chip") || entryName.contains("record") || entryName.contains("call")
                        || clsName.contains("Chip") || clsName.contains("OngoingActivity")) {
                    if (isViewActuallyVisible(child)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isClockView(View view) {
        if (view == null) return false;
        int id = view.getId();
        try {
            String name = mContext.getResources().getResourceEntryName(id);
            if (name.contains("clock")) return true;
        } catch (Throwable ignored) {}
        if (view instanceof TextView) {
            String text = ((TextView) view).getText().toString();
            if (text.contains(":") || text.matches(".*\\d.*")) {
                return true;
            }
        }
        return false;
    }

    private void setSquishedMode(boolean squish) {
        this.mIsSquished = squish;
        updateLayoutMode();
    }

    private void updateLayoutMode() {
        boolean effectiveSquish = mIsSquished || (mSettings.layoutMode == ChipSettings.MODE_ICON_ONLY);

        if (effectiveSquish) {
            if (mTextContainer != null) mTextContainer.setVisibility(View.GONE);
            if (mTvSingleLine != null) mTvSingleLine.setVisibility(View.GONE);
            if (mBgContainer != null && mIvIcon != null) {
                LinearLayout.LayoutParams iconLp = (LinearLayout.LayoutParams) mIvIcon.getLayoutParams();
                if (iconLp != null) iconLp.rightMargin = 0;
                mIvIcon.setLayoutParams(iconLp);
                mBgContainer.setPadding(convertDpToPx(5), 0, convertDpToPx(5), 0);
            }
            if (mMiniEqualizer != null) {
                LinearLayout.LayoutParams eqLp = (LinearLayout.LayoutParams) mMiniEqualizer.getLayoutParams();
                if (eqLp != null) {
                    eqLp.rightMargin = 0;
                    mMiniEqualizer.setLayoutParams(eqLp);
                }
            }
        } else {
            if (mSettings.layoutMode == ChipSettings.MODE_ONE_LINE) {
                if (mTextContainer != null) mTextContainer.setVisibility(View.GONE);
                if (mTvSingleLine != null) mTvSingleLine.setVisibility(View.VISIBLE);
            } else {
                if (mTextContainer != null) mTextContainer.setVisibility(View.VISIBLE);
                if (mTvSingleLine != null) mTvSingleLine.setVisibility(View.GONE);
            }

            if (mBgContainer != null && mIvIcon != null) {
                LinearLayout.LayoutParams iconLp = (LinearLayout.LayoutParams) mIvIcon.getLayoutParams();
                if (iconLp != null) iconLp.rightMargin = convertDpToPx(5);
                mIvIcon.setLayoutParams(iconLp);
                int depth = mSettings.eatCamera ? mSettings.cameraEatDepthDp : 0;
                int rightPad = mSettings.eatCamera ? convertDpToPx(depth + 8) : convertDpToPx(8);
                mBgContainer.setPadding(convertDpToPx(5), 0, rightPad, 0);
            }
            if (mMiniEqualizer != null) {
                LinearLayout.LayoutParams eqLp = (LinearLayout.LayoutParams) mMiniEqualizer.getLayoutParams();
                if (eqLp != null) {
                    eqLp.rightMargin = convertDpToPx(5);
                    mMiniEqualizer.setLayoutParams(eqLp);
                }
            }
        }
    }

    private void updateTextContent(MediaSessionController.MediaState state) {
        String safeTitle = (state != null && state.title != null) ? state.title : "";
        String safeArtist = (state != null && state.artist != null) ? state.artist : "";
        String safeAlbum = (state != null && state.album != null) ? state.album : "";

        if (mTvTitle != null) mTvTitle.setText(safeTitle);
        String artist = !safeArtist.isEmpty() ? safeArtist : safeAlbum;

        if (mTvArtist != null) {
            if (artist.isEmpty() || !mSettings.showArtist) {
                mTvArtist.setVisibility(View.GONE);
            } else {
                mTvArtist.setVisibility(View.VISIBLE);
                mTvArtist.setText(artist);
            }
        }

        if (mTvSingleLine != null) {
            if (artist.isEmpty() || !mSettings.showArtist) {
                mTvSingleLine.setText(safeTitle);
            } else {
                mTvSingleLine.setText(safeTitle.isEmpty() ? artist : (safeTitle + " • " + artist));
            }
        }
    }

    private void animateDropletDetach() {
        if (mBgContainer != null) {
            mBgContainer.setPivotX(mBgContainer.getWidth() / 2f);
            mBgContainer.setPivotY(0f);
            // 1. Surface tension pull downward as droplet forms
            mBgContainer.animate().setListener(null)
                    .translationY(convertDpToPx(3))
                    .scaleY(1.06f)
                    .scaleX(0.97f)
                    .setDuration(130)
                    .setInterpolator(M3Motion.EASE_OUT)
                    .withEndAction(() -> {
                        // 2. Elastic capillary snap recoil when droplet detaches
                        if (mBgContainer != null) {
                            mBgContainer.animate().setListener(null)
                                    .translationY(0f)
                                    .scaleY(1.0f)
                                    .scaleX(1.0f)
                                    .setDuration(240)
                                    .setInterpolator(M3Motion.SPRING_SPATIAL)
                                    .start();
                        }
                    }).start();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (mPauseTimeoutScheduled) {
                    schedulePauseTimeout(true);
                }
                if (v.getParent() != null) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                }
                mStartX = event.getRawX();
                mStartY = event.getRawY();
                mIsLongPressed = false;
                mHandler.postDelayed(mLongPressRunnable, 260);

                // Emil Kowalski tactile button press: scale(0.97) on press
                if (mBgContainer != null) {
                    mBgContainer.animate().setListener(null)
                            .scaleX(0.97f).scaleY(0.97f)
                            .setDuration(M3Motion.DURATION_SHORT2)
                            .setInterpolator(M3Motion.EASE_OUT)
                            .start();
                }

                return true;

            case MotionEvent.ACTION_MOVE:
                float diffX = event.getRawX() - mStartX;
                float diffY = event.getRawY() - mStartY;
                if (Math.hypot(diffX, diffY) > convertDpToPx(14)) {
                    mHandler.removeCallbacks(mLongPressRunnable);
                }
                return true;

            case MotionEvent.ACTION_UP:
                mHandler.removeCallbacks(mLongPressRunnable);

                // Tactile button release: spring back to 1.0
                if (mBgContainer != null) {
                    mBgContainer.animate().setListener(null)
                            .scaleX(1.0f).scaleY(1.0f)
                            .setDuration(M3Motion.DURATION_SHORT3)
                            .setInterpolator(M3Motion.SPRING_SPATIAL)
                            .start();
                }

                if (!mIsLongPressed) {
                    float totalDeltaX = event.getRawX() - mStartX;
                    float totalDeltaY = event.getRawY() - mStartY;

                    if (mSettings.swipeGestures && Math.abs(totalDeltaX) > convertDpToPx(24) && Math.abs(totalDeltaX) > Math.abs(totalDeltaY)) {
                        if (mSettings.hapticEnabled) doHaptic(HapticFeedbackConstants.VIRTUAL_KEY);
                        animateSwipeTransition(true);
                        mSessionController.next();
                    } else if (Math.hypot(totalDeltaX, totalDeltaY) < convertDpToPx(16)) {
                        if (mSettings.hapticEnabled) doHaptic(HapticFeedbackConstants.KEYBOARD_TAP);
                        mSessionController.playPause();
                        if (mCurrentState != null && mCurrentState.isPlaying) {
                            schedulePauseTimeout(true);
                        } else {
                            mIsTimeoutHidden = false;
                            cancelPauseTimeout();
                        }
                    }
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                mHandler.removeCallbacks(mLongPressRunnable);
                if (mBgContainer != null) {
                    mBgContainer.animate().setListener(null)
                            .scaleX(1.0f).scaleY(1.0f)
                            .setDuration(M3Motion.DURATION_SHORT3)
                            .setInterpolator(M3Motion.SPRING_SPATIAL)
                            .start();
                }
                return true;
        }
        return false;
    }

    private void animateTapPulse() {
    }

    private void animateSwipeTransition(boolean isNext) {
        View activeText = (mSettings.layoutMode == ChipSettings.MODE_ONE_LINE) ? mTvSingleLine : mTextContainer;
        if (activeText != null && activeText.getVisibility() == View.VISIBLE) {
            float outDist = isNext ? -convertDpToPx(18) : convertDpToPx(18);
            float inDist = isNext ? convertDpToPx(18) : -convertDpToPx(18);

            activeText.animate().setListener(null)
                    .translationX(outDist).alpha(0f).setDuration(85)
                    .withEndAction(() -> {
                        if (activeText != null) {
                            activeText.setTranslationX(inDist);
                            activeText.animate().setListener(null)
                                    .translationX(0f).alpha(1f).setDuration(150)
                                    .setInterpolator(new DecelerateInterpolator())
                                    .start();
                        }
                    }).start();
        }
    }

    private void doHaptic(int feedbackConstant) {
        try {
            if (mChipRootView != null) {
                mChipRootView.performHapticFeedback(feedbackConstant, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            }
        } catch (Throwable t) {
            if (mVibrator != null && mVibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mVibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    mVibrator.vibrate(45);
                }
            }
        }
    }

    @Override
    public void onMediaUpdated(MediaSessionController.MediaState state) {
        this.mCurrentState = state;
        if (mChipRootView == null) return;

        boolean hasTitle = state != null && state.title != null && !state.title.isEmpty();
        boolean isPlaying = state != null && state.isPlaying;

        if (state == null || (!hasTitle && !isPlaying)) {
            cancelPauseTimeout();
            mIsTimeoutHidden = true;
            if (mMiniEqualizer != null) {
                mMiniEqualizer.setActive(false);
            }
            AudioVisualizerController.get().setPlaying(false);
            mChipRootView.setVisibility(View.GONE);
            updateNotificationIconsVisibility();
            return;
        }

        if (!state.isPlaying) {
            schedulePauseTimeout(false);
            if (mIsTimeoutHidden) {
                if (mMiniEqualizer != null) {
                    mMiniEqualizer.setActive(false);
                }
                AudioVisualizerController.get().setPlaying(false);
                mChipRootView.setVisibility(View.GONE);
                updateNotificationIconsVisibility();
                return;
            }
        } else {
            mIsTimeoutHidden = false;
            cancelPauseTimeout();
        }

        mLastTrackTitle = (state.title != null) ? state.title : "";

        checkNativeChipsAndAdapt();
        updateTextContent(state);
        updateLayoutMode();

        AudioVisualizerController.get().setPlaying(state.isPlaying);
        AudioVisualizerController.get().setSensitivity(mSettings.visualizerSensitivity / 100.0f);

        boolean showMiniEqualizer = state.isPlaying && mSettings.visualizerChipEnabled;
        if (mMiniEqualizer != null) {
            mMiniEqualizer.setColor(Color.WHITE);
            mMiniEqualizer.setVisibility(showMiniEqualizer ? View.VISIBLE : View.GONE);
            mMiniEqualizer.setActive(showMiniEqualizer);
        }

        if (mIvIcon != null) {
            mIvIcon.setVisibility(showMiniEqualizer ? View.GONE : View.VISIBLE);
            if (state.albumArt != null && !state.albumArt.isRecycled()) {
                mIvIcon.setImageTintList(null);
                mIvIcon.setImageTintMode(null);
                mIvIcon.clearColorFilter();
                mIvIcon.setImageDrawable(new BitmapDrawable(mContext.getResources(), getCircularOrRoundedBitmap(state.albumArt)));
            } else {
                mIvIcon.setImageDrawable(state.isPlaying ?
                        PixelVectorIcons.getPauseIcon(mContext, Color.WHITE, convertDpToPx(16)) :
                        PixelVectorIcons.getPlayIcon(mContext, Color.WHITE, convertDpToPx(16)));
            }
        }

        if (mBgContainer != null) {
            Drawable bg = mBgContainer.getBackground();
            if (bg != null) {
                if (mSettings.monetTint) {
                    bg.mutate().setTint(adjustMonetColorForChip(state.monetColor));
                } else {
                    bg.mutate().setTintList(null);
                    if (bg instanceof GradientDrawable) {
                        ((GradientDrawable) bg).setColor(Color.parseColor("#FF141518"));
                    }
                }
            }
        }

        if (mMediaPopup.isShowing()) {
            mMediaPopup.updateState(state);
        }
    }

    private int adjustMonetColorForChip(int color) {
        float[] hsv = new float[3];
        android.graphics.Color.colorToHSV(color, hsv);
        hsv[1] = Math.min(0.65f, hsv[1]);
        hsv[2] = Math.max(0.22f, Math.min(hsv[2], 0.45f));
        return android.graphics.Color.HSVToColor(0xEE, hsv);
    }

    private Bitmap getCircularOrRoundedBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) return null;
        try {
            int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
            Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Rect rect = new Rect(0, 0, size, size);
            RectF rectF = new RectF(rect);
            float roundPx = size * 0.35f;

            canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(bitmap, null, rect, paint);
            return output;
        } catch (Throwable t) {
            return bitmap;
        }
    }

    private int convertDpToPx(int dp) {
        return Math.round(dp * mContext.getResources().getDisplayMetrics().density);
    }
}
