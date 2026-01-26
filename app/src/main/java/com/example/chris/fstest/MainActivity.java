package com.example.chris.fstest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.chris.fstest.schedule.ScheduleConfig;

import java.util.Calendar;

public class MainActivity extends Activity {

    private WebView web;
    private TextView status;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(30,30,30,30);

        status = new TextView(this);
        status.setPadding(0,0,0,20);

        final CheckBox tankFill = new CheckBox(this);
        tankFill.setText("Tank Fill Today");

        final CheckBox swap = new CheckBox(this);
        swap.setText("Force MF / Society Swap");

        web = new WebView(this);
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(false);
        ws.setDefaultTextEncodingName("utf-8");

        tankFill.setChecked(ScheduleConfig.tankFillToday(this));
        swap.setChecked(ScheduleConfig.forceSwapMF(this));

        tankFill.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean c) {
                ScheduleConfig.setTankFillToday(MainActivity.this, c);
                render();
            }
        });

        swap.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean c) {
                ScheduleConfig.setForceSwapMF(MainActivity.this, c);
                render();
            }
        });

        root.addView(status);
        root.addView(tankFill);
        root.addView(swap);
        root.addView(web);

        setContentView(root);
        render();
    }

    private void render() {
        updateStatus();

        Calendar today = Calendar.getInstance();
        Calendar tomorrow = (Calendar) today.clone();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);

        boolean tf = ScheduleConfig.tankFillToday(this);
        boolean fs = ScheduleConfig.forceSwapMF(this);

        String html =
            "<html><head><meta charset=utf-8>" +
            "<style>" +
            "body{font-family:sans-serif;padding:10px;}" +
            "h2{margin-top:20px;}" +
            ".slot{padding:8px;margin:6px 0;border-radius:6px;background:#f2f2f2;}" +
            ".active{background:#c8f7c5;font-weight:bold;}" +
            "</style></head><body>" +
            buildDay("TODAY", today, tf, fs) +
            buildDay("TOMORROW", tomorrow, tf, fs) +
            "</body></html>";

        web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    }

    private String buildDay(String title, Calendar day, boolean tf, boolean fs) {
        int d = day.get(Calendar.DAY_OF_MONTH);
        boolean firstHalf = d <= 15;

        String source = firstHalf ? "બોરવેલ" : "નર્મદા";
        if (fs) source = "નર્મદા";

        StringBuilder sb = new StringBuilder();
        sb.append("<h2>").append(title).append("</h2>");
        sb.append("<div>Source: ").append(source).append("</div>");

        sb.append(slot("Morning", "06:00 - 07:30"));
        sb.append(slot("Afternoon", "13:00 - 14:30"));

        if (tf) sb.append("<div class=slot>🚰 Tank Fill Mode</div>");

        return sb.toString();
    }

    private String slot(String name, String time) {
        return "<div class=slot><b>" + name + "</b><br/>" + time + "</div>";
    }

    private void updateStatus() {
        final boolean acc = isAccessibilityEnabled();

        String text =
            "Accessibility: " + (acc ? "ENABLED ✓" : "DISABLED ⚠️ (Tap)") + "\\n" +
            "Notifications: ENABLED ✓ (Tap)";

        status.setText(text);
        status.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (!acc) {
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                } else {
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
}
