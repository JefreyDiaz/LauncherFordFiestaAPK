package com.fiesta.launcher;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Locale;

public class SettingsActivity extends Activity {
    private static final int REQ_PHOTO = 21;

    private EditText etName;
    private EditText etPlate;
    private EditText etDetail;
    private EditText etKm;
    private EditText etTires;
    private EditText etMon;
    private EditText etTue;
    private EditText etWed;
    private EditText etThu;
    private EditText etFri;
    private EditText etHours;
    private TextView oilDate;
    private String oilDateIso = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppTheme.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        bind();
        load();
        wire();
    }

    @Override
    protected void onPause() {
        save();
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PHOTO && resultCode == RESULT_OK && data != null && data.getData() != null) {
            savePhoto(data.getData());
        }
    }

    private void bind() {
        etName = findViewById(R.id.etName);
        etPlate = findViewById(R.id.etPlate);
        etDetail = findViewById(R.id.etDetail);
        etKm = findViewById(R.id.etKm);
        etTires = findViewById(R.id.etTires);
        etMon = findViewById(R.id.etMon);
        etTue = findViewById(R.id.etTue);
        etWed = findViewById(R.id.etWed);
        etThu = findViewById(R.id.etThu);
        etFri = findViewById(R.id.etFri);
        etHours = findViewById(R.id.etHours);
        oilDate = findViewById(R.id.oilDate);
    }

    private void load() {
        SharedPreferences prefs = Prefs.of(this);
        etName.setText(prefs.getString(Prefs.NAME, ""));
        etPlate.setText(prefs.getString(Prefs.PLATE, ""));
        etDetail.setText(prefs.getString(Prefs.DETAIL, ""));
        etTires.setText(prefs.getString(Prefs.TIRES, ""));
        etHours.setText(prefs.getString(Prefs.HOURS, ""));
        int km = prefs.getInt(Prefs.OIL_KM, -1);
        etKm.setText(km < 0 ? "" : String.valueOf(km));
        oilDateIso = prefs.getString(Prefs.OIL_DATE, "");
        showOilDate();
        String[] days = PicoPlaca.days(prefs.getString(Prefs.PICO, PicoPlaca.DEFAULT));
        etMon.setText(days[0]);
        etTue.setText(days[1]);
        etWed.setText(days[2]);
        etThu.setText(days[3]);
        etFri.setText(days[4]);
    }

    private void wire() {
        oilDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickDate();
            }
        });
        findViewById(R.id.btnClearOil).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                oilDateIso = "";
                etKm.setText("");
                Prefs.of(SettingsActivity.this).edit().remove(Prefs.OIL_DATE).putInt(Prefs.OIL_KM, -1).apply();
                showOilDate();
            }
        });
        findViewById(R.id.btnPhoto).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent, getString(R.string.photo)), REQ_PHOTO);
            }
        });
        findViewById(R.id.btnPhotoClear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = new File(getFilesDir(), "car.jpg");
                if (file.exists()) {
                    file.delete();
                }
            }
        });
        findViewById(R.id.btnNotifications).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            }
        });
        findViewById(R.id.btnLocation).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissions(new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                }, 1);
            }
        });
        findViewById(R.id.btnHome).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
                } catch (Exception ignored) {
                    startActivity(new Intent(Settings.ACTION_SETTINGS));
                }
            }
        });
        findViewById(R.id.btnAndroidSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });
        findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
                finish();
            }
        });
    }

    private void pickDate() {
        Calendar calendar = Calendar.getInstance();
        if (oilDateIso != null && oilDateIso.length() >= 8) {
            try {
                String[] parts = oilDateIso.split("-");
                calendar.set(Calendar.YEAR, Integer.parseInt(parts[0]));
                calendar.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
                calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
            } catch (Exception ignored) {
            }
        }
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                android.R.style.Theme_DeviceDefault_Light_Dialog_Alert,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(android.widget.DatePicker view, int year, int month, int day) {
                        oilDateIso = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
                        Prefs.of(SettingsActivity.this).edit().putString(Prefs.OIL_DATE, oilDateIso).apply();
                        showOilDate();
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private int themeColor(int attr) {
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(attr, value, true);
        return value.data;
    }

    private void showOilDate() {
        if (oilDateIso == null || oilDateIso.length() < 8) {
            oilDate.setText(R.string.pick_date);
            oilDate.setTextColor(themeColor(R.attr.colorInkDim));
            return;
        }
        oilDate.setTextColor(themeColor(R.attr.colorInk));
        try {
            String[] parts = oilDateIso.split("-");
            oilDate.setText(parts[2] + "/" + parts[1] + "/" + parts[0]);
        } catch (Exception ignored) {
            oilDate.setText(oilDateIso);
        }
    }

    private void save() {
        int km = -1;
        try {
            String raw = text(etKm);
            if (!raw.isEmpty()) {
                km = Integer.parseInt(raw);
            }
        } catch (NumberFormatException ignored) {
            km = -1;
        }
        Prefs.of(this).edit()
                .putString(Prefs.NAME, text(etName))
                .putString(Prefs.PLATE, text(etPlate).toUpperCase(Locale.ROOT))
                .putString(Prefs.DETAIL, text(etDetail))
                .putString(Prefs.TIRES, etTires.getText().toString().trim())
                .putString(Prefs.HOURS, text(etHours))
                .putString(Prefs.PICO, PicoPlaca.join(text(etMon), text(etTue), text(etWed), text(etThu), text(etFri)))
                .putInt(Prefs.OIL_KM, km)
                .putString(Prefs.OIL_DATE, oilDateIso == null ? "" : oilDateIso)
                .apply();
    }

    private void savePhoto(Uri uri) {
        File temp = new File(getCacheDir(), "pick.jpg");
        InputStream input = null;
        try {
            input = getContentResolver().openInputStream(uri);
            if (input == null) {
                Toast.makeText(this, R.string.photo_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            FileOutputStream output = new FileOutputStream(temp);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            output.close();
            input.close();
            input = null;
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(temp.getAbsolutePath(), bounds);
            int sample = 1;
            int largest = Math.max(bounds.outWidth, bounds.outHeight);
            while (largest / sample > 1280) {
                sample *= 2;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            Bitmap raw = BitmapFactory.decodeFile(temp.getAbsolutePath(), options);
            if (raw == null) {
                Toast.makeText(this, R.string.photo_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            Bitmap rotated = rotate(raw, temp);
            File dest = new File(getFilesDir(), "car.jpg");
            FileOutputStream photo = new FileOutputStream(dest);
            rotated.compress(Bitmap.CompressFormat.JPEG, 85, photo);
            photo.close();
            if (rotated != raw) {
                raw.recycle();
            }
            rotated.recycle();
        } catch (Exception ignored) {
            Toast.makeText(this, R.string.photo_failed, Toast.LENGTH_SHORT).show();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception ignored) {
                }
            }
            temp.delete();
        }
    }

    private Bitmap rotate(Bitmap source, File file) {
        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int degrees;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degrees = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degrees = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degrees = 270;
                    break;
                default:
                    return source;
            }
            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);
            return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        } catch (Exception ignored) {
            return source;
        }
    }

    private String text(EditText field) {
        return field.getText().toString().trim();
    }
}
