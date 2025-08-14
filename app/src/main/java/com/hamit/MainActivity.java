package com.hamit;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.Patterns;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private TextView permissionStatus;
    private EditText deviceNameInput;
    private EditText emailNameInput;
    private CheckBox checkboxRunOnStartup;

    private static final String PREFS_NAME = "GPSPrefs";
    private static final String KEY_DEVICE_NAME = "device_name";
    private static final String KEY_EMAIL_NAME = "email_name";
    private static final String KEY_RUN_ON_STARTUP = "runOnStartup";

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!isAutoStartAvailable()) {

            LogUtils.logToFile(this, "UYARI","Otomatik başlatma menüsü bu cihazda mevcut değil!");

            Toast.makeText(this, "Bu cihazda otomatik başlatma menüsü bulunamadı. Uygulamayı elle başlatmalısınız.", Toast.LENGTH_LONG).show();
        }
        Log.d("GPSService", "Program Başladı");

        permissionStatus = findViewById(R.id.permissionStatus);
        deviceNameInput = findViewById(R.id.deviceNameInput);
        emailNameInput = findViewById(R.id.emailNameInput);
        checkboxRunOnStartup = findViewById(R.id.checkboxRunOnStartup);



        String savedDeviceName = prefs.getString(KEY_DEVICE_NAME, "");
        String savedEmailName = prefs.getString(KEY_EMAIL_NAME, "");
        boolean savedRunOnStartup = prefs.getBoolean(KEY_RUN_ON_STARTUP, false);

        deviceNameInput.setText(savedDeviceName);
        emailNameInput.setText(savedEmailName);
        checkboxRunOnStartup.setChecked(savedRunOnStartup);

        Button btnSaveDeviceName = findViewById(R.id.btnSaveDeviceName);
        btnSaveDeviceName.setOnClickListener(v -> {
            Animation scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up);
            v.startAnimation(scaleUp);

            String name = deviceNameInput.getText().toString().trim();
            String email = emailNameInput.getText().toString().trim();
            boolean runOnStartup = checkboxRunOnStartup.isChecked();
            if (name.isEmpty()) {
                Toast.makeText(this, "Lütfen cihaz adı girin!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (email.isEmpty()) {
                Toast.makeText(this, "Lütfen e-posta adresi girin!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Geçerli bir e-posta adresi girin!", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.edit()
                    .putString(KEY_DEVICE_NAME, name)
                    .putString(KEY_EMAIL_NAME, email)
                    .putBoolean(KEY_RUN_ON_STARTUP, runOnStartup)
                    .apply();
            Toast.makeText(this, "Cihaz adı, e-posta ve başlangıç ayarı kaydedildi", Toast.LENGTH_SHORT).show();
        });
        Button btnExit = findViewById(R.id.btnExit);
        btnExit.setOnClickListener(v -> {
            stopService(new Intent(MainActivity.this, GPSService.class));
            finishAffinity();
        });

        Button btnMinimize = findViewById(R.id.btnMinimize);
        btnMinimize.setOnClickListener(v -> moveTaskToBack(true));

        updatePermissionStatus();
        checkAndStartService();

    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences("GPSPrefs", MODE_PRIVATE);
        boolean runOnStartup = prefs.getBoolean("runOnStartup", false);
        if (runOnStartup) {
            boolean serviceRunning = isServiceRunning(GPSService.class);
            if (!serviceRunning) {
                Intent intent = new Intent(this, GPSService.class);
                startForegroundService(intent);
                Log.d("MainActivity", "onResume ile servis manuel başlatıldı");
            }
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void checkAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (hasFineLocationPermission()) {
                if (hasBackgroundLocationPermission()) {
                    if (Build.VERSION.SDK_INT >= 34 && !hasForegroundLocationPermission()) {
                        requestForegroundLocationPermission();
                        return;
                    }
                    startGPSServiceIfValid();
                } else {
                    showPermissionSettingsDialog();
                }
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            if (hasFineLocationPermission()) {
                startGPSServiceIfValid();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void startGPSServiceIfValid() {
        String deviceName = deviceNameInput.getText().toString().trim();
        String emailName = emailNameInput.getText().toString().trim();

        if (deviceName.isEmpty() || emailName.isEmpty()) {
            permissionStatus.setText("⚠ Lütfen cihaz adı ve e-posta girin!");
            return;
        }

        prefs.edit()
                .putString(KEY_DEVICE_NAME, deviceName)
                .putString(KEY_EMAIL_NAME, emailName)
                .apply();

        Intent serviceIntent = new Intent(this, GPSService.class);
        serviceIntent.putExtra("device_name", deviceName);
        serviceIntent.putExtra("email_name", emailName);
        startService(serviceIntent);

        Log.d("GPSService", "Servis Başlatıldı");
        Toast.makeText(this, "GPS servisi başlatıldı", Toast.LENGTH_SHORT).show();
    }

    private boolean hasFineLocationPermission() {
        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasBackgroundLocationPermission() {
        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasForegroundLocationPermission() {
        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestForegroundLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.FOREGROUND_SERVICE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        updatePermissionStatus();
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (hasFineLocationPermission()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (!hasBackgroundLocationPermission()) {
                        showPermissionSettingsDialog();
                        return;
                    }
                    if (Build.VERSION.SDK_INT >= 34 && !hasForegroundLocationPermission()) {
                        requestForegroundLocationPermission();
                        return;
                    }
                }
                startGPSServiceIfValid();
            } else {
                Log.e("GPSService", "Fine Location reddedildi");
            }
        }
    }

    private void updatePermissionStatus() {
        StringBuilder sb = new StringBuilder("İzin Durumu:\n");
        if (hasFineLocationPermission()) sb.append("✓ Fine Location\n");
        else sb.append("✗ Fine Location\n");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (hasBackgroundLocationPermission()) sb.append("✓ Background Location\n");
            else sb.append("✗ Background Location\n");
        }
        if (Build.VERSION.SDK_INT >= 34) {
            if (hasForegroundLocationPermission()) sb.append("✓ Foreground Service Location\n");
            else sb.append("✗ Foreground Service Location\n");
        }
        if (permissionStatus != null) {
            permissionStatus.setText(sb.toString());
        }
    }

    private void showPermissionSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Gerekli İzinler ve Ayarlar")
                .setMessage("Bu uygulamanın arka planda doğru şekilde çalışabilmesi için aşağıdaki ayarları yapmanız gerekmektedir:\n\n"
                        + "1. 'Ayarları Aç' butonuna tıklayın\n"
                        + "2. Açılan ekranda 'İzinler' bölümüne girin\n"
                        + "3. 'Konum' seçeneğine dokunun\n"
                        + "4. 'Her zaman izin ver' seçeneğini seçin\n\n"
                        + "5. Pil > Pil optimizasyonu > Arka plan etkinliği → 'Sınırsız' seçeneğini seçin")
                .setPositiveButton("Ayarları Aç", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private boolean isAutoStartAvailable() {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            startActivity(intent);
            List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            return list != null && list.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
