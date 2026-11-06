package com.fiesta.launcher;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeActivity extends Activity {
    private static final int REQ_MAP = 1;
    private static final int REQ_RADIO = 2;
    private static final int REQ_AUTO = 3;
    private static final int REQ_LOCATION = 4;
    private static final int REQ_ADMIN = 5;
    private static final String[] LEGACY_MEDIA_ACTIONS = {
            "com.android.music.metachanged",
            "com.android.music.playstatechanged",
            "com.android.music.playbackcomplete",
            "com.spotify.music.metadatachanged",
            "com.spotify.music.playbackstatechanged"
    };
    private static final int REQ_SLOT = 20;
    private static final String HUR_PACKAGE = "gb.xxy.hr";

    private static final String[] MAP_PACKAGES = {"com.google.android.apps.maps", "com.waze"};
    private static final String[] MAP_WORDS = {"mapa", "maps", "navi", "waze"};
    private static final String[] RADIO_WORDS = {"radio", "fm"};
    private static final String[] MUSIC_PACKAGES = {"com.spotify.music"};
    private static final String[] MUSIC_WORDS = {"bluetooth", "spotify", "music", "musica"};
    private static final String[] PHONE_PACKAGES = {"com.google.android.dialer", "com.android.dialer"};
    private static final String[] PHONE_WORDS = {"tel", "phone", "dial", "llamad"};

    private static final class Slot {
        final String key;
        final String[] packages;
        final String[] words;
        final int label;
        final int icon;
        final boolean settingsFallback;

        Slot(String key, String[] packages, String[] words, int label, int icon, boolean settingsFallback) {
            this.key = key;
            this.packages = packages;
            this.words = words;
            this.label = label;
            this.icon = icon;
            this.settingsFallback = settingsFallback;
        }
    }

    private final Slot[] slots = {
            new Slot(Prefs.SHORTCUT_RADIO, new String[0], RADIO_WORDS, R.string.radio, R.drawable.ic_radio, false),
            new Slot(Prefs.SHORTCUT_MUSIC, MUSIC_PACKAGES, MUSIC_WORDS, R.string.music, R.drawable.ic_note, false),
            new Slot(Prefs.SHORTCUT_PHONE, PHONE_PACKAGES, PHONE_WORDS, R.string.phone, R.drawable.ic_phone, false),
            new Slot(Prefs.SHORTCUT_MAP, MAP_PACKAGES, MAP_WORDS, R.string.map, R.drawable.ic_map, false),
            new Slot(Prefs.SHORTCUT_LINK, new String[0], new String[0], R.string.choose_slot, R.drawable.ic_link, false),
            new Slot(Prefs.SHORTCUT_EXTRA, new String[0], new String[0], R.string.settings, R.drawable.ic_settings, true)
    };

    private TextView clock;
    private TextView dateLine;
    private TextView tripLine;
    private TextView carNote;
    private TextView picoLine;
    private TextView autoSub;
    private TextView speed;
    private TextView weather;
    private TextView weatherLabel;
    private TextView mediaTitle;
    private TextView mediaArtist;
    private ImageView carImage;
    private ImageView carBackdrop;
    private View heroScrim;
    private TextView brandLine;
    private ImageView mediaArt;
    private ImageView btnPlay;
    private ImageView btnTheme;
    private ImageView[] slotIcons;
    private TextView[] slotLabels;
    private View screenOff;
    private ProgressBar mediaProgress;
    private ProgressBar speedBar;

    private Handler handler;
    private Runnable tick;
    private Runnable mediaListener;
    private LocationManager locationManager;
    private boolean started;
    private boolean appliedDark;
    private int ticks;
    private boolean legacyRegistered;
    private String legacyTitle = "";
    private String legacyArtist = "";
    private boolean legacyPlaying;

    private final BroadcastReceiver legacyMedia = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String track = intent.getStringExtra("track");
            String artist = intent.getStringExtra("artist");
            if (track != null && !track.trim().isEmpty()) {
                legacyTitle = track.trim();
                legacyArtist = artist == null ? "" : artist.trim();
            }
            if (intent.hasExtra("playing")) {
                legacyPlaying = intent.getBooleanExtra("playing", legacyPlaying);
            } else if (intent.hasExtra("playstate")) {
                legacyPlaying = intent.getBooleanExtra("playstate", legacyPlaying);
            }
            if ("com.android.music.playbackcomplete".equals(intent.getAction())) {
                legacyPlaying = false;
            }
            renderMedia();
        }
    };
    private Bitmap carBitmap;
    private Location lastFix;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE '·' d MMM", new Locale("es", "CO"));

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            onFix(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppTheme.apply(this);
        appliedDark = AppTheme.isDark(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        bind();
        wire();
        handler = new Handler(Looper.getMainLooper());
        tick = new Runnable() {
            @Override
            public void run() {
                renderClock();
                renderMedia();
                ticks++;
                if (ticks % 5 == 0 && !MediaHub.snapshot().playing) {
                    MediaHub.refresh(HomeActivity.this);
                }
                if (AppTheme.isDark(HomeActivity.this) != appliedDark) {
                    recreate();
                    return;
                }
                handler.postDelayed(this, 1000L);
            }
        };
        mediaListener = new Runnable() {
            @Override
            public void run() {
                renderMedia();
            }
        };
        askLocation();
        buildSlots();
        applyBrightness();
    }

    @Override
    protected void onStart() {
        super.onStart();
        started = true;
        MediaHub.addListener(mediaListener);
        MediaListenerService.requestRescan(this);
        IntentFilter filter = new IntentFilter();
        for (String action : LEGACY_MEDIA_ACTIONS) {
            filter.addAction(action);
        }
        try {
            registerReceiver(legacyMedia, filter);
            legacyRegistered = true;
        } catch (Exception ignored) {
            legacyRegistered = false;
        }
        handler.post(tick);
        startLocation();
        renderAll();
    }

    @Override
    protected void onStop() {
        started = false;
        handler.removeCallbacks(tick);
        MediaHub.removeListener(mediaListener);
        if (legacyRegistered) {
            try {
                unregisterReceiver(legacyMedia);
            } catch (Exception ignored) {
            }
            legacyRegistered = false;
        }
        stopLocation();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideBars();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideBars();
        }
    }

    @SuppressWarnings("deprecation")
    private void hideBars() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (screenOff.getVisibility() == View.VISIBLE) {
            hideScreenOff();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ADMIN) {
            DevicePolicyManager policy = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            if (policy != null && policy.isAdminActive(new ComponentName(this, ScreenAdmin.class))) {
                try {
                    policy.lockNow();
                    return;
                } catch (Exception ignored) {
                }
            }
            Toast.makeText(this, R.string.admin_denied, Toast.LENGTH_LONG).show();
            showScreenOff();
            return;
        }
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        String flat = data.getStringExtra(AppsActivity.EXTRA_COMPONENT);
        if (flat == null) {
            return;
        }
        if (requestCode == REQ_AUTO) {
            Prefs.of(this).edit().putString(Prefs.SHORTCUT_AUTO, flat).apply();
            Toast.makeText(this, R.string.saved_app, Toast.LENGTH_SHORT).show();
            renderAuto();
            return;
        }
        int slot = requestCode - REQ_SLOT;
        if (slot < 0 || slot >= slots.length) {
            return;
        }
        Prefs.of(this).edit().putString(slots[slot].key, flat).apply();
        Toast.makeText(this, R.string.saved_app, Toast.LENGTH_SHORT).show();
        renderSlots();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION && started) {
            startLocation();
        }
    }

    private void bind() {
        clock = findViewById(R.id.clock);
        dateLine = findViewById(R.id.dateLine);
        tripLine = findViewById(R.id.tripLine);
        carNote = findViewById(R.id.carNote);
        picoLine = findViewById(R.id.picoLine);
        autoSub = findViewById(R.id.autoSub);
        speed = findViewById(R.id.speed);
        speedBar = findViewById(R.id.speedBar);
        weather = findViewById(R.id.weather);
        weatherLabel = findViewById(R.id.weatherLabel);
        mediaTitle = findViewById(R.id.mediaTitle);
        mediaArtist = findViewById(R.id.mediaArtist);
        mediaArt = findViewById(R.id.mediaArt);
        carImage = findViewById(R.id.carImage);
        btnPlay = findViewById(R.id.btnPlay);
        btnTheme = findViewById(R.id.btnTheme);
        screenOff = findViewById(R.id.screenOff);
        mediaProgress = findViewById(R.id.mediaProgress);
        mediaArt.setClipToOutline(true);
        carBackdrop = findViewById(R.id.carBackdrop);
        heroScrim = findViewById(R.id.heroScrim);
        brandLine = findViewById(R.id.brandLine);
        findViewById(R.id.hero).setClipToOutline(true);
    }

    private void wire() {
        findViewById(R.id.btnScreenOff).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnScreenOff();
            }
        });
        findViewById(R.id.btnScreenOff).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showScreenOff();
                return true;
            }
        });
        findViewById(R.id.mediaCard).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMedia();
            }
        });
        findViewById(R.id.btnBrightness).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int level = Prefs.of(HomeActivity.this).getInt(Prefs.BRIGHTNESS, -1);
                level = level >= 2 ? -1 : level + 1;
                Prefs.of(HomeActivity.this).edit().putInt(Prefs.BRIGHTNESS, level).apply();
                applyBrightness();
                int message = R.string.brightness_auto;
                if (level == 0) {
                    message = R.string.brightness_low;
                } else if (level == 1) {
                    message = R.string.brightness_mid;
                } else if (level == 2) {
                    message = R.string.brightness_high;
                }
                Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
        screenOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideScreenOff();
            }
        });
        btnTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean dark = AppTheme.isDark(HomeActivity.this);
                Prefs.of(HomeActivity.this).edit()
                        .putBoolean(Prefs.DARK_MANUAL, true)
                        .putBoolean(Prefs.DARK, !dark)
                        .apply();
                recreate();
            }
        });
        findViewById(R.id.btnApps).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, AppsActivity.class));
            }
        });
        findViewById(R.id.btnPrev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            }
        });
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
            }
        });
        findViewById(R.id.btnNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaKey(KeyEvent.KEYCODE_MEDIA_NEXT);
            }
        });
        findViewById(R.id.btnAuto).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAuto();
            }
        });
        findViewById(R.id.btnAuto).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                pick(REQ_AUTO);
                return true;
            }
        });
    }

    private void pick(int request) {
        Intent intent = new Intent(this, AppsActivity.class);
        intent.putExtra(AppsActivity.EXTRA_PICK, true);
        startActivityForResult(intent, request);
    }

    private void openAuto() {
        String saved = Prefs.of(this).getString(Prefs.SHORTCUT_AUTO, "");
        if (saved != null && !saved.isEmpty()) {
            ComponentName component = ComponentName.unflattenFromString(saved);
            if (AppShortcuts.open(this, component)) {
                return;
            }
        }
        Intent launch = getPackageManager().getLaunchIntentForPackage(HUR_PACKAGE);
        if (launch != null) {
            try {
                startActivity(launch);
                return;
            } catch (Exception ignored) {
            }
        }
        Toast.makeText(this, R.string.hold_to_pick, Toast.LENGTH_SHORT).show();
    }

    private void renderAll() {
        renderClock();
        renderTrip();
        renderCarNote();
        renderBrand();
        renderPico();
        renderAuto();
        renderWeather();
        renderCar();
        renderSlots();
        renderMedia();
        btnTheme.setImageResource(AppTheme.isDark(this) ? R.drawable.ic_sun : R.drawable.ic_moon);
    }

    private void renderClock() {
        String date = dateFormat.format(new Date());
        if (!date.isEmpty()) {
            date = date.substring(0, 1).toUpperCase(new Locale("es", "CO")) + date.substring(1);
        }
        dateLine.setText(date);
        clock.setText(timeFormat.format(new Date()));
    }

    private void renderTrip() {
        SharedPreferences prefs = Prefs.of(this);
        Calendar now = Calendar.getInstance();
        String day = String.format(Locale.US, "%04d%02d%02d",
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
        String month = day.substring(0, 6);
        float dayMeters = day.equals(prefs.getString(Prefs.TRIP_DAY, "")) ? prefs.getFloat(Prefs.TRIP_DAY_M, 0f) : 0f;
        float monthMeters = month.equals(prefs.getString(Prefs.TRIP_MONTH, "")) ? prefs.getFloat(Prefs.TRIP_MONTH_M, 0f) : 0f;
        tripLine.setText(getString(R.string.trip_line, Math.round(dayMeters / 1000f), Math.round(monthMeters / 1000f)));
    }

    private void renderCarNote() {
        SharedPreferences prefs = Prefs.of(this);
        StringBuilder line = new StringBuilder();
        String date = prefs.getString(Prefs.OIL_DATE, "");
        if (date != null && date.length() >= 8) {
            String shown = date;
            String[] parts = date.split("-");
            if (parts.length == 3) {
                shown = parts[2] + "/" + parts[1] + "/" + parts[0];
            }
            line.append(getString(R.string.car_note_oil, shown));
        }
        int km = prefs.getInt(Prefs.OIL_KM, -1);
        if (km >= 0) {
            if (line.length() > 0) {
                line.append(" · ");
            }
            line.append(getString(R.string.car_note_km, String.valueOf(km)));
        }
        String tires = prefs.getString(Prefs.TIRES, "");
        if (tires != null && !tires.trim().isEmpty()) {
            if (line.length() > 0) {
                line.append(" · ");
            }
            line.append(getString(R.string.car_note_tires, tires.trim()));
        }
        if (line.length() == 0) {
            carNote.setVisibility(View.GONE);
            return;
        }
        carNote.setVisibility(View.VISIBLE);
        carNote.setText(line.toString());
    }

    private void renderAuto() {
        String saved = Prefs.of(this).getString(Prefs.SHORTCUT_AUTO, "");
        boolean assigned = saved != null && !saved.isEmpty();
        boolean hur = getPackageManager().getLaunchIntentForPackage(HUR_PACKAGE) != null;
        autoSub.setText(assigned || hur ? R.string.android_auto_sub : R.string.hold_to_pick);
    }

    private void renderPico() {
        SharedPreferences prefs = Prefs.of(this);
        int digit = PicoPlaca.lastDigit(prefs.getString(Prefs.PLATE, ""));
        if (digit < 0) {
            picoLine.setVisibility(View.GONE);
            return;
        }
        boolean blocked = PicoPlaca.restricted(Calendar.getInstance(), digit, prefs.getString(Prefs.PICO, PicoPlaca.DEFAULT));
        picoLine.setVisibility(View.VISIBLE);
        picoLine.setText(blocked ? R.string.today_bad : R.string.today_ok);
        boolean dark = AppTheme.isDark(this);
        int color;
        if (blocked) {
            color = dark ? 0xFFFF9A9A : 0xFFC44747;
        } else {
            color = dark ? 0xFF6BF0A8 : 0xFF14804A;
        }
        picoLine.setTextColor(color);
    }

    private void renderWeather() {
        SharedPreferences prefs = Prefs.of(this);
        if (!prefs.contains(Prefs.WEATHER_TEMP)) {
            weather.setText("—");
            weatherLabel.setText("Clima");
            return;
        }
        weather.setText(prefs.getInt(Prefs.WEATHER_TEMP, 0) + "°");
        String text = prefs.getString(Prefs.WEATHER_TEXT, "");
        weatherLabel.setText(text == null || text.isEmpty() ? "Clima" : text);
    }

    private void renderCar() {
        File file = new File(getFilesDir(), "car.jpg");
        if (file.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap != null) {
                Bitmap previous = carBitmap;
                showBackdrop(false);
                carImage.setImageBitmap(bitmap);
                carBitmap = bitmap;
                if (previous != null && previous != bitmap) {
                    previous.recycle();
                }
                return;
            }
        }
        boolean dark = AppTheme.isDark(this);
        showBackdrop(dark);
        if (dark) {
            carImage.setImageDrawable(null);
        } else {
            carImage.setImageResource(R.drawable.fiesta_car);
        }
        if (carBitmap != null) {
            carBitmap.recycle();
            carBitmap = null;
        }
    }

    private void showBackdrop(boolean show) {
        findViewById(R.id.hero).setBackgroundResource(show ? R.drawable.bg_hero_dark : R.drawable.bg_hero_light);
        if (show) {
            if (carBackdrop.getDrawable() == null) {
                carBackdrop.setImageResource(R.drawable.fiesta_night);
            }
            carBackdrop.setVisibility(View.VISIBLE);
            heroScrim.setVisibility(View.VISIBLE);
        } else {
            carBackdrop.setImageDrawable(null);
            carBackdrop.setVisibility(View.GONE);
            heroScrim.setVisibility(View.GONE);
        }
    }

    private void renderBrand() {
        String detail = Prefs.of(this).getString(Prefs.DETAIL, "");
        SpannableStringBuilder text = new SpannableStringBuilder("FORD ");
        int start = text.length();
        text.append("FIESTA");
        text.setSpan(new StyleSpan(Typeface.BOLD), start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (detail != null && !detail.trim().isEmpty()) {
            text.append(" · ").append(detail.trim().toUpperCase(Locale.ROOT));
        }
        brandLine.setText(text);
    }

    private void renderMedia() {
        MediaHub.Snapshot snap = MediaHub.snapshot();
        boolean hasSession = MediaHub.controller() != null;
        String title = snap.title == null ? "" : snap.title.trim();
        String artist = snap.artist == null ? "" : snap.artist.trim();
        boolean playing = snap.playing;
        if (!hasSession && !legacyTitle.isEmpty()) {
            title = legacyTitle;
            artist = legacyArtist;
            playing = legacyPlaying;
        }
        if (title.isEmpty() && hasSession) {
            title = appLabel(snap.packageName);
            artist = getString(playing ? R.string.media_playing : R.string.media_paused);
        }
        if (title.isEmpty()) {
            if (!MediaListenerService.enabled(this)) {
                title = getString(R.string.media_enable);
                artist = getString(R.string.media_enable_sub);
            } else {
                title = getString(R.string.no_media);
                artist = getString(R.string.media_tap_open);
            }
        }
        mediaTitle.setText(title);
        if (artist.isEmpty()) {
            mediaArtist.setVisibility(View.GONE);
        } else {
            mediaArtist.setVisibility(View.VISIBLE);
            mediaArtist.setText(artist);
        }
        btnPlay.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        if (snap.art != null) {
            mediaArt.setImageBitmap(snap.art);
            mediaArt.setImageTintList(null);
            mediaArt.setPadding(0, 0, 0, 0);
        } else {
            mediaArt.setImageResource(R.drawable.ic_note);
            mediaArt.setImageTintList(ColorStateList.valueOf(themeColor(R.attr.colorInkSoft)));
            int pad = dp(16);
            mediaArt.setPadding(pad, pad, pad, pad);
        }
        if (snap.duration > 0L) {
            int progress = (int) (snap.estimatedPosition() * 1000L / snap.duration);
            mediaProgress.setProgress(Math.max(0, Math.min(1000, progress)));
        } else {
            mediaProgress.setProgress(0);
        }
    }

    private String appLabel(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return "";
        }
        try {
            PackageManager manager = getPackageManager();
            return manager.getApplicationLabel(manager.getApplicationInfo(packageName, 0)).toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private void openMedia() {
        if (!MediaListenerService.enabled(this)) {
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            } catch (Exception ignored) {
                startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
            }
            return;
        }
        MediaController controller = MediaHub.controller();
        if (controller != null) {
            android.app.PendingIntent session = controller.getSessionActivity();
            if (session != null) {
                try {
                    session.send();
                    return;
                } catch (Exception ignored) {
                }
            }
            Intent launch = getPackageManager().getLaunchIntentForPackage(controller.getPackageName());
            if (launch != null) {
                try {
                    startActivity(launch);
                    return;
                } catch (Exception ignored) {
                }
            }
        }
        openSlot(1);
    }

    private void turnScreenOff() {
        DevicePolicyManager policy = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, ScreenAdmin.class);
        if (policy != null && policy.isAdminActive(admin)) {
            try {
                policy.lockNow();
                return;
            } catch (Exception ignored) {
            }
            showScreenOff();
            return;
        }
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.admin_why));
        try {
            startActivityForResult(intent, REQ_ADMIN);
        } catch (Exception ignored) {
            showScreenOff();
        }
    }

    private void showScreenOff() {
        screenOff.setVisibility(View.VISIBLE);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.screenBrightness = 0.01f;
        getWindow().setAttributes(params);
    }

    private void hideScreenOff() {
        screenOff.setVisibility(View.GONE);
        applyBrightness();
    }

    private void applyBrightness() {
        if (screenOff.getVisibility() == View.VISIBLE) {
            return;
        }
        int level = Prefs.of(this).getInt(Prefs.BRIGHTNESS, -1);
        float value = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        if (level == 0) {
            value = 0.2f;
        } else if (level == 1) {
            value = 0.55f;
        } else if (level == 2) {
            value = 1f;
        }
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.screenBrightness = value;
        getWindow().setAttributes(params);
    }

    private void mediaKey(int keyCode) {
        MediaController controller = MediaHub.controller();
        if (controller != null) {
            MediaController.TransportControls controls = controller.getTransportControls();
            if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
                controls.skipToNext();
                return;
            }
            if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                controls.skipToPrevious();
                return;
            }
            PlaybackState state = controller.getPlaybackState();
            if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                controls.pause();
            } else {
                controls.play();
            }
            return;
        }
        AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audio == null) {
            return;
        }
        audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
        audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
    }

    private void askLocation() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        requestPermissions(new String[]{
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
        }, REQ_LOCATION);
    }

    private void startLocation() {
        stopLocation();
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        }
        if (locationManager == null) {
            return;
        }
        String[] providers = {LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER};
        for (String provider : providers) {
            try {
                if (!locationManager.isProviderEnabled(provider)) {
                    continue;
                }
                Location last = locationManager.getLastKnownLocation(provider);
                if (last != null) {
                    onFix(last);
                }
                locationManager.requestLocationUpdates(provider, 2000L, 1f, locationListener);
            } catch (Exception ignored) {
            }
        }
    }

    private void stopLocation() {
        if (locationManager == null) {
            return;
        }
        try {
            locationManager.removeUpdates(locationListener);
        } catch (Exception ignored) {
        }
    }

    private void onFix(Location location) {
        if (location.hasSpeed()) {
            int kmh = Math.max(0, Math.round(location.getSpeed() * 3.6f));
            speed.setText(String.valueOf(kmh));
            speedBar.setProgress(Math.min(160, kmh));
        }
        addTrip(location);
        WeatherClient.refresh(this, location.getLatitude(), location.getLongitude(), new WeatherClient.Callback() {
            @Override
            public void onDone() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing()) {
                            renderWeather();
                        }
                    }
                });
            }
        });
    }

    private void addTrip(Location location) {
        if (location.hasAccuracy() && location.getAccuracy() > 40f) {
            return;
        }
        long now = location.getTime() > 0L ? location.getTime() : System.currentTimeMillis();
        if (lastFix != null) {
            float meters = lastFix.distanceTo(location);
            long elapsed = now - lastFix.getTime();
            if (elapsed > 500L && elapsed < 120000L && meters >= 12f && meters < 1200f && meters / (elapsed / 1000f) < 55f) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(now);
                String day = String.format(Locale.US, "%04d%02d%02d",
                        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
                String month = day.substring(0, 6);
                SharedPreferences prefs = Prefs.of(this);
                float dayMeters = day.equals(prefs.getString(Prefs.TRIP_DAY, "")) ? prefs.getFloat(Prefs.TRIP_DAY_M, 0f) : 0f;
                float monthMeters = month.equals(prefs.getString(Prefs.TRIP_MONTH, "")) ? prefs.getFloat(Prefs.TRIP_MONTH_M, 0f) : 0f;
                Prefs.of(this).edit()
                        .putString(Prefs.TRIP_DAY, day)
                        .putFloat(Prefs.TRIP_DAY_M, dayMeters + meters)
                        .putString(Prefs.TRIP_MONTH, month)
                        .putFloat(Prefs.TRIP_MONTH_M, monthMeters + meters)
                        .apply();
                renderTrip();
            }
        }
        lastFix = new Location(location);
    }

    private void buildSlots() {
        GridLayout grid = findViewById(R.id.slotGrid);
        slotIcons = new ImageView[slots.length];
        slotLabels = new TextView[slots.length];
        int ink = themeColor(R.attr.colorInk);
        for (int i = 0; i < slots.length; i++) {
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setBackgroundResource(R.drawable.bg_quick);
            cell.setClickable(true);
            cell.setFocusable(true);
            int pad = dp(4);
            cell.setPadding(pad, pad, pad, pad);
            ImageView icon = new ImageView(this);
            icon.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(36)));
            TextView label = new TextView(this);
            label.setTextColor(ink);
            label.setTextSize(12);
            label.setMaxLines(1);
            label.setEllipsize(TextUtils.TruncateAt.END);
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            textParams.topMargin = dp(2);
            label.setLayoutParams(textParams);
            cell.addView(icon);
            cell.addView(label);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(i / 3, 1f),
                    GridLayout.spec(i % 3, 1f));
            params.width = 0;
            params.height = 0;
            int margin = dp(3);
            params.setMargins(margin, margin, margin, margin);
            grid.addView(cell, params);
            slotIcons[i] = icon;
            slotLabels[i] = label;
            final int index = i;
            cell.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openSlot(index);
                }
            });
            cell.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    pick(REQ_SLOT + index);
                    return true;
                }
            });
        }
    }

    private void openSlot(int index) {
        Slot slot = slots[index];
        ComponentName component = AppShortcuts.resolve(this, slot.key, slot.packages, slot.words);
        if (component == null && slot.settingsFallback) {
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }
        if (!AppShortcuts.open(this, component)) {
            Toast.makeText(this, R.string.hold_to_pick, Toast.LENGTH_SHORT).show();
        }
    }

    private void renderSlots() {
        if (slotIcons == null) {
            return;
        }
        PackageManager manager = getPackageManager();
        int ink = themeColor(R.attr.colorInk);
        for (int i = 0; i < slots.length; i++) {
            Slot slot = slots[i];
            ComponentName component = AppShortcuts.resolve(this, slot.key, slot.packages, slot.words);
            if (component != null) {
                try {
                    slotIcons[i].setImageDrawable(manager.getActivityIcon(component));
                    slotIcons[i].setImageTintList(null);
                    slotLabels[i].setText(manager.getActivityInfo(component, 0).loadLabel(manager));
                    continue;
                } catch (Exception ignored) {
                }
            }
            slotIcons[i].setImageResource(slot.icon);
            slotIcons[i].setImageTintList(ColorStateList.valueOf(ink));
            slotLabels[i].setText(slot.label);
        }
    }

    private int themeColor(int attr) {
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(attr, value, true);
        return value.data;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
