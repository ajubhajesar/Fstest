package com.example.chris.fstest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView tv = new TextView(this);
        tv.setPadding(40, 40, 40, 40);
        tv.setTextSize(16f);
        setContentView(tv);

        boolean accessibilityEnabled = isAccessibilityEnabled();

        tv.setText(
            "Accessibility: " + (accessibilityEnabled ? "ENABLED" : "DISABLED")
        );

        if (!accessibilityEnabled) {
            showAccessibilityDialog();
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
}
