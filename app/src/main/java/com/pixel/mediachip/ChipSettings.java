package com.pixel.mediachip;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class ChipSettings {
    private static final String TAG = "PixelMediaChip_Settings";
    public static final String PREFS_NAME = "pixel_media_chip_prefs";
    public static final String ACTION_SETTINGS_CHANGED = "com.pixel.mediachip.ACTION_SETTINGS_CHANGED";
    public static final Uri PROVIDER_URI = Uri.parse("content://com.pixel.mediachip.provider");

    // Layout Modes
    public static final int MODE_TWO_LINES = 0;
    public static final int MODE_ONE_LINE = 1;
    public static final int MODE_ICON_ONLY = 2;

    // Squish Modes on Screen Recording / Call
    public static final int SQUISH_TO_ICON = 0;
    public static final int SQUISH_HIDE = 1;
    public static final int SQUISH_DISABLED = 2;

    // Keys
    public static final String KEY_LAYOUT_MODE = "layout_mode";
    public static final String KEY_SQUISH_MODE = "squish_mode";
    public static final String KEY_SHOW_ARTIST = "show_artist";
    public static final String KEY_MARQUEE_ONE_LINE = "marquee_one_line";
    public static final String KEY_MONET_TINT = "monet_tint";
    public static final String KEY_SWIPE_GESTURES = "swipe_gestures";
    public static final String KEY_HAPTIC_ENABLED = "haptic_enabled";
    public static final String KEY_MAX_WIDTH_DP = "max_width_dp";
    public static final String KEY_TITLE_SIZE_SP = "title_size_sp";
    public static final String KEY_ARTIST_SIZE_SP = "artist_size_sp";
    public static final String KEY_CORNER_RADIUS_DP = "corner_radius_dp";
    public static final String KEY_EAT_CAMERA = "eat_camera";
    public static final String KEY_MAX_TEXT_WIDTH_DP = "max_text_width_dp";
    public static final String KEY_CAMERA_EAT_DEPTH_DP = "camera_eat_depth_dp";
    public static final String KEY_VISUALIZER_CARD_ENABLED = "visualizer_card_enabled";
    public static final String KEY_VISUALIZER_CHIP_ENABLED = "visualizer_chip_enabled";
    public static final String KEY_VISUALIZER_SENSITIVITY = "visualizer_sensitivity";

    // Default Values
    public static final int DEFAULT_LAYOUT_MODE = MODE_TWO_LINES;
    public static final int DEFAULT_SQUISH_MODE = SQUISH_TO_ICON;
    public static final boolean DEFAULT_SHOW_ARTIST = true;
    public static final boolean DEFAULT_MARQUEE_ONE_LINE = true;
    public static final boolean DEFAULT_MONET_TINT = true;
    public static final boolean DEFAULT_SWIPE_GESTURES = true;
    public static final boolean DEFAULT_HAPTIC_ENABLED = true;
    public static final boolean DEFAULT_EAT_CAMERA = false;
    public static final boolean DEFAULT_VISUALIZER_CARD_ENABLED = true;
    public static final boolean DEFAULT_VISUALIZER_CHIP_ENABLED = true;
    public static final int DEFAULT_VISUALIZER_SENSITIVITY = 100;
    public static final int DEFAULT_MAX_WIDTH_DP = 105;
    public static final int DEFAULT_MAX_TEXT_WIDTH_DP = 105;
    public static final int DEFAULT_CAMERA_EAT_DEPTH_DP = 16;
    public static final float DEFAULT_TITLE_SIZE_SP = 10.0f;
    public static final float DEFAULT_ARTIST_SIZE_SP = 8.0f;
    public static final int DEFAULT_CORNER_RADIUS_DP = 14;

    // Values
    public int layoutMode = DEFAULT_LAYOUT_MODE;
    public int squishMode = DEFAULT_SQUISH_MODE;
    public boolean showArtist = DEFAULT_SHOW_ARTIST;
    public boolean marqueeOneLine = DEFAULT_MARQUEE_ONE_LINE;
    public boolean monetTint = DEFAULT_MONET_TINT;
    public boolean swipeGestures = DEFAULT_SWIPE_GESTURES;
    public boolean hapticEnabled = DEFAULT_HAPTIC_ENABLED;
    public boolean eatCamera = DEFAULT_EAT_CAMERA;
    public boolean visualizerCardEnabled = DEFAULT_VISUALIZER_CARD_ENABLED;
    public boolean visualizerChipEnabled = DEFAULT_VISUALIZER_CHIP_ENABLED;
    public int visualizerSensitivity = DEFAULT_VISUALIZER_SENSITIVITY;
    public int maxWidthDp = DEFAULT_MAX_WIDTH_DP;
    public int maxTextWidthDp = DEFAULT_MAX_TEXT_WIDTH_DP;
    public int cameraEatDepthDp = DEFAULT_CAMERA_EAT_DEPTH_DP;
    public float titleSizeSp = DEFAULT_TITLE_SIZE_SP;
    public float artistSizeSp = DEFAULT_ARTIST_SIZE_SP;
    public int cornerRadiusDp = DEFAULT_CORNER_RADIUS_DP;

    private static ChipSettings sInstance;

    public static synchronized ChipSettings get() {
        if (sInstance == null) {
            sInstance = new ChipSettings();
        }
        return sInstance;
    }

    public void resetToDefaults() {
        layoutMode = DEFAULT_LAYOUT_MODE;
        squishMode = DEFAULT_SQUISH_MODE;
        showArtist = DEFAULT_SHOW_ARTIST;
        marqueeOneLine = DEFAULT_MARQUEE_ONE_LINE;
        monetTint = DEFAULT_MONET_TINT;
        swipeGestures = DEFAULT_SWIPE_GESTURES;
        hapticEnabled = DEFAULT_HAPTIC_ENABLED;
        eatCamera = DEFAULT_EAT_CAMERA;
        visualizerCardEnabled = DEFAULT_VISUALIZER_CARD_ENABLED;
        visualizerChipEnabled = DEFAULT_VISUALIZER_CHIP_ENABLED;
        visualizerSensitivity = DEFAULT_VISUALIZER_SENSITIVITY;
        maxWidthDp = DEFAULT_MAX_WIDTH_DP;
        maxTextWidthDp = DEFAULT_MAX_TEXT_WIDTH_DP;
        cameraEatDepthDp = DEFAULT_CAMERA_EAT_DEPTH_DP;
        titleSizeSp = DEFAULT_TITLE_SIZE_SP;
        artistSizeSp = DEFAULT_ARTIST_SIZE_SP;
        cornerRadiusDp = DEFAULT_CORNER_RADIUS_DP;
    }

    public void applyPresetPixelDefault() {
        resetToDefaults();
    }

    public void applyPresetDynamicIsland() {
        layoutMode = MODE_ONE_LINE;
        eatCamera = true;
        maxTextWidthDp = 125;
        cameraEatDepthDp = 20;
        showArtist = true;
        marqueeOneLine = true;
        cornerRadiusDp = 16;
        titleSizeSp = 10.5f;
    }

    public void applyPresetCompact() {
        layoutMode = MODE_ICON_ONLY;
        eatCamera = false;
        cornerRadiusDp = 14;
        squishMode = SQUISH_DISABLED;
    }

    public void applyPresetExpanded() {
        layoutMode = MODE_TWO_LINES;
        eatCamera = false;
        maxWidthDp = 180;
        showArtist = true;
        marqueeOneLine = true;
        cornerRadiusDp = 16;
        titleSizeSp = 11.0f;
        artistSizeSp = 8.5f;
    }

    private static int getSafeInt(SharedPreferences prefs, String key, int def) {
        try {
            return prefs.getInt(key, def);
        } catch (ClassCastException e) {
            try {
                float f = prefs.getFloat(key, (float) def);
                return Math.round(f);
            } catch (Throwable t) {
                return def;
            }
        }
    }

    private static float getSafeFloat(SharedPreferences prefs, String key, float def) {
        try {
            return prefs.getFloat(key, def);
        } catch (ClassCastException e) {
            try {
                int i = prefs.getInt(key, Math.round(def));
                return (float) i;
            } catch (Throwable t) {
                return def;
            }
        }
    }

    private static boolean getSafeBoolean(SharedPreferences prefs, String key, boolean def) {
        try {
            return prefs.getBoolean(key, def);
        } catch (ClassCastException e) {
            return def;
        }
    }

    public void loadFromPrefs(SharedPreferences prefs) {
        if (prefs == null) return;
        layoutMode = getSafeInt(prefs, KEY_LAYOUT_MODE, DEFAULT_LAYOUT_MODE);
        squishMode = getSafeInt(prefs, KEY_SQUISH_MODE, DEFAULT_SQUISH_MODE);
        showArtist = getSafeBoolean(prefs, KEY_SHOW_ARTIST, DEFAULT_SHOW_ARTIST);
        marqueeOneLine = getSafeBoolean(prefs, KEY_MARQUEE_ONE_LINE, DEFAULT_MARQUEE_ONE_LINE);
        monetTint = getSafeBoolean(prefs, KEY_MONET_TINT, DEFAULT_MONET_TINT);
        swipeGestures = getSafeBoolean(prefs, KEY_SWIPE_GESTURES, DEFAULT_SWIPE_GESTURES);
        hapticEnabled = getSafeBoolean(prefs, KEY_HAPTIC_ENABLED, DEFAULT_HAPTIC_ENABLED);
        maxWidthDp = getSafeInt(prefs, KEY_MAX_WIDTH_DP, DEFAULT_MAX_WIDTH_DP);
        maxTextWidthDp = getSafeInt(prefs, KEY_MAX_TEXT_WIDTH_DP, maxWidthDp);
        cameraEatDepthDp = getSafeInt(prefs, KEY_CAMERA_EAT_DEPTH_DP, DEFAULT_CAMERA_EAT_DEPTH_DP);
        titleSizeSp = getSafeFloat(prefs, KEY_TITLE_SIZE_SP, DEFAULT_TITLE_SIZE_SP);
        artistSizeSp = getSafeFloat(prefs, KEY_ARTIST_SIZE_SP, DEFAULT_ARTIST_SIZE_SP);
        cornerRadiusDp = getSafeInt(prefs, KEY_CORNER_RADIUS_DP, DEFAULT_CORNER_RADIUS_DP);
        eatCamera = getSafeBoolean(prefs, KEY_EAT_CAMERA, DEFAULT_EAT_CAMERA);
        visualizerCardEnabled = getSafeBoolean(prefs, KEY_VISUALIZER_CARD_ENABLED, DEFAULT_VISUALIZER_CARD_ENABLED);
        visualizerChipEnabled = getSafeBoolean(prefs, KEY_VISUALIZER_CHIP_ENABLED, DEFAULT_VISUALIZER_CHIP_ENABLED);
        visualizerSensitivity = getSafeInt(prefs, KEY_VISUALIZER_SENSITIVITY, DEFAULT_VISUALIZER_SENSITIVITY);
    }

    public void saveToLocalPrefs(Context context) {
        if (context == null) return;
        try {
            SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
            editor.putInt(KEY_LAYOUT_MODE, layoutMode);
            editor.putInt(KEY_SQUISH_MODE, squishMode);
            editor.putBoolean(KEY_SHOW_ARTIST, showArtist);
            editor.putBoolean(KEY_MARQUEE_ONE_LINE, marqueeOneLine);
            editor.putBoolean(KEY_MONET_TINT, monetTint);
            editor.putBoolean(KEY_SWIPE_GESTURES, swipeGestures);
            editor.putBoolean(KEY_HAPTIC_ENABLED, hapticEnabled);
            editor.putBoolean(KEY_EAT_CAMERA, eatCamera);
            editor.putBoolean(KEY_VISUALIZER_CARD_ENABLED, visualizerCardEnabled);
            editor.putBoolean(KEY_VISUALIZER_CHIP_ENABLED, visualizerChipEnabled);
            editor.putInt(KEY_VISUALIZER_SENSITIVITY, visualizerSensitivity);
            editor.putInt(KEY_MAX_WIDTH_DP, maxWidthDp);
            editor.putInt(KEY_MAX_TEXT_WIDTH_DP, maxTextWidthDp);
            editor.putInt(KEY_CAMERA_EAT_DEPTH_DP, cameraEatDepthDp);
            editor.putFloat(KEY_TITLE_SIZE_SP, titleSizeSp);
            editor.putFloat(KEY_ARTIST_SIZE_SP, artistSizeSp);
            editor.putInt(KEY_CORNER_RADIUS_DP, cornerRadiusDp);
            editor.apply();
        } catch (Throwable ignored) {}
    }

    public void load(Context context) {
        if (context == null) return;
        boolean loaded = false;

        // 1. Try ContentProvider
        try {
            Bundle bundle = context.getContentResolver().call(PROVIDER_URI, "getSettings", null, null);
            if (bundle != null && !bundle.isEmpty()) {
                fromBundle(bundle);
                loaded = true;
                saveToLocalPrefs(context);
                Log.i(TAG, "Loaded settings from provider: maxWidthDp=" + maxWidthDp);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Could not load settings via ContentProvider: " + t.getMessage());
        }

        // 2. Try createPackageContext for com.pixel.mediachip (cross-process preferences lookup)
        if (!loaded && !context.getPackageName().equals("com.pixel.mediachip")) {
            try {
                Context appContext = context.createPackageContext("com.pixel.mediachip", Context.CONTEXT_IGNORE_SECURITY);
                SharedPreferences appPrefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                if (appPrefs != null && appPrefs.contains(KEY_VISUALIZER_CHIP_ENABLED)) {
                    loadFromPrefs(appPrefs);
                    loaded = true;
                    saveToLocalPrefs(context);
                }
            } catch (Throwable ignored) {}
        }

        // 3. Fallback to local SharedPreferences
        if (!loaded) {
            try {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                loadFromPrefs(prefs);
            } catch (Throwable ignored) {}
        }
    }

    public void save(Context context) {
        if (context == null) return;
        try {
            SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
            editor.putInt(KEY_LAYOUT_MODE, layoutMode);
            editor.putInt(KEY_SQUISH_MODE, squishMode);
            editor.putBoolean(KEY_SHOW_ARTIST, showArtist);
            editor.putBoolean(KEY_MARQUEE_ONE_LINE, marqueeOneLine);
            editor.putBoolean(KEY_MONET_TINT, monetTint);
            editor.putBoolean(KEY_SWIPE_GESTURES, swipeGestures);
            editor.putBoolean(KEY_HAPTIC_ENABLED, hapticEnabled);
            editor.putBoolean(KEY_EAT_CAMERA, eatCamera);
            editor.putBoolean(KEY_VISUALIZER_CARD_ENABLED, visualizerCardEnabled);
            editor.putBoolean(KEY_VISUALIZER_CHIP_ENABLED, visualizerChipEnabled);
            editor.putInt(KEY_VISUALIZER_SENSITIVITY, visualizerSensitivity);
            editor.putInt(KEY_MAX_WIDTH_DP, maxWidthDp);
            editor.putInt(KEY_MAX_TEXT_WIDTH_DP, maxTextWidthDp);
            editor.putInt(KEY_CAMERA_EAT_DEPTH_DP, cameraEatDepthDp);
            editor.putFloat(KEY_TITLE_SIZE_SP, titleSizeSp);
            editor.putFloat(KEY_ARTIST_SIZE_SP, artistSizeSp);
            editor.putInt(KEY_CORNER_RADIUS_DP, cornerRadiusDp);
            editor.apply();
        } catch (Throwable ignored) {}

        try {
            context.getContentResolver().call(PROVIDER_URI, "saveSettings", null, toBundle());
        } catch (Throwable ignored) {}

        sendBroadcast(context);
    }

    public Bundle toBundle() {
        Bundle b = new Bundle();
        b.putInt(KEY_LAYOUT_MODE, layoutMode);
        b.putInt(KEY_SQUISH_MODE, squishMode);
        b.putBoolean(KEY_SHOW_ARTIST, showArtist);
        b.putBoolean(KEY_MARQUEE_ONE_LINE, marqueeOneLine);
        b.putBoolean(KEY_MONET_TINT, monetTint);
        b.putBoolean(KEY_SWIPE_GESTURES, swipeGestures);
        b.putBoolean(KEY_HAPTIC_ENABLED, hapticEnabled);
        b.putBoolean(KEY_EAT_CAMERA, eatCamera);
        b.putBoolean(KEY_VISUALIZER_CARD_ENABLED, visualizerCardEnabled);
        b.putBoolean(KEY_VISUALIZER_CHIP_ENABLED, visualizerChipEnabled);
        b.putInt(KEY_VISUALIZER_SENSITIVITY, visualizerSensitivity);
        b.putInt(KEY_MAX_WIDTH_DP, maxWidthDp);
        b.putInt(KEY_MAX_TEXT_WIDTH_DP, maxTextWidthDp);
        b.putInt(KEY_CAMERA_EAT_DEPTH_DP, cameraEatDepthDp);
        b.putFloat(KEY_TITLE_SIZE_SP, titleSizeSp);
        b.putFloat(KEY_ARTIST_SIZE_SP, artistSizeSp);
        b.putInt(KEY_CORNER_RADIUS_DP, cornerRadiusDp);
        return b;
    }

    public void sendBroadcast(Context context) {
        if (context == null) return;
        try {
            Intent intent = new Intent(ACTION_SETTINGS_CHANGED);
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            intent.putExtras(toBundle());
            context.sendBroadcast(intent);

            // Also send targeted to SystemUI
            Intent sysUiIntent = new Intent(ACTION_SETTINGS_CHANGED);
            sysUiIntent.setPackage("com.android.systemui");
            sysUiIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            sysUiIntent.putExtras(toBundle());
            context.sendBroadcast(sysUiIntent);
        } catch (Throwable ignored) {}
    }

    public void fromBundle(Bundle b) {
        if (b == null) return;
        if (b.containsKey(KEY_LAYOUT_MODE)) {
            Object v = b.get(KEY_LAYOUT_MODE);
            if (v instanceof Number) layoutMode = ((Number) v).intValue();
            else layoutMode = b.getInt(KEY_LAYOUT_MODE, layoutMode);
        }
        if (b.containsKey(KEY_SQUISH_MODE)) {
            Object v = b.get(KEY_SQUISH_MODE);
            if (v instanceof Number) squishMode = ((Number) v).intValue();
            else squishMode = b.getInt(KEY_SQUISH_MODE, squishMode);
        }
        if (b.containsKey(KEY_SHOW_ARTIST)) {
            Object v = b.get(KEY_SHOW_ARTIST);
            if (v instanceof Boolean) showArtist = (Boolean) v;
            else showArtist = b.getBoolean(KEY_SHOW_ARTIST, showArtist);
        }
        if (b.containsKey(KEY_MARQUEE_ONE_LINE)) {
            Object v = b.get(KEY_MARQUEE_ONE_LINE);
            if (v instanceof Boolean) marqueeOneLine = (Boolean) v;
            else marqueeOneLine = b.getBoolean(KEY_MARQUEE_ONE_LINE, marqueeOneLine);
        }
        if (b.containsKey(KEY_MONET_TINT)) {
            Object v = b.get(KEY_MONET_TINT);
            if (v instanceof Boolean) monetTint = (Boolean) v;
            else monetTint = b.getBoolean(KEY_MONET_TINT, monetTint);
        }
        if (b.containsKey(KEY_SWIPE_GESTURES)) {
            Object v = b.get(KEY_SWIPE_GESTURES);
            if (v instanceof Boolean) swipeGestures = (Boolean) v;
            else swipeGestures = b.getBoolean(KEY_SWIPE_GESTURES, swipeGestures);
        }
        if (b.containsKey(KEY_HAPTIC_ENABLED)) {
            Object v = b.get(KEY_HAPTIC_ENABLED);
            if (v instanceof Boolean) hapticEnabled = (Boolean) v;
            else hapticEnabled = b.getBoolean(KEY_HAPTIC_ENABLED, hapticEnabled);
        }
        if (b.containsKey(KEY_EAT_CAMERA)) {
            Object v = b.get(KEY_EAT_CAMERA);
            if (v instanceof Boolean) eatCamera = (Boolean) v;
            else eatCamera = b.getBoolean(KEY_EAT_CAMERA, eatCamera);
        }
        if (b.containsKey(KEY_VISUALIZER_CARD_ENABLED)) {
            Object v = b.get(KEY_VISUALIZER_CARD_ENABLED);
            if (v instanceof Boolean) visualizerCardEnabled = (Boolean) v;
            else visualizerCardEnabled = b.getBoolean(KEY_VISUALIZER_CARD_ENABLED, visualizerCardEnabled);
        }
        if (b.containsKey(KEY_VISUALIZER_CHIP_ENABLED)) {
            Object v = b.get(KEY_VISUALIZER_CHIP_ENABLED);
            if (v instanceof Boolean) visualizerChipEnabled = (Boolean) v;
            else visualizerChipEnabled = b.getBoolean(KEY_VISUALIZER_CHIP_ENABLED, visualizerChipEnabled);
        }
        if (b.containsKey(KEY_VISUALIZER_SENSITIVITY)) {
            Object v = b.get(KEY_VISUALIZER_SENSITIVITY);
            if (v instanceof Number) visualizerSensitivity = ((Number) v).intValue();
            else visualizerSensitivity = b.getInt(KEY_VISUALIZER_SENSITIVITY, visualizerSensitivity);
        }
        if (b.containsKey(KEY_MAX_WIDTH_DP)) {
            Object v = b.get(KEY_MAX_WIDTH_DP);
            if (v instanceof Number) maxWidthDp = ((Number) v).intValue();
            else maxWidthDp = b.getInt(KEY_MAX_WIDTH_DP, maxWidthDp);
        }
        if (b.containsKey(KEY_MAX_TEXT_WIDTH_DP)) {
            Object v = b.get(KEY_MAX_TEXT_WIDTH_DP);
            if (v instanceof Number) maxTextWidthDp = ((Number) v).intValue();
            else maxTextWidthDp = b.getInt(KEY_MAX_TEXT_WIDTH_DP, maxTextWidthDp);
        }
        if (b.containsKey(KEY_CAMERA_EAT_DEPTH_DP)) {
            Object v = b.get(KEY_CAMERA_EAT_DEPTH_DP);
            if (v instanceof Number) cameraEatDepthDp = ((Number) v).intValue();
            else cameraEatDepthDp = b.getInt(KEY_CAMERA_EAT_DEPTH_DP, cameraEatDepthDp);
        }
        if (b.containsKey(KEY_TITLE_SIZE_SP)) {
            Object v = b.get(KEY_TITLE_SIZE_SP);
            if (v instanceof Number) titleSizeSp = ((Number) v).floatValue();
            else titleSizeSp = b.getFloat(KEY_TITLE_SIZE_SP, titleSizeSp);
        }
        if (b.containsKey(KEY_ARTIST_SIZE_SP)) {
            Object v = b.get(KEY_ARTIST_SIZE_SP);
            if (v instanceof Number) artistSizeSp = ((Number) v).floatValue();
            else artistSizeSp = b.getFloat(KEY_ARTIST_SIZE_SP, artistSizeSp);
        }
        if (b.containsKey(KEY_CORNER_RADIUS_DP)) {
            Object v = b.get(KEY_CORNER_RADIUS_DP);
            if (v instanceof Number) cornerRadiusDp = ((Number) v).intValue();
            else cornerRadiusDp = b.getInt(KEY_CORNER_RADIUS_DP, cornerRadiusDp);
        }
    }

    public void copyFrom(ChipSettings other) {
        if (other == null) return;
        this.layoutMode = other.layoutMode;
        this.squishMode = other.squishMode;
        this.showArtist = other.showArtist;
        this.marqueeOneLine = other.marqueeOneLine;
        this.monetTint = other.monetTint;
        this.swipeGestures = other.swipeGestures;
        this.hapticEnabled = other.hapticEnabled;
        this.eatCamera = other.eatCamera;
        this.visualizerCardEnabled = other.visualizerCardEnabled;
        this.visualizerChipEnabled = other.visualizerChipEnabled;
        this.visualizerSensitivity = other.visualizerSensitivity;
        this.maxWidthDp = other.maxWidthDp;
        this.maxTextWidthDp = other.maxTextWidthDp;
        this.cameraEatDepthDp = other.cameraEatDepthDp;
        this.titleSizeSp = other.titleSizeSp;
        this.artistSizeSp = other.artistSizeSp;
        this.cornerRadiusDp = other.cornerRadiusDp;
    }

    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put(KEY_LAYOUT_MODE, layoutMode);
        map.put(KEY_SQUISH_MODE, squishMode);
        map.put(KEY_SHOW_ARTIST, showArtist);
        map.put(KEY_MARQUEE_ONE_LINE, marqueeOneLine);
        map.put(KEY_MONET_TINT, monetTint);
        map.put(KEY_SWIPE_GESTURES, swipeGestures);
        map.put(KEY_HAPTIC_ENABLED, hapticEnabled);
        map.put(KEY_EAT_CAMERA, eatCamera);
        map.put(KEY_VISUALIZER_CARD_ENABLED, visualizerCardEnabled);
        map.put(KEY_VISUALIZER_CHIP_ENABLED, visualizerChipEnabled);
        map.put(KEY_VISUALIZER_SENSITIVITY, visualizerSensitivity);
        map.put(KEY_MAX_WIDTH_DP, maxWidthDp);
        map.put(KEY_MAX_TEXT_WIDTH_DP, maxTextWidthDp);
        map.put(KEY_CAMERA_EAT_DEPTH_DP, cameraEatDepthDp);
        map.put(KEY_TITLE_SIZE_SP, titleSizeSp);
        map.put(KEY_ARTIST_SIZE_SP, artistSizeSp);
        map.put(KEY_CORNER_RADIUS_DP, cornerRadiusDp);
        return map;
    }

    public void fromMap(java.util.Map<String, Object> map) {
        if (map == null) return;
        if (map.containsKey(KEY_LAYOUT_MODE)) {
            Object v = map.get(KEY_LAYOUT_MODE);
            if (v instanceof Number) layoutMode = ((Number) v).intValue();
        }
        if (map.containsKey(KEY_SQUISH_MODE)) {
            Object v = map.get(KEY_SQUISH_MODE);
            if (v instanceof Number) squishMode = ((Number) v).intValue();
        }
        if (map.containsKey(KEY_SHOW_ARTIST)) {
            Object v = map.get(KEY_SHOW_ARTIST);
            if (v instanceof Boolean) showArtist = (Boolean) v;
        }
        if (map.containsKey(KEY_MARQUEE_ONE_LINE)) {
            Object v = map.get(KEY_MARQUEE_ONE_LINE);
            if (v instanceof Boolean) marqueeOneLine = (Boolean) v;
        }
        if (map.containsKey(KEY_MONET_TINT)) {
            Object v = map.get(KEY_MONET_TINT);
            if (v instanceof Boolean) monetTint = (Boolean) v;
        }
        if (map.containsKey(KEY_SWIPE_GESTURES)) {
            Object v = map.get(KEY_SWIPE_GESTURES);
            if (v instanceof Boolean) swipeGestures = (Boolean) v;
        }
        if (map.containsKey(KEY_HAPTIC_ENABLED)) {
            Object v = map.get(KEY_HAPTIC_ENABLED);
            if (v instanceof Boolean) hapticEnabled = (Boolean) v;
        }
        if (map.containsKey(KEY_EAT_CAMERA)) {
            Object v = map.get(KEY_EAT_CAMERA);
            if (v instanceof Boolean) eatCamera = (Boolean) v;
        }
        if (map.containsKey(KEY_VISUALIZER_CARD_ENABLED)) {
            Object v = map.get(KEY_VISUALIZER_CARD_ENABLED);
            if (v instanceof Boolean) visualizerCardEnabled = (Boolean) v;
        }
        if (map.containsKey(KEY_VISUALIZER_CHIP_ENABLED)) {
            Object v = map.get(KEY_VISUALIZER_CHIP_ENABLED);
            if (v instanceof Boolean) visualizerChipEnabled = (Boolean) v;
        }
        if (map.containsKey(KEY_VISUALIZER_SENSITIVITY)) {
            Object v = map.get(KEY_VISUALIZER_SENSITIVITY);
            if (v instanceof Number) visualizerSensitivity = ((Number) v).intValue();
        }
        if (map.containsKey(KEY_MAX_WIDTH_DP)) {
            Object v = map.get(KEY_MAX_WIDTH_DP);
            if (v instanceof Number) maxWidthDp = ((Number) v).intValue();
        }
        if (map.containsKey(KEY_MAX_TEXT_WIDTH_DP)) {
            Object v = map.get(KEY_MAX_TEXT_WIDTH_DP);
            if (v instanceof Number) maxTextWidthDp = ((Number) v).intValue();
        }
        if (map.containsKey(KEY_CAMERA_EAT_DEPTH_DP)) {
            Object v = map.get(KEY_CAMERA_EAT_DEPTH_DP);
            if (v instanceof Number) cameraEatDepthDp = ((Number) v).intValue();
        }
        if (map.containsKey(KEY_TITLE_SIZE_SP)) {
            Object v = map.get(KEY_TITLE_SIZE_SP);
            if (v instanceof Number) titleSizeSp = ((Number) v).floatValue();
        }
        if (map.containsKey(KEY_ARTIST_SIZE_SP)) {
            Object v = map.get(KEY_ARTIST_SIZE_SP);
            if (v instanceof Number) artistSizeSp = ((Number) v).floatValue();
        }
        if (map.containsKey(KEY_CORNER_RADIUS_DP)) {
            Object v = map.get(KEY_CORNER_RADIUS_DP);
            if (v instanceof Number) cornerRadiusDp = ((Number) v).intValue();
        }
    }
}
