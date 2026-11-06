package com.fiesta.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.List;
import java.util.Locale;

public final class AppShortcuts {
    private AppShortcuts() {
    }

    public static ComponentName resolve(Context context, String key, String[] packages, String[] words) {
        PackageManager manager = context.getPackageManager();
        String saved = Prefs.of(context).getString(key, "");
        if (saved != null && !saved.isEmpty()) {
            ComponentName stored = ComponentName.unflattenFromString(saved);
            if (stored != null && launchable(manager, stored)) {
                return stored;
            }
        }
        ComponentName found = search(manager, context.getPackageName(), packages, words);
        if (found != null) {
            Prefs.of(context).edit().putString(key, found.flattenToString()).apply();
        }
        return found;
    }

    public static boolean open(Context context, ComponentName component) {
        if (component == null) {
            return false;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(component);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean launchable(PackageManager manager, ComponentName component) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(component);
            return manager.resolveActivity(intent, 0) != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static ComponentName search(PackageManager manager, String self, String[] packages, String[] words) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> all = manager.queryIntentActivities(intent, 0);
        if (packages != null) {
            for (String pkg : packages) {
                for (ResolveInfo info : all) {
                    if (info.activityInfo != null && pkg.equals(info.activityInfo.packageName)) {
                        return new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
                    }
                }
            }
        }
        for (ResolveInfo info : all) {
            if (info.activityInfo == null || self.equals(info.activityInfo.packageName)) {
                continue;
            }
            String label = info.loadLabel(manager).toString().toLowerCase(Locale.ROOT);
            String pkg = info.activityInfo.packageName.toLowerCase(Locale.ROOT);
            for (String word : words) {
                if (label.contains(word) || pkg.contains(word)) {
                    return new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
                }
            }
        }
        return null;
    }
}
