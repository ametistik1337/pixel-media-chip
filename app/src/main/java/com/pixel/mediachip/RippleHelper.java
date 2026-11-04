package com.pixel.mediachip;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageButton;

public class RippleHelper {
    public static void applySystemRipple(Context context, View view) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
        view.setBackgroundResource(outValue.resourceId);
        if (view instanceof ImageButton) {
            ((ImageButton) view).setStateListAnimator(null);
        }
    }
}
