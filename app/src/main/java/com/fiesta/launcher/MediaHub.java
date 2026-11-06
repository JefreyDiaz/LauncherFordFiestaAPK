package com.fiesta.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MediaHub {
    public static final class Snapshot {
        public final String title;
        public final String artist;
        public final String packageName;
        public final boolean playing;
        public final long position;
        public final long duration;
        public final long anchorElapsed;
        public final float speed;
        public final Bitmap art;

        Snapshot(String title, String artist, String packageName, boolean playing, long position, long duration,
                 long anchorElapsed, float speed, Bitmap art) {
            this.title = title;
            this.artist = artist;
            this.packageName = packageName;
            this.playing = playing;
            this.position = position;
            this.duration = duration;
            this.anchorElapsed = anchorElapsed;
            this.speed = speed;
            this.art = art;
        }

        static Snapshot empty() {
            return new Snapshot("", "", "", false, 0L, 0L, 0L, 1f, null);
        }

        long estimatedPosition() {
            if (!playing) {
                return position;
            }
            long extra = (long) ((SystemClock.elapsedRealtime() - anchorElapsed) * speed);
            long value = position + Math.max(0L, extra);
            if (duration > 0L) {
                return Math.min(value, duration);
            }
            return value;
        }
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<Runnable>();
    private static MediaController controller;
    private static MediaController.Callback callback;
    private static Snapshot snapshot = Snapshot.empty();
    private static MediaSessionManager sessionManager;
    private static MediaSessionManager.OnActiveSessionsChangedListener sessionsListener;

    private MediaHub() {
    }

    public static MediaController controller() {
        return controller;
    }

    public static Snapshot snapshot() {
        return snapshot;
    }

    public static void addListener(Runnable listener) {
        LISTENERS.add(listener);
    }

    public static void removeListener(Runnable listener) {
        LISTENERS.remove(listener);
    }

    public static void refresh(Context context) {
        final Context app = context.getApplicationContext();
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(new Runnable() {
                @Override
                public void run() {
                    refresh(app);
                }
            });
            return;
        }
        if (!MediaListenerService.enabled(app)) {
            publish(app, null);
            return;
        }
        ComponentName listener = new ComponentName(app, MediaListenerService.class);
        List<MediaController> sessions;
        try {
            if (sessionManager == null) {
                sessionManager = (MediaSessionManager) app.getSystemService(Context.MEDIA_SESSION_SERVICE);
            }
            if (sessionManager == null) {
                return;
            }
            if (sessionsListener == null) {
                sessionsListener = new MediaSessionManager.OnActiveSessionsChangedListener() {
                    @Override
                    public void onActiveSessionsChanged(List<MediaController> controllers) {
                        choose(app, controllers);
                    }
                };
                sessionManager.addOnActiveSessionsChangedListener(sessionsListener, listener, MAIN);
            }
            sessions = sessionManager.getActiveSessions(listener);
        } catch (Exception ignored) {
            sessionsListener = null;
            return;
        }
        choose(app, sessions);
    }

    private static void choose(Context context, List<MediaController> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            publish(context, null);
            return;
        }
        MediaController best = null;
        for (MediaController candidate : sessions) {
            if (candidate == null || context.getPackageName().equals(candidate.getPackageName())) {
                continue;
            }
            PlaybackState state = candidate.getPlaybackState();
            if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                best = candidate;
                break;
            }
            if (best == null) {
                best = candidate;
            }
        }
        if (best != null && controller != null && !isPlaying(best) && isPlaying(controller)
                && sessions.contains(controller)) {
            return;
        }
        publish(context, best);
    }

    private static boolean isPlaying(MediaController target) {
        try {
            PlaybackState state = target.getPlaybackState();
            return state != null && state.getState() == PlaybackState.STATE_PLAYING;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static void publish(final Context context, MediaController next) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            final MediaController target = next;
            MAIN.post(new Runnable() {
                @Override
                public void run() {
                    publish(context, target);
                }
            });
            return;
        }
        if (controller != null && next != null && controller.getSessionToken().equals(next.getSessionToken())) {
            read();
            return;
        }
        if (controller != null && callback != null) {
            try {
                controller.unregisterCallback(callback);
            } catch (Exception ignored) {
            }
        }
        controller = next;
        if (controller == null) {
            callback = null;
            replace(Snapshot.empty());
            return;
        }
        callback = new MediaController.Callback() {
            @Override
            public void onMetadataChanged(MediaMetadata metadata) {
                read();
            }

            @Override
            public void onPlaybackStateChanged(PlaybackState state) {
                read();
                if (state == null || state.getState() != PlaybackState.STATE_PLAYING) {
                    refresh(context);
                }
            }

            @Override
            public void onSessionDestroyed() {
                publish(context, null);
                refresh(context);
            }
        };
        controller.registerCallback(callback, MAIN);
        read();
    }

    private static void read() {
        if (controller == null) {
            replace(Snapshot.empty());
            return;
        }
        MediaMetadata metadata;
        PlaybackState state;
        try {
            metadata = controller.getMetadata();
            state = controller.getPlaybackState();
        } catch (Exception ignored) {
            replace(Snapshot.empty());
            return;
        }
        String title = "";
        String artist = "";
        long duration = 0L;
        Bitmap art = null;
        if (metadata != null) {
            CharSequence titleText = metadata.getText(MediaMetadata.METADATA_KEY_TITLE);
            if (titleText == null) {
                titleText = metadata.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
            }
            CharSequence artistText = metadata.getText(MediaMetadata.METADATA_KEY_ARTIST);
            if (artistText == null) {
                artistText = metadata.getText(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE);
            }
            if (artistText == null) {
                artistText = metadata.getText(MediaMetadata.METADATA_KEY_ALBUM);
            }
            title = titleText == null ? "" : titleText.toString();
            artist = artistText == null ? "" : artistText.toString();
            duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
            try {
                art = scaleArt(metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART));
                if (art == null) {
                    art = scaleArt(metadata.getBitmap(MediaMetadata.METADATA_KEY_ART));
                }
                if (art == null) {
                    art = scaleArt(metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON));
                }
            } catch (Exception ignored) {
                art = null;
            }
        }
        boolean playing = false;
        long position = 0L;
        float speed = 1f;
        long anchor = SystemClock.elapsedRealtime();
        if (state != null) {
            playing = state.getState() == PlaybackState.STATE_PLAYING;
            position = Math.max(0L, state.getPosition());
            if (state.getPlaybackSpeed() != 0f) {
                speed = state.getPlaybackSpeed();
            }
            if (state.getLastPositionUpdateTime() > 0L) {
                anchor = state.getLastPositionUpdateTime();
            }
        }
        String pkg = controller.getPackageName();
        replace(new Snapshot(title, artist, pkg == null ? "" : pkg, playing, position, duration, anchor, speed, art));
    }

    private static void replace(Snapshot next) {
        Snapshot previous = snapshot;
        snapshot = next;
        notifyListeners();
        if (previous.art != null && previous.art != next.art) {
            previous.art.recycle();
        }
    }

    private static Bitmap scaleArt(Bitmap source) {
        if (source == null || source.isRecycled() || source.getWidth() <= 0 || source.getHeight() <= 0) {
            return null;
        }
        int max = 200;
        float scale = Math.min(1f, max / (float) Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, Math.round(source.getWidth() * scale));
        int height = Math.max(1, Math.round(source.getHeight() * scale));
        Bitmap scaled = Bitmap.createScaledBitmap(source, width, height, true);
        if (scaled == source) {
            Bitmap.Config config = source.getConfig() == null ? Bitmap.Config.ARGB_8888 : source.getConfig();
            return source.copy(config, false);
        }
        return scaled;
    }

    private static void notifyListeners() {
        for (Runnable listener : LISTENERS) {
            listener.run();
        }
    }
}
