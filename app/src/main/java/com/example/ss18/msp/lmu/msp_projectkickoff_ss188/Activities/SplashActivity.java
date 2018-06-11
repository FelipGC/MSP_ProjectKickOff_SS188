package com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.DataBase.LocalDataBase;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.R;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SPLASH_ACTIVITY";

    static final String PREFS_NAME = "preferences_title_id_12345";
    static final String PREF_USER = "preferences_username";
    static final String PREF_IMAGE = "preferences_image";

    private boolean userNameAlreadyEntered = false;
    private boolean userImageAlreadyChosen = false;

    /**
     * ACCESS_COARSE_LOCATION is considered dangerous, so we need to explicitly
     * grant the permission every time we start the app
     */
    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.VIBRATE
            };

    /**
     * Called when our Activity has been made visible to the user.
     * This is only needed for newer devices
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void setRequiredPermissions() {
        Log.i(TAG, "AsetRequiredPermissions()");
        //Check if we have all permissions, if not, then add!
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Requesting permission: " + permission);
                requestPermissions(REQUIRED_PERMISSIONS, 1);
                return;
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setRequiredPermissions();
        }
        super.onCreate(savedInstanceState);

        loadPreferences();
        loadImagePreferences();
        Log.i(TAG, "userNameAlreadyEntered: " + userNameAlreadyEntered);
        if (!userNameAlreadyEntered) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("newUser", true);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            Toast.makeText(getApplicationContext(),
                    String.format("Welcome back %s!", LocalDataBase.getUserName()),
                    Toast.LENGTH_LONG).show();
        }
        finish();
    }

    /**
     * Loads the username form the preferences
     */
    private void loadPreferences() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);
        // Set username if already existing
        if (userNameAlreadyEntered = settings.contains(PREF_USER)) {
            LocalDataBase.setUserName(settings.getString(PREF_USER, LocalDataBase.getUserName()));
            Log.i(TAG, "Load username: " + LocalDataBase.getUserName());
        }
    }

    /**
     * Loads the username form the preferences
     */
    private void loadImagePreferences() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);
        // Set username if already existing
        if (userImageAlreadyChosen = settings.contains(PREF_IMAGE)) {
            String imageUri = settings.getString(PREF_IMAGE, String.valueOf(LocalDataBase.getProfilePictureUri()));
            LocalDataBase.setProfilePictureUri(Uri.parse(imageUri));
            Log.i(TAG, "Load user image: " + imageUri);
        }
    }

    /**
     * Called when the user has accepted (or denied) our permission request.
     */
    @CallSuper
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, R.string.missingPermission, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Permission error");
                    finish();
                    return;
                }
            }
            recreate();
        }
    }
}
