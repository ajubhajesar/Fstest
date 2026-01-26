package com.example.chris.fstest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.chris.fstest.schedule.ScheduleConfig;
import com.example.chris.fstest.schedule.TankSchedule;

import java.util.Calendar;

public class MainActivity extends Activity {

    private TextView output;
    private TextView status;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40,40,40,40);

        status = new TextView(this);
        status.setPadding(0,0,0,30);

        final CheckBox tankFill = new CheckBox(this);
        tankFill.setText("Tank Fill Today");

        final CheckBox swap = new CheckBox(this);
        swap.setText("Force MF / Society Swap");

        output = new TextView(this);
        output.setTextSize(15f);

        tankFill.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean c) {
                ScheduleConfig.setTankFillToday(MainActivity.this, c);
                refresh();
            }
        });

        swap.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean c) {
                ScheduleConfig.setForceSwapMF(MainActivity.this, c);
                refresh();
            }
        });

        root.addView(status);
        root.addView(tankFill);
        root.addView(swap);
        root.addView(output);

        sv.addView(root);
        setContentView(sv);

        tankFill.setChecked(ScheduleConfig.tankFillToday(this));
        swap.setChecked(ScheduleConfig.forceSwapMF(this));

        refresh();
    }

    private void refresh() {
        updateStatus();

        Calendar today = Calendar.getInstance();
        Calendar tomorrow = (Calendar) today.clone();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);

        boolean tf = ScheduleConfig.tankFillToday(this);
        boolean fs = ScheduleConfig.forceSwapMF(this);

        String text =
            "📅 TODAY\n" +
            TankSchedule.build(today, tf, fs) +
            "\n\n📅 TOMORROW\n" +
            TankSchedule.build(tomorrow, tf, fs);

        output.setText(text);
    }

    private void updateStatus() {
        boolean acc = isAccessibilityEnabled();
        boolean notif = areNotificationsEnabled();

        StringBuilder sb = new StringBuilder();

        sb.append("Accessibility: ")
          .append(acc ? "ENABLED ✓" : "DISABLED ⚠️ (Tap to enable)")
          .append("\n");

        sb.append("Notifications: ")
          .append(notif ? "ENABLED ✓" : "DISABLED ⚠️ (Tap to enable)")
          .append("\n\n");

        status.setText(sb.toString());

        status.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (!acc) {
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                } else if (!notif) {
                    startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
                }
            }
        });
    }

    private boolean isAccessibilityEnabled() {
        String enabled = Settings.Secure.getString(
            getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabled != null && enabled.contains(getPackageName());
    }

    private boolean areNotificationsEnabled() {
        return getApplicationInfo().targetSdkVersion < 33 ||
               checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
               == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }
}
