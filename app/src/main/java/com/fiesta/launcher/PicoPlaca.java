package com.fiesta.launcher;

import java.util.Calendar;

public final class PicoPlaca {
    public static final String DEFAULT = "1,2;3,4;5,6;7,8;9,0";

    private PicoPlaca() {
    }

    public static int lastDigit(String plate) {
        if (plate == null) {
            return -1;
        }
        for (int i = plate.length() - 1; i >= 0; i--) {
            char c = plate.charAt(i);
            if (c >= '0' && c <= '9') {
                return c - '0';
            }
        }
        return -1;
    }

    public static boolean restricted(Calendar day, int digit, String spec) {
        if (digit < 0) {
            return false;
        }
        int index = dayIndex(day.get(Calendar.DAY_OF_WEEK));
        if (index < 0) {
            return false;
        }
        String[] days = split(spec);
        if (index >= days.length) {
            return false;
        }
        String slot = days[index];
        for (int i = 0; i < slot.length(); i++) {
            if (slot.charAt(i) == (char) ('0' + digit)) {
                return true;
            }
        }
        return false;
    }

    public static String[] days(String spec) {
        String[] raw = split(spec);
        String[] out = new String[5];
        for (int i = 0; i < 5; i++) {
            out[i] = i < raw.length ? raw[i].trim() : "";
        }
        return out;
    }

    public static String join(String monday, String tuesday, String wednesday, String thursday, String friday) {
        return clean(monday) + ";" + clean(tuesday) + ";" + clean(wednesday) + ";" + clean(thursday) + ";" + clean(friday);
    }

    private static String[] split(String spec) {
        String value = spec == null || spec.trim().isEmpty() ? DEFAULT : spec;
        return value.split(";", -1);
    }

    private static int dayIndex(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY:
                return 0;
            case Calendar.TUESDAY:
                return 1;
            case Calendar.WEDNESDAY:
                return 2;
            case Calendar.THURSDAY:
                return 3;
            case Calendar.FRIDAY:
                return 4;
            default:
                return -1;
        }
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') {
                continue;
            }
            if (builder.indexOf(String.valueOf(c)) >= 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(c);
        }
        return builder.toString();
    }
}
