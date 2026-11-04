package com.pixel.mediachip;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

public class SettingsProvider extends ContentProvider {
    public static final String AUTHORITY = "com.pixel.mediachip.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    public static final String METHOD_GET_SETTINGS = "getSettings";
    public static final String METHOD_SAVE_SETTINGS = "saveSettings";

    @Override
    public boolean onCreate() {
        return true;
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

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Context context = getContext();
        if (context == null) return null;

        SharedPreferences prefs = context.getSharedPreferences(ChipSettings.PREFS_NAME, Context.MODE_PRIVATE);

        if (METHOD_GET_SETTINGS.equals(method)) {
            Bundle bundle = new Bundle();
            bundle.putInt(ChipSettings.KEY_LAYOUT_MODE, getSafeInt(prefs, ChipSettings.KEY_LAYOUT_MODE, ChipSettings.DEFAULT_LAYOUT_MODE));
            bundle.putInt(ChipSettings.KEY_SQUISH_MODE, getSafeInt(prefs, ChipSettings.KEY_SQUISH_MODE, ChipSettings.DEFAULT_SQUISH_MODE));
            bundle.putBoolean(ChipSettings.KEY_SHOW_ARTIST, getSafeBoolean(prefs, ChipSettings.KEY_SHOW_ARTIST, ChipSettings.DEFAULT_SHOW_ARTIST));
            bundle.putBoolean(ChipSettings.KEY_MARQUEE_ONE_LINE, getSafeBoolean(prefs, ChipSettings.KEY_MARQUEE_ONE_LINE, ChipSettings.DEFAULT_MARQUEE_ONE_LINE));
            bundle.putBoolean(ChipSettings.KEY_MONET_TINT, getSafeBoolean(prefs, ChipSettings.KEY_MONET_TINT, ChipSettings.DEFAULT_MONET_TINT));
            bundle.putBoolean(ChipSettings.KEY_SWIPE_GESTURES, getSafeBoolean(prefs, ChipSettings.KEY_SWIPE_GESTURES, ChipSettings.DEFAULT_SWIPE_GESTURES));
            bundle.putBoolean(ChipSettings.KEY_HAPTIC_ENABLED, getSafeBoolean(prefs, ChipSettings.KEY_HAPTIC_ENABLED, ChipSettings.DEFAULT_HAPTIC_ENABLED));
            bundle.putBoolean(ChipSettings.KEY_EAT_CAMERA, getSafeBoolean(prefs, ChipSettings.KEY_EAT_CAMERA, ChipSettings.DEFAULT_EAT_CAMERA));
            bundle.putBoolean(ChipSettings.KEY_VISUALIZER_CARD_ENABLED, getSafeBoolean(prefs, ChipSettings.KEY_VISUALIZER_CARD_ENABLED, ChipSettings.DEFAULT_VISUALIZER_CARD_ENABLED));
            bundle.putBoolean(ChipSettings.KEY_VISUALIZER_CHIP_ENABLED, getSafeBoolean(prefs, ChipSettings.KEY_VISUALIZER_CHIP_ENABLED, ChipSettings.DEFAULT_VISUALIZER_CHIP_ENABLED));
            bundle.putInt(ChipSettings.KEY_VISUALIZER_SENSITIVITY, getSafeInt(prefs, ChipSettings.KEY_VISUALIZER_SENSITIVITY, ChipSettings.DEFAULT_VISUALIZER_SENSITIVITY));
            bundle.putInt(ChipSettings.KEY_MAX_WIDTH_DP, getSafeInt(prefs, ChipSettings.KEY_MAX_WIDTH_DP, ChipSettings.DEFAULT_MAX_WIDTH_DP));
            bundle.putInt(ChipSettings.KEY_MAX_TEXT_WIDTH_DP, getSafeInt(prefs, ChipSettings.KEY_MAX_TEXT_WIDTH_DP, ChipSettings.DEFAULT_MAX_TEXT_WIDTH_DP));
            bundle.putInt(ChipSettings.KEY_CAMERA_EAT_DEPTH_DP, getSafeInt(prefs, ChipSettings.KEY_CAMERA_EAT_DEPTH_DP, ChipSettings.DEFAULT_CAMERA_EAT_DEPTH_DP));
            bundle.putFloat(ChipSettings.KEY_TITLE_SIZE_SP, getSafeFloat(prefs, ChipSettings.KEY_TITLE_SIZE_SP, ChipSettings.DEFAULT_TITLE_SIZE_SP));
            bundle.putFloat(ChipSettings.KEY_ARTIST_SIZE_SP, getSafeFloat(prefs, ChipSettings.KEY_ARTIST_SIZE_SP, ChipSettings.DEFAULT_ARTIST_SIZE_SP));
            bundle.putInt(ChipSettings.KEY_CORNER_RADIUS_DP, getSafeInt(prefs, ChipSettings.KEY_CORNER_RADIUS_DP, ChipSettings.DEFAULT_CORNER_RADIUS_DP));
            return bundle;
        } else if (METHOD_SAVE_SETTINGS.equals(method) && extras != null) {
            SharedPreferences.Editor editor = prefs.edit();
            for (String key : extras.keySet()) {
                Object val = extras.get(key);
                if (val == null) continue;

                switch (key) {
                    // Float keys
                    case ChipSettings.KEY_TITLE_SIZE_SP:
                    case ChipSettings.KEY_ARTIST_SIZE_SP:
                        if (val instanceof Number) {
                            editor.putFloat(key, ((Number) val).floatValue());
                        }
                        break;

                    // Boolean keys
                    case ChipSettings.KEY_SHOW_ARTIST:
                    case ChipSettings.KEY_MARQUEE_ONE_LINE:
                    case ChipSettings.KEY_MONET_TINT:
                    case ChipSettings.KEY_SWIPE_GESTURES:
                    case ChipSettings.KEY_HAPTIC_ENABLED:
                    case ChipSettings.KEY_EAT_CAMERA:
                    case ChipSettings.KEY_VISUALIZER_CARD_ENABLED:
                    case ChipSettings.KEY_VISUALIZER_CHIP_ENABLED:
                        if (val instanceof Boolean) {
                            editor.putBoolean(key, (Boolean) val);
                        } else if (val instanceof String) {
                            editor.putBoolean(key, Boolean.parseBoolean((String) val));
                        }
                        break;

                    // Integer keys
                    case ChipSettings.KEY_LAYOUT_MODE:
                    case ChipSettings.KEY_SQUISH_MODE:
                    case ChipSettings.KEY_VISUALIZER_SENSITIVITY:
                    case ChipSettings.KEY_MAX_WIDTH_DP:
                    case ChipSettings.KEY_MAX_TEXT_WIDTH_DP:
                    case ChipSettings.KEY_CAMERA_EAT_DEPTH_DP:
                    case ChipSettings.KEY_CORNER_RADIUS_DP:
                        if (val instanceof Number) {
                            editor.putInt(key, ((Number) val).intValue());
                        }
                        break;

                    default:
                        if (val instanceof Integer) editor.putInt(key, (Integer) val);
                        else if (val instanceof Boolean) editor.putBoolean(key, (Boolean) val);
                        else if (val instanceof Float) editor.putFloat(key, (Float) val);
                        else if (val instanceof Number) editor.putInt(key, ((Number) val).intValue());
                        break;
                }
            }
            editor.apply();

            ChipSettings.get().fromBundle(extras);
            return new Bundle();
        }
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
