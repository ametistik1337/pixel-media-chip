package com.pixel.mediachip;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.palette.graphics.Palette;

import java.io.InputStream;
import java.util.List;

public class MediaSessionController {
    private static final String TAG = "PixelMediaChip_Session";

    public interface MediaListener {
        void onMediaUpdated(MediaState state);
    }

    public static class MediaState {
        public boolean isPlaying = false;
        public String title = "";
        public String artist = "";
        public String album = "";
        public String packageName = "";
        public Drawable appIcon = null;
        public Bitmap albumArt = null;
        public int monetColor = 0xFF8AB4F8; // Pixel blue default
        public long duration = 0;
        public long position = 0;
        public boolean isExplicit = false;
        public List<PlaybackState.CustomAction> customActions = null;
        public PlaybackState.CustomAction likeAction = null;
        public PlaybackState.CustomAction shuffleAction = null;
        public Drawable likeIcon = null;
        public Drawable shuffleIcon = null;
        public boolean isLiked = false;
        public boolean isShuffleOn = false;
        public MediaController controller = null;
    }

    private final Context mContext;
    private final MediaListener mListener;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private MediaSessionManager mSessionManager;
    private MediaController mActiveController;

    private final MediaController.Callback mMediaCallback = new MediaController.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            updateMediaState();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            updateMediaState();
        }

        @Override
        public void onSessionDestroyed() {
            mActiveController = null;
            findActiveSession();
        }
    };

    public MediaSessionController(Context context, MediaListener listener) {
        this.mContext = context;
        this.mListener = listener;
        initSessionManager();
    }

    private void initSessionManager() {
        try {
            mSessionManager = (MediaSessionManager) mContext.getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (mSessionManager != null) {
                mSessionManager.addOnActiveSessionsChangedListener(
                        controllers -> findActiveSession(),
                        null
                );
                findActiveSession();
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error initializing MediaSessionManager: ", t);
        }
    }

    public synchronized void findActiveSession() {
        if (mSessionManager == null) return;
        try {
            List<MediaController> controllers = mSessionManager.getActiveSessions(null);
            MediaController selected = null;
            for (MediaController mc : controllers) {
                PlaybackState state = mc.getPlaybackState();
                if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                    selected = mc;
                    break;
                }
            }
            if (selected == null && !controllers.isEmpty()) {
                selected = controllers.get(0);
            }

            if (selected != mActiveController) {
                if (mActiveController != null) {
                    try {
                        mActiveController.unregisterCallback(mMediaCallback);
                    } catch (Throwable ignored) {}
                }
                mActiveController = selected;
                if (mActiveController != null) {
                    try {
                        mActiveController.registerCallback(mMediaCallback, mHandler);
                    } catch (Throwable ignored) {}
                }
            }
            updateMediaState();
        } catch (Throwable t) {
            Log.e(TAG, "Error finding active session: ", t);
        }
    }

    public void updateMediaState() {
        if (mActiveController == null) {
            MediaState emptyState = new MediaState();
            mHandler.post(() -> mListener.onMediaUpdated(emptyState));
            return;
        }

        MediaState state = new MediaState();
        state.controller = mActiveController;
        state.packageName = mActiveController.getPackageName();

        try {
            PackageManager pm = mContext.getPackageManager();
            state.appIcon = pm.getApplicationIcon(state.packageName);
        } catch (Throwable ignored) {}

        PlaybackState pbState = mActiveController.getPlaybackState();
        if (pbState != null) {
            state.isPlaying = pbState.getState() == PlaybackState.STATE_PLAYING;
            state.position = pbState.getPosition();
            state.customActions = pbState.getCustomActions();

            if (state.customActions != null) {
                Context pkgCtx = null;
                try {
                    pkgCtx = mContext.createPackageContext(state.packageName, 0);
                } catch (Throwable ignored) {}

                for (int i = 0; i < state.customActions.size(); i++) {
                    PlaybackState.CustomAction ca = state.customActions.get(i);
                    if (ca == null) continue;
                    String act = ca.getAction() != null ? ca.getAction().toLowerCase() : "";
                    String name = ca.getName() != null ? ca.getName().toString().toLowerCase() : "";

                    boolean isRadio = act.contains("radio") || name.contains("радио");
                    if (isRadio) continue; // NEVER bind radio as like or shuffle!

                    boolean isRepeat = act.contains("repeat") || name.contains("повтор");

                    Drawable icon = null;
                    if (ca.getIcon() != 0) {
                        try {
                            icon = android.graphics.drawable.Icon.createWithResource(state.packageName, ca.getIcon()).loadDrawable(mContext);
                        } catch (Throwable ignored) {}
                        if (icon == null && pkgCtx != null) {
                            try {
                                icon = pkgCtx.getDrawable(ca.getIcon());
                            } catch (Throwable ignored) {}
                        }
                    }

                    boolean isShuffle = !isRepeat && (act.contains("shuffle") || name.contains("перемеш") || name.contains("shuffle"));
                    boolean isLike = !isRepeat && !isShuffle && (
                            act.contains("like") || act.contains("favorite") || act.contains("thumb") ||
                            act.contains("collection") || act.contains("rating") ||
                            name.contains("коллекц") || name.contains("нравит") || name.contains("любит") ||
                            name.contains("избран") || name.contains("любим") ||
                            name.contains("like") || name.contains("favorite") || name.contains("thumb")
                    );

                    if (isLike) {
                        state.likeAction = ca;
                        if (name.contains("удалить") || name.contains("remove") || act.contains("unlike") || act.contains("remove")) {
                            state.isLiked = true;
                        } else if (name.contains("добавить") || name.contains("add")) {
                            state.isLiked = false;
                        }
                        if (icon != null) state.likeIcon = icon;
                    } else if (isShuffle) {
                        state.shuffleAction = ca;
                        if (name.contains("выключить") || act.contains("disable") || act.contains("off")) {
                            state.isShuffleOn = true;
                        } else {
                            state.isShuffleOn = false;
                        }
                        if (icon != null) state.shuffleIcon = icon;
                    }
                }

                // If after semantic inspection actions weren't identified, bind non-radio/non-repeat fallback slots
                if (state.shuffleAction == null && state.customActions.size() > 0) {
                    for (PlaybackState.CustomAction ca : state.customActions) {
                        if (ca == null || ca == state.likeAction) continue;
                        String act = ca.getAction() != null ? ca.getAction().toLowerCase() : "";
                        String name = ca.getName() != null ? ca.getName().toString().toLowerCase() : "";
                        if (act.contains("radio") || name.contains("радио") || act.contains("repeat") || name.contains("повтор")) continue;
                        state.shuffleAction = ca;
                        if (state.shuffleIcon == null && ca.getIcon() != 0) {
                            try {
                                state.shuffleIcon = android.graphics.drawable.Icon.createWithResource(state.packageName, ca.getIcon()).loadDrawable(mContext);
                            } catch (Throwable ignored) {}
                        }
                        break;
                    }
                }
                if (state.likeAction == null && state.customActions.size() > 0) {
                    for (PlaybackState.CustomAction ca : state.customActions) {
                        if (ca == null || ca == state.shuffleAction) continue;
                        String act = ca.getAction() != null ? ca.getAction().toLowerCase() : "";
                        String name = ca.getName() != null ? ca.getName().toString().toLowerCase() : "";
                        if (act.contains("radio") || name.contains("радио") || act.contains("repeat") || name.contains("повтор")) continue;
                        state.likeAction = ca;
                        if (name.contains("удалить") || name.contains("remove") || act.contains("unlike")) {
                            state.isLiked = true;
                        }
                        if (state.likeIcon == null && ca.getIcon() != 0) {
                            try {
                                state.likeIcon = android.graphics.drawable.Icon.createWithResource(state.packageName, ca.getIcon()).loadDrawable(mContext);
                            } catch (Throwable ignored) {}
                        }
                        break;
                    }
                }
            }
        }

        MediaMetadata metadata = null;
        try {
            metadata = mActiveController.getMetadata();
        } catch (Throwable ignored) {}

        if (metadata != null) {
            try {
                CharSequence seq = metadata.getText(MediaMetadata.METADATA_KEY_TITLE);
                if (seq != null) state.title = seq.toString();
                if (state.title == null || state.title.trim().isEmpty()) {
                    seq = metadata.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
                    if (seq != null) state.title = seq.toString();
                }
            } catch (Throwable ignored) {}

            try {
                CharSequence seq = metadata.getText(MediaMetadata.METADATA_KEY_ARTIST);
                if (seq != null) state.artist = seq.toString();
                if (state.artist == null || state.artist.trim().isEmpty()) {
                    seq = metadata.getText(MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
                    if (seq != null) state.artist = seq.toString();
                }
            } catch (Throwable ignored) {}

            try {
                CharSequence seq = metadata.getText(MediaMetadata.METADATA_KEY_ALBUM);
                if (seq != null) state.album = seq.toString();
            } catch (Throwable ignored) {}

            try {
                state.duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
            } catch (Throwable ignored) {}

            // Description fallback
            try {
                android.media.MediaDescription desc = metadata.getDescription();
                if (desc != null) {
                    if ((state.title == null || state.title.trim().isEmpty()) && desc.getTitle() != null) {
                        state.title = desc.getTitle().toString();
                    }
                    if ((state.artist == null || state.artist.trim().isEmpty()) && desc.getSubtitle() != null) {
                        state.artist = desc.getSubtitle().toString();
                    }
                    if ((state.album == null || state.album.trim().isEmpty()) && desc.getDescription() != null) {
                        state.album = desc.getDescription().toString();
                    }
                }
            } catch (Throwable ignored) {}

            // Robust album art extraction
            Bitmap art = null;
            try {
                art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                if (art == null) {
                    art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
                }
                if (art == null) {
                    art = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
                }
                if (art == null) {
                    String uriStr = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI);
                    if (uriStr == null) uriStr = metadata.getString(MediaMetadata.METADATA_KEY_ART_URI);
                    if (uriStr == null) uriStr = metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI);
                    if (uriStr != null) {
                        try {
                            Uri uri = Uri.parse(uriStr);
                            InputStream is = mContext.getContentResolver().openInputStream(uri);
                            if (is != null) {
                                art = BitmapFactory.decodeStream(is);
                                is.close();
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable ignored) {}
            state.albumArt = art;

            if (art != null && !art.isRecycled()) {
                int extractedMonet = 0;
                // 1. Exact SystemUI Monet ColorScheme reflection (style 6 / SchemeContent, primaryFixed)
                try {
                    Class<?> colorSchemeClass = mContext.getClassLoader().loadClass("com.android.systemui.monet.ColorScheme");
                    Class<?> wpColorsClass = android.app.WallpaperColors.class;
                    Object wpColors = wpColorsClass.getMethod("fromBitmap", Bitmap.class).invoke(null, art);
                    if (wpColors != null) {
                        java.lang.reflect.Constructor<?> ctor = colorSchemeClass.getConstructor(wpColorsClass, int.class);
                        Object colorScheme = ctor.newInstance(wpColors, 6);
                        java.lang.reflect.Field schemeField = colorSchemeClass.getField("mMaterialScheme");
                        Object materialScheme = schemeField.get(colorScheme);
                        java.lang.reflect.Method getPrimaryFixed = materialScheme.getClass().getMethod("getPrimaryFixed");
                        extractedMonet = (Integer) getPrimaryFixed.invoke(materialScheme);
                        Log.d(TAG, "Extracted SystemUI Monet primaryFixed: " + Integer.toHexString(extractedMonet));
                    }
                } catch (Throwable t) {
                    Log.d(TAG, "SystemUI ColorScheme reflection fallback: " + t.getMessage());
                }

                if (extractedMonet != 0) {
                    state.monetColor = extractedMonet;
                } else {
                    try {
                        android.app.WallpaperColors wc = android.app.WallpaperColors.fromBitmap(art);
                        if (wc != null && wc.getPrimaryColor() != null) {
                            state.monetColor = wc.getPrimaryColor().toArgb();
                        }
                    } catch (Throwable t2) {
                        Palette palette = Palette.from(art).generate();
                        int defaultColor = 0xFF8AB4F8;
                        state.monetColor = palette.getVibrantColor(
                                palette.getLightVibrantColor(
                                        palette.getDominantColor(defaultColor)
                                )
                        );
                    }
                }
            }
        }

        // Fallback for title if empty/null (common in video players like YouTube, browsers, etc.)
        if ((state.title == null || state.title.trim().isEmpty()) && state.packageName != null && !state.packageName.isEmpty()) {
            try {
                PackageManager pm = mContext.getPackageManager();
                CharSequence appLabel = pm.getApplicationLabel(pm.getApplicationInfo(state.packageName, 0));
                if (appLabel != null && !appLabel.toString().trim().isEmpty()) {
                    state.title = appLabel.toString();
                }
            } catch (Throwable ignored) {}
        }

        if (state.title == null || state.title.trim().isEmpty()) {
            state.title = state.isPlaying ? "Media" : "";
        }
        if (state.artist == null) state.artist = "";
        if (state.album == null) state.album = "";
        if (state.packageName == null) state.packageName = "";

        mHandler.post(() -> mListener.onMediaUpdated(state));
    }

    public void playPause() {
        if (mActiveController != null && mActiveController.getTransportControls() != null) {
            PlaybackState pbState = mActiveController.getPlaybackState();
            if (pbState != null && pbState.getState() == PlaybackState.STATE_PLAYING) {
                mActiveController.getTransportControls().pause();
            } else {
                mActiveController.getTransportControls().play();
            }
        }
    }

    public void next() {
        if (mActiveController != null && mActiveController.getTransportControls() != null) {
            mActiveController.getTransportControls().skipToNext();
        }
    }

    public void previous() {
        if (mActiveController != null && mActiveController.getTransportControls() != null) {
            mActiveController.getTransportControls().skipToPrevious();
        }
    }

    public void seekTo(long pos) {
        if (mActiveController != null && mActiveController.getTransportControls() != null) {
            mActiveController.getTransportControls().seekTo(pos);
        }
    }

    public void triggerCustomAction(PlaybackState.CustomAction action) {
        if (action != null && mActiveController != null && mActiveController.getTransportControls() != null) {
            try {
                mActiveController.getTransportControls().sendCustomAction(action, action.getExtras());
            } catch (Throwable t) {
                try {
                    mActiveController.getTransportControls().sendCustomAction(action.getAction(), action.getExtras());
                } catch (Throwable ignored) {}
            }
        }
    }

    public void triggerCustomAction(String action, android.os.Bundle extras) {
        if (mActiveController != null && mActiveController.getTransportControls() != null) {
            mActiveController.getTransportControls().sendCustomAction(action, extras);
        }
    }
}
