package com.fiesta.launcher;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    public static final String NAME = "name";
    public static final String PLATE = "plate";
    public static final String DETAIL = "detail";
    public static final String OIL_DATE = "oil_date";
    public static final String OIL_KM = "oil_km";
    public static final String TIRES = "tires";
    public static final String PICO = "pico";
    public static final String HOURS = "hours";
    public static final String SHORTCUT_MAP = "shortcut_map";
    public static final String SHORTCUT_RADIO = "shortcut_radio";
    public static final String SHORTCUT_LINK = "shortcut_link";
    public static final String SHORTCUT_AUTO = "shortcut_auto";
    public static final String SHORTCUT_MUSIC = "shortcut_music";
    public static final String SHORTCUT_PHONE = "shortcut_phone";
    public static final String SHORTCUT_EXTRA = "shortcut_extra";
    public static final String TRIP_DAY = "trip_day";
    public static final String TRIP_DAY_M = "trip_day_m";
    public static final String TRIP_MONTH = "trip_month";
    public static final String TRIP_MONTH_M = "trip_month_m";
    public static final String BRIGHTNESS = "brightness";
    public static final String WEATHER_TEMP = "weather_temp";
    public static final String WEATHER_TEXT = "weather_text";
    public static final String WEATHER_AT = "weather_at";
    public static final String DARK = "dark";
    public static final String DARK_MANUAL = "dark_manual";

    private static final String FILE = "fiesta";

    private Prefs() {
    }

    public static SharedPreferences of(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
