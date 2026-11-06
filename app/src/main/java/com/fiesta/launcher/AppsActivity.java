package com.fiesta.launcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppsActivity extends Activity {
    public static final String EXTRA_PICK = "pick";
    public static final String EXTRA_COMPONENT = "component";

    private boolean alive = true;
    private boolean pick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppTheme.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apps);
        pick = getIntent().getBooleanExtra(EXTRA_PICK, false);
        TextView title = findViewById(R.id.appsTitle);
        title.setText(pick ? R.string.pick_title : R.string.apps_title);
        final GridView grid = findViewById(R.id.grid);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<Item> items = load();
                if (!pick) {
                    Drawable icon = getDrawable(R.drawable.ic_settings);
                    if (icon != null) {
                        icon = icon.mutate();
                        TypedValue value = new TypedValue();
                        getTheme().resolveAttribute(R.attr.colorInk, value, true);
                        icon.setTint(value.data);
                    }
                    items.add(0, new Item(getString(R.string.settings), null, icon));
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!alive) {
                            return;
                        }
                        grid.setAdapter(new AppAdapter(items));
                    }
                });
            }
        }).start();
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppAdapter adapter = (AppAdapter) parent.getAdapter();
                if (adapter == null) {
                    return;
                }
                Item item = adapter.getItem(position);
                if (item.component == null) {
                    startActivity(new Intent(AppsActivity.this, SettingsActivity.class));
                    return;
                }
                if (pick) {
                    Intent data = new Intent();
                    data.putExtra(EXTRA_COMPONENT, item.component.flattenToString());
                    setResult(RESULT_OK, data);
                    finish();
                    return;
                }
                Intent launch = new Intent(Intent.ACTION_MAIN);
                launch.addCategory(Intent.CATEGORY_LAUNCHER);
                launch.setComponent(item.component);
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(launch);
                } catch (Exception ignored) {
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        alive = false;
        super.onDestroy();
    }

    private List<Item> load() {
        PackageManager manager = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved = manager.queryIntentActivities(intent, 0);
        List<Item> items = new ArrayList<Item>();
        String self = getPackageName();
        for (ResolveInfo info : resolved) {
            if (info.activityInfo == null || self.equals(info.activityInfo.packageName)) {
                continue;
            }
            ComponentName component = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
            CharSequence label = info.loadLabel(manager);
            Drawable icon;
            try {
                icon = info.loadIcon(manager);
            } catch (Exception ignored) {
                icon = null;
            }
            items.add(new Item(label == null ? info.activityInfo.packageName : label.toString(), component, icon));
        }
        Collections.sort(items, new Comparator<Item>() {
            @Override
            public int compare(Item left, Item right) {
                return left.label.compareToIgnoreCase(right.label);
            }
        });
        return items;
    }

    private static final class Item {
        final String label;
        final ComponentName component;
        final Drawable icon;

        Item(String label, ComponentName component, Drawable icon) {
            this.label = label;
            this.component = component;
            this.icon = icon;
        }
    }

    private final class AppAdapter extends BaseAdapter {
        private final List<Item> items;

        AppAdapter(List<Item> items) {
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Item getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.item_app, parent, false);
            }
            Item item = items.get(position);
            ImageView icon = view.findViewById(R.id.appIcon);
            TextView label = view.findViewById(R.id.appLabel);
            icon.setImageDrawable(item.icon);
            label.setText(item.label);
            return view;
        }
    }
}
