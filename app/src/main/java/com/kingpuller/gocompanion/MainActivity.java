package com.kingpuller.gocompanion;

import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.kingpuller.gocompanion.helper.Constants;
import com.kingpuller.gocompanion.service.ScanService;

import hotchemi.android.rate.AppRate;


public class MainActivity extends AppCompatActivity {

    private Button poGoDetectorButton;
    private TextView serviceIndicator;
    private boolean serviceRunning = false;
    private static int IGNORE_DOZE = 1;

    private void checkBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            SharedPreferences sharedPreferences = getSharedPreferences(BuildConfig.APPLICATION_ID, 0);
            if (sharedPreferences.getBoolean("batteryReminderDisabled", false)) return;
            assert pm != null;
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.battery_dialog_message))
                        .setPositiveButton(getString(R.string.battery_dialog_positive), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                openBatteryOptimizationSettings();
                            }
                        })
                        .setNegativeButton(getString(R.string.battery_dialog_negative), null)
                        .setNeutralButton(getString(R.string.battery_dialog_neutral), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SharedPreferences sharedPreferences = getSharedPreferences(BuildConfig.APPLICATION_ID, 0);
                                sharedPreferences.edit().putBoolean("batteryReminderDisabled", true).apply();
                            }
                        });

                try {
                    alertDialogBuilder.show();
                } catch (Exception e) {
                    // WindowManager$BadTokenException will be caught and the app would not display
                    // the 'Force Close' message
                }

            }
        }
    }


    private void openBatteryOptimizationSettings() {
        //Instantiate a new intent to show ignore battery optimizations settings
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            try {
                startActivityForResult(intent, IGNORE_DOZE);
            } catch (ActivityNotFoundException ex) {
                try {
                    Toast.makeText(this, getString(R.string.battery_dialog_error), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    // WindowManager$BadTokenException will be caught and the app would not display
                    // the 'Force Close' message
                }
            }
        }
        //set to the intent the package name to configure on the settings section

    }


    public void setServiceRunning(boolean serviceRunning) {
        this.serviceRunning = serviceRunning;
        if (serviceRunning) {
            poGoDetectorButton.setText(R.string.stop_service);
            serviceIndicator.setText(this.getString(R.string.service_running));
        } else {
            poGoDetectorButton.setText(R.string.start_service);
            serviceIndicator.setText(this.getString(R.string.service_not_running));
        }
    }


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, Constants.ADMOB_ID);
        checkBatteryOptimizations();

        this.poGoDetectorButton = findViewById(R.id.button_pogo_detector);
        this.serviceIndicator = findViewById(R.id.tv_indicator);

        AppRate.with(this)
                .setInstallDays(3) // default 10, 0 means install day.
                .setLaunchTimes(2) // default 10
                .setRemindInterval(2) // default 1
                .setShowLaterButton(true) // default true
                .setDebug(false) // default false
                .monitor();

        // Show a dialog if meets conditions
        AppRate.showRateDialogIfMeetsConditions(this);

        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        this.poGoDetectorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!serviceRunning) {
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
                        startForegroundService(new Intent(getBaseContext(), ScanService.class));
                    } else {
                        startService(new Intent(getBaseContext(), ScanService.class));
                    }
                    setServiceRunning(true);
                } else {
                    stopService(new Intent(getBaseContext(), ScanService.class));
                    setServiceRunning(false);
                }
            }

        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // launch settings activity
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    protected void onPostResume() {
        super.onPostResume();
        setServiceRunning(isServiceRunning(ScanService.class));
    }

    // https://stackoverflow.com/questions/600207/how-to-check-if-a-service-is-running-on-android
    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
