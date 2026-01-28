package com.example.chris.fstest;

import android.app.Activity;
import android.os.Bundle;
import android.provider.Settings;
import android.content.Intent;
import android.content.SharedPreferences;
import android.webkit.WebView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Button;

import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;

public class MainActivity extends Activity {

    private static final String PREFS = "flags";
    private static final String KEY_TANK = "TANK_FILL_TODAY";
    private static final String KEY_SWAP = "MF_SOCIETY_FORCE_SWAP";

    WebView web;
    Switch tankSwitch, swapSwitch;
    TextView accStatus, notifStatus;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        web = (WebView) findViewById(R.id.webView);
        tankSwitch = (Switch) findViewById(R.id.switchTank);
        swapSwitch = (Switch) findViewById(R.id.switchSwap);
        accStatus = (TextView) findViewById(R.id.textAcc);
        notifStatus = (TextView) findViewById(R.id.textNotif);

        Button accBtn = (Button) findViewById(R.id.btnAcc);
        Button notifBtn = (Button) findViewById(R.id.btnNotif);

        final SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);

        tankSwitch.setChecked(p.getBoolean(KEY_TANK, false));
        swapSwitch.setChecked(p.getBoolean(KEY_SWAP, false));

        tankSwitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton b, boolean v) {
                p.edit().putBoolean(KEY_TANK, v).apply();
                render();
            }
        });

        swapSwitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton b, boolean v) {
                p.edit().putBoolean(KEY_SWAP, v).apply();
                render();
            }
        });

        accBtn.setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        );

        notifBtn.setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()))
        );

        render();
    }

    private void render() {
        updateStatus();
        web.loadData(buildHtml(), "text/html; charset=utf-8", "utf-8");
    }

    private void updateStatus() {
        boolean acc = false;
        try {
            acc = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED) == 1;
        } catch (Exception e) {}

        accStatus.setText("Accessibility: " + (acc ? "ENABLED ✓" : "DISABLED ⚠"));

        notifStatus.setText("Notifications: ENABLED ✓");
    }

    private String buildHtml() {
        Calendar now = Calendar.getInstance();
        boolean tank = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(KEY_TANK, false);
        boolean swap = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(KEY_SWAP, false);

        String todaySrc = swap ? "નર્મદા" : "બોરવેલ";
        String tomSrc = swap ? "બોરવેલ" : "નર્મદા";

        return "<html><body style=padding:16px;font-family:sans-serif>" +
                "<h2>📅 આજે</h2>" +
                "<b>Source:</b> " + todaySrc +
                "<ul>" +
                slot("06:00 – 07:30", tank ? "ટાંકી ભરાઈ રહી છે" : "યાદવ નગરી + ચૌધરી ફરીયો") +
                slot("07:30 – 09:00", "મફત નગરી") +
                slot("09:00 – 12:00", "બાકીનો વિસ્તાર") +
                "</ul>" +
                "<h2>📅 આવતીકાલે</h2>" +
                "<b>Source:</b> " + tomSrc +
                "<ul>" +
                slot("06:00 – 07:30", "યાદવ નગરી + મફત નગરી") +
                slot("07:30 – 09:00", "સોસાયટી") +
                slot("09:00 – 12:00", "બાકીનો વિસ્તાર") +
                "</ul>" +
                "</body></html>";
    }

    private String slot(String t, String l) {
        return "<li><b>" + t + "</b> — " + l + "</li>";
    }
}
