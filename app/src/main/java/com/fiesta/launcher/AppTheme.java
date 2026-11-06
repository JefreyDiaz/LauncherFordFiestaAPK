package com.fiesta.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;

public final class AppTheme {
    private AppTheme() {
    }

    public static void apply(Activity activity) {
        activity.setTheme(isDark(activity) ? R.style.Theme_Fiesta_Dark : R.style.Theme_Fiesta);
    }

    public static boolean isDark(Context context) {
        SharedPreferences prefs = Prefs.of(context);
        if (prefs.getBoolean(Prefs.DARK_MANUAL, false)) {
            return prefs.getBoolean(Prefs.DARK, false);
        }
        return isNight(Calendar.getInstance());
    }

    static boolean isNight(Calendar now) {
        int minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        return minutes >= 18 * 60 + 30 || minutes < 6 * 60 + 30;
    }
}
