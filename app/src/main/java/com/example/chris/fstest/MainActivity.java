package com.example.chris.fstest;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.TextView;
import android.view.ViewGroup;
import android.graphics.Color;
import android.app.AlertDialog;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    
    private WebView webView;
    private TextView tvNextAlert;
    private SharedPreferences prefs;
    private LinearLayout notifContainer;
    private boolean notifExpanded = false;
    
    private static final String PREFS_NAME = "TankSchedulePrefs";
    private static final String KEY_TANK_FILL_TEMP = "tank_fill_today_temp";
    private static final String KEY_TANK_FILL_PERSIST = "tank_fill_persist";
    private static final String KEY_SWAP_MS = "mf_society_swap";
    private static final String KEY_MORNING_ALERT = "morning_alert_enabled";
    private static final String KEY_MORNING_SOURCE = "morning_source_pref";
    private static final String KEY_AREA_ALERT = "area_alert_enabled";
    private static final String KEY_AREA_YADAV = "area_yadav";
    private static final String KEY_AREA_MAFAT = "area_mafat";
    private static final String KEY_AREA_SOCIETY = "area_society";
    private static final String KEY_AREA_REMAINING = "area_remaining";
    private static final String KEY_MINUTES_BEFORE = "minutes_before";
    private static final String KEY_NOTIF_EXPANDED = "notif_expanded";
    
    private static final int REQ_CODE_MORNING = 1000;
    private static final int REQ_CODE_AREA_BASE = 2000;
    
    private static final String[] SOURCE_LABELS = {"બંને (Both)", "માત્ર નર્મદા (Only Narmada)", "માત્ર બોરવેલ (Only Borewell)"};
    
    private static final Calendar SEED_DATE;
    static {
        SEED_DATE = Calendar.getInstance();
        SEED_DATE.set(2025, Calendar.AUGUST, 29, 0, 0, 0);
        SEED_DATE.set(Calendar.MILLISECOND, 0);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        notifExpanded = prefs.getBoolean(KEY_NOTIF_EXPANDED, false);
        
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.BLACK);
        
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.BLACK);
        mainLayout.setPadding(16, 16, 16, 16);
        
        // Title
        TextView title = new TextView(this);
        title.setText("પાણી સમયપત્રક");
        title.setTextSize(24);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 24);
        mainLayout.addView(title);
        
        // Schedule Settings Card
        LinearLayout settingsCard = createCard();
        addCardTitle(settingsCard, "⚙️ સમયપત્રક સેટિંગ્સ");
        
        CheckBox cbSwap = createCheckBox("મફત/સોસાયટી અદલાબદલી (Swap)");
        cbSwap.setChecked(prefs.getBoolean(KEY_SWAP_MS, false));
        cbSwap.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_SWAP_MS, isChecked).apply();
                refreshAll();
            }
        });
        settingsCard.addView(cbSwap);
        
        CheckBox cbTankPersist = createCheckBox("ટાંકી ભરવાની મોડ (Persistent)");
        cbTankPersist.setChecked(prefs.getBoolean(KEY_TANK_FILL_PERSIST, false));
        cbTankPersist.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_TANK_FILL_PERSIST, isChecked).apply();
                refreshAll();
            }
        });
        settingsCard.addView(cbTankPersist);
        
        CheckBox cbTankTemp = createCheckBox("ટાંકો ભરાઈ રહ્યો છે (Today Only)");
        cbTankTemp.setChecked(prefs.getBoolean(KEY_TANK_FILL_TEMP, false));
        cbTankTemp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_TANK_FILL_TEMP, isChecked).apply();
                refreshAll();
            }
        });
        settingsCard.addView(cbTankTemp);
        
        mainLayout.addView(settingsCard);
        
        // Spacer
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 16));
        mainLayout.addView(spacer);
        
        // WebView for schedules - FIXED HEIGHT
        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(false);
        ws.setSupportZoom(false);
        
        LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        webParams.setMargins(0, 0, 0, 16);
        webView.setLayoutParams(webParams);
        
        mainLayout.addView(webView);
        
        // Notification Settings Expandable Section
        final Button btnToggleNotif = new Button(this);
        btnToggleNotif.setText(notifExpanded ? "🔼 Hide Notifications" : "🔽 Notification Settings");
        btnToggleNotif.setTextColor(Color.WHITE);
        btnToggleNotif.setBackgroundColor(Color.parseColor("#1a1a1a"));
        btnToggleNotif.setTextSize(16);
        btnToggleNotif.setPadding(16, 24, 16, 24);
        btnToggleNotif.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notifExpanded = !notifExpanded;
                prefs.edit().putBoolean(KEY_NOTIF_EXPANDED, notifExpanded).apply();
                notifContainer.setVisibility(notifExpanded ? View.VISIBLE : View.GONE);
                btnToggleNotif.setText(notifExpanded ? "🔼 Hide Notifications" : "🔽 Notification Settings");
            }
        });
        mainLayout.addView(btnToggleNotif);
        
        // Notification Container (Expandable)
        notifContainer = createCard();
        notifContainer.setVisibility(notifExpanded ? View.VISIBLE : View.GONE);
        addCardTitle(notifContainer, "🔔 એલર્ટ સેટિંગ્સ");
        
        // Morning Alert
        CheckBox cbMorning = createCheckBox("સવારના 7:45 નો એલર્ટ");
        cbMorning.setChecked(prefs.getBoolean(KEY_MORNING_ALERT, false));
        cbMorning.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_MORNING_ALERT, isChecked).apply();
                scheduleMorningAlert(isChecked);
                refreshAll();
            }
        });
        notifContainer.addView(cbMorning);
        
        // Source Selection
        TextView lblSource = createLabel("પાણીનો સ્ત્રોત:");
        notifContainer.addView(lblSource);
        
        Spinner spnSource = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
            android.R.layout.simple_spinner_item, SOURCE_LABELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnSource.setAdapter(adapter);
        spnSource.setBackgroundColor(Color.parseColor("#2a2a2a"));
        spnSource.setSelection(prefs.getInt(KEY_MORNING_SOURCE, 0));
        spnSource.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt(KEY_MORNING_SOURCE, position).apply();
                ((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
                scheduleMorningAlert(prefs.getBoolean(KEY_MORNING_ALERT, false));
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        notifContainer.addView(spnSource);
        
        addDivider(notifContainer);
        
        // Area Selection Header
        TextView lblAreas = createLabel("તમારા વિસ્તારો (Select areas):");
        notifContainer.addView(lblAreas);
        
        // Area Checkboxes
        CheckBox cbYadav = createCheckBox("યાદવ નગરી + ચૌધરી ફરીયો");
        cbYadav.setChecked(prefs.getBoolean(KEY_AREA_YADAV, false));
        cbYadav.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_AREA_YADAV, isChecked).apply();
                scheduleAreaAlerts();
            }
        });
        notifContainer.addView(cbYadav);
        
        CheckBox cbMafat = createCheckBox("મફત નગરી");
        cbMafat.setChecked(prefs.getBoolean(KEY_AREA_MAFAT, false));
        cbMafat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_AREA_MAFAT, isChecked).apply();
                scheduleAreaAlerts();
            }
        });
        notifContainer.addView(cbMafat);
        
        CheckBox cbSociety = createCheckBox("સોસાયટી");
        cbSociety.setChecked(prefs.getBoolean(KEY_AREA_SOCIETY, false));
        cbSociety.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_AREA_SOCIETY, isChecked).apply();
                scheduleAreaAlerts();
            }
        });
        notifContainer.addView(cbSociety);
        
        CheckBox cbRemaining = createCheckBox("બાકીનો વિસ્તાર");
        cbRemaining.setChecked(prefs.getBoolean(KEY_AREA_REMAINING, false));
        cbRemaining.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_AREA_REMAINING, isChecked).apply();
                scheduleAreaAlerts();
            }
        });
        notifContainer.addView(cbRemaining);
        
        // Minutes Before
        LinearLayout minutesRow = new LinearLayout(this);
        minutesRow.setOrientation(LinearLayout.HORIZONTAL);
        minutesRow.setPadding(0, 16, 0, 0);
        
        TextView lblMin = createLabel("મિનિટ પહેલાં: ");
        minutesRow.addView(lblMin);
        
        EditText etMinutes = new EditText(this);
        etMinutes.setText(String.valueOf(prefs.getInt(KEY_MINUTES_BEFORE, 15)));
        etMinutes.setTextColor(Color.WHITE);
        etMinutes.setBackgroundColor(Color.parseColor("#2a2a2a"));
        etMinutes.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etMinutes.setPadding(16, 16, 16, 16);
        etMinutes.setLayoutParams(new LinearLayout.LayoutParams(150, ViewGroup.LayoutParams.WRAP_CONTENT));
        etMinutes.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                try {
                    int val = Integer.parseInt(s.toString());
                    prefs.edit().putInt(KEY_MINUTES_BEFORE, val).apply();
                    scheduleAreaAlerts();
                } catch (Exception e) {}
            }
        });
        minutesRow.addView(etMinutes);
        notifContainer.addView(minutesRow);
        
        // Next Alert Info
        tvNextAlert = new TextView(this);
        tvNextAlert.setTextSize(12);
        tvNextAlert.setTextColor(Color.GRAY);
        tvNextAlert.setPadding(0, 16, 0, 0);
        notifContainer.addView(tvNextAlert);
        
        mainLayout.addView(notifContainer);
        
        // Keyboard Button
        Button btnKeyboard = new Button(this);
        btnKeyboard.setText("⌨️ Keyboard Service Settings");
        btnKeyboard.setTextColor(Color.WHITE);
        btnKeyboard.setBackgroundColor(Color.parseColor("#1a1a1a"));
        btnKeyboard.setTextSize(16);
        btnKeyboard.setPadding(16, 24, 16, 24);
        btnKeyboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });
        mainLayout.addView(btnKeyboard);
        
        scrollView.addView(mainLayout);
        setContentView(scrollView);
        
        refreshAll();
    }
    
    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#121212"));
        card.setPadding(16, 16, 16, 16);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        return card;
    }
    
    private void addCardTitle(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16);
        tv.setTextColor(Color.WHITE);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(0, 0, 0, 16);
        parent.addView(tv);
    }
    
    private CheckBox createCheckBox(String text) {
        CheckBox cb = new CheckBox(this);
        cb.setText(text);
        cb.setTextColor(Color.WHITE);
        cb.setTextSize(14);
        cb.setPadding(8, 12, 8, 12);
        return cb;
    }
    
    private TextView createLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.LTGRAY);
        tv.setTextSize(14);
        tv.setPadding(0, 8, 0, 8);
        return tv;
    }
    
    private void addDivider(LinearLayout parent) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 1));
        v.setBackgroundColor(Color.parseColor("#333333"));
        v.setPadding(0, 16, 0, 16);
        parent.addView(v);
    }
    
    private void refreshAll() {
        webView.loadDataWithBaseURL(null, getScheduleHTML(), "text/html; charset=UTF-8", "UTF-8", null);
        updateNextAlertInfo();
    }
    
    private void updateNextAlertInfo() {
        StringBuilder sb = new StringBuilder();
        
        if (prefs.getBoolean(KEY_MORNING_ALERT, false)) {
            sb.append("• સવારે 7:45 (Morning Alert)\n");
        }
        
        boolean anyArea = prefs.getBoolean(KEY_AREA_YADAV, false) || 
                         prefs.getBoolean(KEY_AREA_MAFAT, false) ||
                         prefs.getBoolean(KEY_AREA_SOCIETY, false) || 
                         prefs.getBoolean(KEY_AREA_REMAINING, false);
        
        if (anyArea) {
            int mins = prefs.getInt(KEY_MINUTES_BEFORE, 15);
            sb.append("• Area alerts (").append(mins).append(" min before)\n");
        }
        
        if (sb.length() == 0) {
            sb.append("કોઈ એલર્ટ સેટ નથી");
        }
        
        tvNextAlert.setText(sb.toString());
    }
    
    private void scheduleMorningAlert(boolean enable) {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.setAction("MORNING_ALERT");
        
        PendingIntent pi = PendingIntent.getBroadcast(this, REQ_CODE_MORNING, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        if (!enable) {
            if (am != null) am.cancel(pi);
            return;
        }
        
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 7);
        cal.set(Calendar.MINUTE, 45);
        cal.set(Calendar.SECOND, 0);
        
        if (cal.getTimeInMillis() < System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        if (am != null) {
            if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            }
        }
    }
    
    private void scheduleAreaAlerts() {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        
        // Cancel all existing area alarms
        for (int i = 0; i < 10; i++) {
            Intent intent = new Intent(this, NotificationReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(this, REQ_CODE_AREA_BASE + i, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            if (am != null) am.cancel(pi);
        }
        
        int minsBefore = prefs.getInt(KEY_MINUTES_BEFORE, 15);
        Calendar now = Calendar.getInstance();
        int todayDay = now.get(Calendar.DAY_OF_MONTH);
        long diffMillis = now.getTimeInMillis() - SEED_DATE.getTimeInMillis();
        int days = (int) (diffMillis / (1000 * 60 * 60 * 24));
        
        boolean firstHalf = todayDay <= 15;
        boolean swap = prefs.getBoolean(KEY_SWAP_MS, false);
        boolean tankFill = prefs.getBoolean(KEY_TANK_FILL_TEMP, false) || 
                          prefs.getBoolean(KEY_TANK_FILL_PERSIST, false);
        
        String first = (days % 2 == 0) ? "society" : "mafat";
        if (swap) first = first.equals("society") ? "mafat" : "society";
        String second = first.equals("society") ? "mafat" : "society";
        
        int alarmIdx = 0;
        
        // Generate slots and check each against selected areas
        if (firstHalf) {
            if (tankFill) {
                // 06:00 - Remaining
                if (prefs.getBoolean(KEY_AREA_REMAINING, false)) {
                    alarmIdx = scheduleIfPossible(am, now, 6, 0, minsBefore, "બાકીનો વિસ્તાર", alarmIdx);
                }
                // 11:00 - Yadav + First
                if (prefs.getBoolean(KEY_AREA_YADAV, false)) {
                    String label = "યાદવ નગરી" + (first.equals("society") ? " + સોસાયટી" : " + મફત નગરી");
                    alarmIdx = scheduleIfPossible(am, now, 11, 0, minsBefore, label, alarmIdx);
                }
                // 12:30 - Second
                if (second.equals("society") && prefs.getBoolean(KEY_AREA_SOCIETY, false)) {
                    alarmIdx = scheduleIfPossible(am, now, 12, 30, minsBefore, "સોસાયટી", alarmIdx);
                } else if (second.equals("mafat") && prefs.getBoolean(KEY_AREA_MAFAT, false)) {
                    alarmIdx = scheduleIfPossible(am, now, 12, 30, minsBefore, "મફત નગરી", alarmIdx);
                }
            } else {
                // 06:00 - Remaining
                if (prefs.getBoolean(KEY_AREA_REMAINING, false)) {
                    alarmIdx = scheduleIfPossible(am, now, 6, 0, minsBefore, "બાકીનો વિસ્તાર", alarmIdx);
                }
                // 09:00 - First
                if (first.equals("society") && prefs.getBoolean(KEY_AREA_SOCIETY, false)) {
                    alarmIdx = scheduleIfPossible(am, now, 9, 0, minsBefore, "સોસાયટી", alarmIdx);
                } else if (first.equals("mafat") && prefs.getBoolean(KEY_AREA_MAFAT, false)) {
                    alarmIdx = scheduleIfPossible(am, now, 9, 0, minsBefore, "મફત નગરી", alarmIdx);
                }
                // 10:30 - Second
                if (second.equals("society") && prefs.getBoolean(KEY_AREA_SOCIETY, false)) {
                    alarmIdx = scheduleIfPossible(am, now, 10, 30, minsBefore, "સોસાયટી", alarmIdx);
                } else if (second.equals("mafat") && prefs.getBoolean(KEY_AREA_MAFAT, false)) {
                    alarmIdx = scheduleIfPossible(am, now, 10, 30, minsBefore, "મફત નગરી", alarmIdx);
                }
                // 12:00 - Yadav
                if (prefs.getBoolean(KEY_AREA_YADAV, false)) {
                    alarmIdx = scheduleIfPossible(am, now, 12, 0, minsBefore, "યાદવ નગરી", alarmIdx);
                }
            }
        } else {
            // Second half of month
            // 06:00 - Yadav + First
            if (prefs.getBoolean(KEY_AREA_YADAV, false)) {
                alarmIdx = scheduleIfPossible(am, now, 6, 0, minsBefore, "યાદવ નગરી", alarmIdx);
            }
            if (first.equals("society") && prefs.getBoolean(KEY_AREA_SOCIETY, false)) {
                alarmIdx = scheduleIfPossible(am, now, 6, 0, minsBefore, "સોસાયટી (06:00)", alarmIdx);
            } else if (first.equals("mafat") && prefs.getBoolean(KEY_AREA_MAFAT, false)) {
                alarmIdx = scheduleIfPossible(am, now, 6, 0, minsBefore, "મફત નગરી (06:00)", alarmIdx);
            }
            // 07:30 - Second
            if (second.equals("society") && prefs.getBoolean(KEY_AREA_SOCIETY, false)) {
                alarmIdx = scheduleIfPossible(am, now, 7, 30, minsBefore, "સોસાયટી", alarmIdx);
            } else if (second.equals("mafat") && prefs.getBoolean(KEY_AREA_MAFAT, false)) {
                alarmIdx = scheduleIfPossible(am, now, 7, 30, minsBefore, "મફત નગરી", alarmIdx);
            }
            // 09:00 - Remaining
            if (prefs.getBoolean(KEY_AREA_REMAINING, false)) {
                alarmIdx = scheduleIfPossible(am, now, 9, 0, minsBefore, "બાકીનો વિસ્તાર", alarmIdx);
            }
        }
    }
    
    private int scheduleIfPossible(AlarmManager am, Calendar now, int hour, int minute, 
                                   int minsBefore, String areaName, int alarmIdx) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.add(Calendar.MINUTE, -minsBefore);
        
        if (cal.getTimeInMillis() > now.getTimeInMillis() && alarmIdx < 10) {
            Intent intent = new Intent(this, NotificationReceiver.class);
            intent.setAction("AREA_ALERT");
            intent.putExtra("area", areaName);
            intent.putExtra("time", String.format("%02d:%02d", hour, minute));
            intent.putExtra("notif_id", REQ_CODE_AREA_BASE + alarmIdx);
            
            PendingIntent pi = PendingIntent.getBroadcast(this, REQ_CODE_AREA_BASE + alarmIdx, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            
            if (am != null) {
                if (Build.VERSION.SDK_INT >= 23) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                }
            }
            return alarmIdx + 1;
        }
        return alarmIdx;
    }
    
    private String getScheduleHTML() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'>");
        // AMOLED Dark CSS - Pure black background
        html.append("<style>");
        html.append("body{font-family:system-ui;background:#000000;padding:0;margin:0;color:#ffffff}");
        html.append(".card{background:#121212;border:1px solid #333333;border-radius:12px;padding:16px;margin-bottom:16px}");
        html.append("h2{font-size:18px;margin:0 0 12px 0;color:#ffffff;font-weight:bold}");
        html.append(".badge{color:#4fc3f7;background:#0d2b3a;padding:4px 10px;border-radius:6px;font-size:13px;font-weight:600;display:inline-block;margin-bottom:8px}");
        html.append(".date{color:#aaaaaa;font-size:13px;margin:8px 0 12px 0}");
        html.append(".slot{display:flex;padding:12px 0;border-top:1px solid #333333}");
        html.append(".slot:first-child{border-top:none}");
        html.append(".time{min-width:90px;font-weight:600;color:#4fc3f7;font-size:14px}");
        html.append(".label{flex:1;color:#ffffff;font-size:14px;padding-left:8px}");
        html.append(".note{background:#1a1a1a;padding:12px;border-radius:8px;font-size:12px;color:#888888;margin-top:12px;border-left:3px solid #4fc3f7}");
        html.append("</style></head><body>");
        
        html.append(getDayCardHtml(true));
        html.append(getDayCardHtml(false));
        html.append("</body></html>");
        return html.toString();
    }
    
    private String getDayCardHtml(boolean isToday) {
        Calendar cal = Calendar.getInstance();
        if (!isToday) cal.add(Calendar.DAY_OF_MONTH, 1);
        
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        long diff = (cal.getTimeInMillis() - SEED_DATE.getTimeInMillis()) / (1000*60*60*24);
        int days = (int) diff;
        
        boolean isBorewell = (days % 2 == 0);
        String source = isBorewell ? "બોરવેલ (Borewell)" : "નર્મદા (Narmada)";
        boolean firstHalf = dayOfMonth <= 15;
        boolean swap = prefs.getBoolean(KEY_SWAP_MS, false);
        
                boolean tankFill;
        if (isToday) {
            tankFill = prefs.getBoolean(KEY_TANK_FILL_TEMP, false) || 
                      prefs.getBoolean(KEY_TANK_FILL_PERSIST, false);
        } else {
            tankFill = prefs.getBoolean(KEY_TANK_FILL_PERSIST, false);
        }
        
        String first = (days % 2 == 0) ? "society" : "mafat";
        if (swap) first = first.equals("society") ? "mafat" : "society";
        String second = first.equals("society") ? "mafat" : "society";
        String firstLabel = first.equals("society") ? "સોસાયટી" : "મફત નગરી";
        String secondLabel = second.equals("society") ? "સોસાયટી" : "મફત નગરી";
        
        StringBuilder slots = new StringBuilder();
        
        if (firstHalf) {
            if (tankFill) {
                addSlotHtml(slots, "06:00–09:00", "બાકીનો વિસ્તાર");
                addSlotHtml(slots, "09:00–11:00", "ટાંકો ભરાઈ રહ્યો છે");
                addSlotHtml(slots, "11:00–12:30", "યાદવ નગરી + " + firstLabel);
                addSlotHtml(slots, "12:30–14:00", secondLabel);
            } else {
                addSlotHtml(slots, "06:00–09:00", "બાકીનો વિસ્તાર");
                addSlotHtml(slots, "09:00–10:30", firstLabel);
                addSlotHtml(slots, "10:30–12:00", secondLabel);
                addSlotHtml(slots, "12:00–13:30", "યાદવ નગરી");
            }
        } else {
            addSlotHtml(slots, "06:00–07:30", "યાદવ નગરી + " + firstLabel);
            addSlotHtml(slots, "07:30–09:00", secondLabel);
            addSlotHtml(slots, "09:00–12:00", "બાકીનો વિસ્તાર");
        }
        
        String dateStr = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH).format(cal.getTime());
        String title = isToday ? "📅 આજે (Today)" : "📅 આવતીકાલે (Tomorrow)";
        
        StringBuilder card = new StringBuilder();
        card.append("<div class='card'><h2>").append(title).append("</h2>");
        card.append("<span class='badge'>").append(source).append("</span>");
        card.append("<div class='date'>").append(dateStr).append("</div>");
        card.append(slots);
        card.append("<div class='note'>ℹ️ બાકીનો વિસ્તાર = વથાણ ચોક, બજાર ચોક અને નજીકના વિસ્તારો</div></div>");
        
        return card.toString();
    }
    
    private void addSlotHtml(StringBuilder sb, String time, String label) {
        sb.append("<div class='slot'><div class='time'>").append(time).append("</div><div class='label'>").append(label).append("</div></div>");
    }
}

