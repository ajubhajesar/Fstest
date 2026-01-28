package com.example.chris.fstest;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Switch;
import android.widget.CompoundButton;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.content.Intent;
import android.widget.TextView;
import android.widget.Button;

public class MainActivity extends Activity {

    private static final String PREFS = "flags";
    private static final String KEY_TANK = "TANK_FILL_TODAY";
    private static final String KEY_SWAP = "MF_SOCIETY_FORCE_SWAP";

    private WebView web;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        web = (WebView) findViewById(R.id.webView);

        final Switch tank = (Switch) findViewById(R.id.switchTank);
        final Switch swap = (Switch) findViewById(R.id.switchSwap);

        TextView acc = (TextView) findViewById(R.id.textAcc);
        TextView noti = (TextView) findViewById(R.id.textNotif);

        Button accBtn = (Button) findViewById(R.id.btnAcc);
        Button notiBtn = (Button) findViewById(R.id.btnNotif);

        final SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);

        tank.setChecked(p.getBoolean(KEY_TANK, false));
        swap.setChecked(p.getBoolean(KEY_SWAP, false));

        tank.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean v) {
                p.edit().putBoolean(KEY_TANK, v).apply();
                render();
            }
        });

        swap.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean v) {
                p.edit().putBoolean(KEY_SWAP, v).apply();
                render();
            }
        });

        boolean accEnabled = false;
        try {
            accEnabled =
                Settings.Secure.getInt(
                    getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED
                ) == 1;
        } catch (Exception e) {}

        acc.setText("Accessibility: " + (accEnabled ? "ENABLED ✓" : "DISABLED ⚠"));
        noti.setText("Notifications: ENABLED ✓");

        accBtn.setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        );

        notiBtn.setOnClickListener(v ->
            startActivity(
                new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())
            )
        );

        render();
    }

    private void render() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);

        ScheduleData.DaySchedule today =
                ScheduleData.today(
                        p.getBoolean(KEY_TANK, false),
                        p.getBoolean(KEY_SWAP, false)
                );

        ScheduleData.DaySchedule tomorrow =
                ScheduleData.tomorrow(
                        p.getBoolean(KEY_SWAP, false)
                );

        StringBuilder h = new StringBuilder();
        h.append("<html><body style=\"font-family:sans-serif;padding:16px\">");

        draw(h, today);
        draw(h, tomorrow);

        h.append("</body></html>");

        web.loadDataWithBaseURL(null, h.toString(), "text/html", "utf-8", null);
    }

    private void draw(StringBuilder h, ScheduleData.DaySchedule d) {
        h.append("<h2>").append(d.title).append("</h2>");
        h.append("<b>").append(d.source).append("</b><br/><br/>");

        for (int i = 0; i < d.slots.size(); i++) {
            ScheduleData.Slot s = d.slots.get(i);
            if (s.active) {
                h.append("<div style=\"background:#e0f2ff;padding:6px;border-radius:6px\">");
            }
            h.append("<b>").append(s.time).append("</b><br/>");
            h.append(s.area).append("<br/>");
            if (s.active) {
                h.append("</div>");
            }
            h.append("<br/>");
        }
    }
}
