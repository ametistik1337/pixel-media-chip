package com.pixel.mediachip;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

public class MarqueeTextView extends TextView {
    private boolean mMarqueeEnabled = true;

    public MarqueeTextView(Context context) {
        super(context);
        init();
    }

    public MarqueeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MarqueeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setSingleLine(true);
        setHorizontallyScrolling(true);
        if (mMarqueeEnabled) {
            setEllipsize(TextUtils.TruncateAt.MARQUEE);
            setMarqueeRepeatLimit(-1);
            setSelected(true);
        } else {
            setEllipsize(TextUtils.TruncateAt.END);
        }
    }

    public void setMarqueeEnabled(boolean enable) {
        this.mMarqueeEnabled = enable;
        if (enable) {
            setSingleLine(true);
            setHorizontallyScrolling(true);
            setEllipsize(TextUtils.TruncateAt.MARQUEE);
            setMarqueeRepeatLimit(-1);
            setSelected(true);
        } else {
            setEllipsize(TextUtils.TruncateAt.END);
            setSelected(false);
        }
        invalidate();
    }

    @Override
    public boolean isFocused() {
        return mMarqueeEnabled || super.isFocused();
    }

    @Override
    public boolean isSelected() {
        return mMarqueeEnabled || super.isSelected();
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        if (mMarqueeEnabled) {
            super.onFocusChanged(true, direction, previouslyFocusedRect);
        } else {
            super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (mMarqueeEnabled) {
            super.onWindowFocusChanged(true);
        } else {
            super.onWindowFocusChanged(hasWindowFocus);
        }
    }
}
