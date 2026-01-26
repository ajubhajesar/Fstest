package com.example.chris.fstest;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView tv = new TextView(this);
        tv.setPadding(40, 40, 40, 40);
        tv.setTextSize(16f);
        setContentView(tv);

        boolean accessibilityEnabled = isAccessibilityEnabled();
        boolean notificationEnabled = isNotificationEnabled();

        tv.setText(
            "Accessibility: " + (accessibilityEnabled ? "ENABLED" : "DISABLED") + "\\n"
          + "Notifications: " + (notificationEnabled ? "ENABLED" : "DISABLED")
        );

        if (!accessibilityEnabled) {
            showAccessibilityDialog();
        } else if (!notificationEnabled) {
            showNotificationDialog();
        }
    }

    private boolean isAccessibilityEnabled() {
        try {
            String enabled = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            return enabled != null && enabled.contains(getPackageName());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isNotificationEnabled() {
        if (Build.VERSION.SDK_INT < 33) return true;
        return checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private void showAccessibilityDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Enable Accessibility")
            .setMessage("Accessibility is required for Enter-to-Send to work.")
            .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) {
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showNotificationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Enable Notifications")
            .setMessage("Notifications show when Enter-to-Send is active.")
            .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) {
                    Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    i.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    startActivity(i);
                }
            })
            .setNegativeButton("Later", null)
            .show();
    }
}
