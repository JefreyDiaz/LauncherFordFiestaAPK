package com.fiesta.launcher;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class WeatherClient {
    public interface Callback {
        void onDone();
    }

    private WeatherClient() {
    }

    public static void refresh(Context context, double latitude, double longitude, Callback callback) {
        final Context app = context.getApplicationContext();
        long cachedAt = Prefs.of(app).getLong(Prefs.WEATHER_AT, 0L);
        if (System.currentTimeMillis() - cachedAt < 30L * 60L * 1000L) {
            if (callback != null) {
                callback.onDone();
            }
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    String address = String.format(Locale.US,
                            "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current_weather=true",
                            latitude, longitude);
                    connection = (HttpURLConnection) new URL(address).openConnection();
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    connection.setRequestProperty("Accept", "application/json");
                    InputStream stream = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                    StringBuilder body = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        body.append(line);
                    }
                    reader.close();
                    JSONObject current = new JSONObject(body.toString()).getJSONObject("current_weather");
                    int temp = (int) Math.round(current.getDouble("temperature"));
                    String text = describe(current.getInt("weathercode"));
                    Prefs.of(app).edit()
                            .putInt(Prefs.WEATHER_TEMP, temp)
                            .putString(Prefs.WEATHER_TEXT, text)
                            .putLong(Prefs.WEATHER_AT, System.currentTimeMillis())
                            .apply();
                } catch (Exception ignored) {
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
                if (callback != null) {
                    callback.onDone();
                }
            }
        }).start();
    }

    static String describe(int code) {
        if (code == 0) {
            return "Despejado";
        }
        if (code <= 3) {
            return "Nublado";
        }
        if (code == 45 || code == 48) {
            return "Niebla";
        }
        if (code >= 51 && code <= 67) {
            return "Lluvia";
        }
        if (code >= 71 && code <= 77) {
            return "Nieve";
        }
        if (code >= 80 && code <= 82) {
            return "Aguacero";
        }
        if (code >= 95) {
            return "Tormenta";
        }
        return "Nublado";
    }
}
