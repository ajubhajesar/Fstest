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
        root.setPadding(30, 30, 30, 30);

        status = new TextView(this);
        status.setPadding(0, 0, 0, 20);

        final CheckBox tankFill = new CheckBox(this);
        tankFill.setText("Tank Fill Today");

        final CheckBox swap = new CheckBox(this);
        swap.setText("Force MF / Society Swap");

        web = new WebView(this);
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(false);
        ws.setDefaultTextEncodingName("utf-8");

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 0);
        lp.weight = 1;
        web.setLayoutParams(lp);

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
                "<html><head><meta charset='utf-8'>" +
                "<style>" +
                "body{font-family:sans-serif;padding:10px;}" +
                "h2{margin-top:20px;}" +
                ".slot{padding:10px;margin:8px 0;border-radius:6px;background:#f2f2f2;}" +
                ".active{background:#c8f7c5;font-weight:bold;}" +
                "</style></head><body>" +
                buildDay("આજે", today, tf, fs) +
                buildDay("કાલે", tomorrow, tf, fs) +
                "</body></html>";

        web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    }

    private String buildDay(String title, Calendar day, boolean tf, boolean fs) {
        Calendar now = Calendar.getInstance();
        int nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        boolean isToday = title.equals("આજે");

        String source = "બોરવેલ";
        if (fs) source = "નર્મદા";

        StringBuilder sb = new StringBuilder();
        sb.append("<h2>").append(title).append("</h2>");
        sb.append("<div><b>સ્ત્રોત:</b> ").append(source).append("</div>");

        if (tf && isToday) {
            sb.append("<div class='slot' style='background:#ffe6b3;font-weight:bold;'>🚰 ટાંકી ભરવાનું કાર્ય</div>");
        }

        sb.append(slot("સવાર", 360, 450, nowMin, isToday));
        sb.append(slot("બપોર", 780, 870, nowMin, isToday));

        return sb.toString();
    }

    private String slot(String name, int start, int end, int nowMin, boolean activeDay) {
        boolean active = activeDay && nowMin >= start && nowMin <= end;
        String cls = active ? "slot active" : "slot";
        return "<div class='" + cls + "'><b>" + name + "</b><br/>" +
                (start / 60) + ":00 - " + (end / 60) + ":30</div>";
    }

    private void updateStatus() {
        final boolean acc = isAccessibilityEnabled();
        status.setText(
                "Accessibility: " + (acc ? "ENABLED ✓" : "DISABLED ⚠️ (Tap)") + "\n" +
                "Notifications: ENABLED ✓"
        );
        status.setLineSpacing(0, 1.3f);

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
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabled != null && enabled.contains(getPackageName());
    }
}
