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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
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
    
    private static final String PREFS_NAME = "TankSchedulePrefs";
    private static final String KEY_TANK_FILL_TEMP = "tank_fill_today_temp";
    private static final String KEY_TANK_FILL_PERSIST = "tank_fill_persist";
    private static final String KEY_SWAP_MS = "mf_society_swap";
    private static final String KEY_MORNING_ALERT = "morning_alert_enabled";
    private static final String KEY_MORNING_SOURCE = "morning_source_pref";
    private static final String KEY_AREA_ALERT = "area_alert_enabled";
    private static final String KEY_MY_AREA = "my_area";
    private static final String KEY_MINUTES_BEFORE = "minutes_before";
    
    private static final int REQ_CODE_MORNING = 1000;
    private static final int REQ_CODE_AREA_BASE = 2000;
    
    private static final String[] AREAS = {"Yadav", "Mafat", "Society", "Remaining", "All Areas"};
    private static final String[] AREA_LABELS = {
        "યાદવ નગરી + ચૌધરી ફરીયો", 
        "મફત નગરી", 
        "સોસાયટી", 
        "બાકીનો વિસ્તાર",
        "બધા વિસ્તારો"
    };
    private static final String[] SOURCES = {"Both", "Narmada", "Borewell"};
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
        
        ScrollView scrollView = new ScrollView(this);
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#f5f5f5"));
        mainLayout.setPadding(16, 16, 16, 16);
        
        // Title
        TextView title = new TextView(this);
        title.setText("પાણી સમયપત્રક સેટિંગ્સ");
        title.setTextSize(22);
        title.setTextColor(Color.parseColor("#222222"));
        title.setPadding(0, 0, 0, 16);
        mainLayout.addView(title);
        
        // Settings Panel
        LinearLayout settingsPanel = new LinearLayout(this);
        settingsPanel.setOrientation(LinearLayout.VERTICAL);
        settingsPanel.setBackgroundColor(Color.WHITE);
        settingsPanel.setPadding(16, 16, 16, 16);
        settingsPanel.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        
        // Section: Schedule Settings
        addSectionTitle(settingsPanel, "સમયપત્રક સેટિંગ્સ (Schedule Settings)");
        
        // Persistent Swap
        CheckBox cbSwap = new CheckBox(this);
        cbSwap.setText("મફત/સોસાયટી અદલાબદલી (Persistent Swap)");
        cbSwap.setChecked(prefs.getBoolean(KEY_SWAP_MS, false));
        cbSwap.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_SWAP_MS, isChecked).apply();
                refreshAll();
            }
        });
        settingsPanel.addView(cbSwap);
        
        // Persistent Tank Fill
        CheckBox cbTankPersist = new CheckBox(this);
        cbTankPersist.setText("ટાંકી ભરવાની મોડ (Persistent - All days)");
        cbTankPersist.setChecked(prefs.getBoolean(KEY_TANK_FILL_PERSIST, false));
        cbTankPersist.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_TANK_FILL_PERSIST, isChecked).apply();
                refreshAll();
            }
        });
        settingsPanel.addView(cbTankPersist);
        
        // Temporary Tank Fill (Today only)
        CheckBox cbTankTemp = new CheckBox(this);
        cbTankTemp.setText("ટાંકો ભરાઈ રહ્યો છે (Today only)");
        cbTankTemp.setChecked(prefs.getBoolean(KEY_TANK_FILL_TEMP, false));
        cbTankTemp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_TANK_FILL_TEMP, isChecked).apply();
                refreshAll();
            }
        });
        settingsPanel.addView(cbTankTemp);
        
        addDivider(settingsPanel);
        
        // Section: Notifications
        addSectionTitle(settingsPanel, "નોટિફિકેશન સેટિંગ્સ (Notifications)");
        
        // Morning Alert
        CheckBox cbMorning = new CheckBox(this);
        cbMorning.setText("સવારના 7:45 નો એલર્ટ (Morning Source Alert)");
        cbMorning.setChecked(prefs.getBoolean(KEY_MORNING_ALERT, false));
        cbMorning.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && !checkNotificationPermissions()) {
                    buttonView.setChecked(false);
                    return;
                }
                prefs.edit().putBoolean(KEY_MORNING_ALERT, isChecked).apply();
                scheduleMorningAlert(isChecked);
                refreshAll();
            }
        });
        settingsPanel.addView(cbMorning);
        
        // Morning Source Spinner
        Spinner spnMorningSource = new Spinner(this);
        ArrayAdapter<String> sourceAdapter = new ArrayAdapter<String>(this, 
            android.R.layout.simple_spinner_item, SOURCE_LABELS);
        sourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnMorningSource.setAdapter(sourceAdapter);
        spnMorningSource.setSelection(prefs.getInt(KEY_MORNING_SOURCE, 0));
        spnMorningSource.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt(KEY_MORNING_SOURCE, position).apply();
                if (prefs.getBoolean(KEY_MORNING_ALERT, false)) {
                    scheduleMorningAlert(true);
                }
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        settingsPanel.addView(spnMorningSource);
        
        // Next Alert Info
        tvNextAlert = new TextView(this);
        tvNextAlert.setTextSize(12);
        tvNextAlert.setTextColor(Color.parseColor("#666666"));
        tvNextAlert.setPadding(0, 8, 0, 0);
        settingsPanel.addView(tvNextAlert);
        
        addDivider(settingsPanel);
        
        // My Area Alert
        CheckBox cbArea = new CheckBox(this);
        cbArea.setText("મારા વિસ્તારનો એલર્ટ (My Area Alert)");
        cbArea.setChecked(prefs.getBoolean(KEY_AREA_ALERT, false));
        cbArea.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && !checkNotificationPermissions()) {
                    buttonView.setChecked(false);
                    return;
                }
                prefs.edit().putBoolean(KEY_AREA_ALERT, isChecked).apply();
                scheduleAreaAlerts(isChecked);
                refreshAll();
            }
        });
        settingsPanel.addView(cbArea);
        
        // Area selection
        Spinner spnArea = new Spinner(this);
        ArrayAdapter<String> areaAdapter = new ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_item, AREA_LABELS);
        areaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnArea.setAdapter(areaAdapter);
        spnArea.setSelection(prefs.getInt(KEY_MY_AREA, 0));
        spnArea.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt(KEY_MY_AREA, position).apply();
                scheduleAreaAlerts(prefs.getBoolean(KEY_AREA_ALERT, false));
                refreshAll();
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        settingsPanel.addView(spnArea);
        
        // Minutes before
        LinearLayout minutesRow = new LinearLayout(this);
        minutesRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView lblMin = new TextView(this);
        lblMin.setText("મિનિટ પહેલાં: ");
        lblMin.setTextSize(14);
        minutesRow.addView(lblMin);
        
        EditText etMinutes = new EditText(this);
        etMinutes.setText(String.valueOf(prefs.getInt(KEY_MINUTES_BEFORE, 15)));
        etMinutes.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etMinutes.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                try {
                    int val = Integer.parseInt(s.toString());
                    prefs.edit().putInt(KEY_MINUTES_BEFORE, val).apply();
                    scheduleAreaAlerts(prefs.getBoolean(KEY_AREA_ALERT, false));
                    refreshAll();
                } catch (Exception e) {}
            }
        });
        etMinutes.setLayoutParams(new LinearLayout.LayoutParams(150, ViewGroup.LayoutParams.WRAP_CONTENT));
        minutesRow.addView(etMinutes);
        settingsPanel.addView(minutesRow);
        
        mainLayout.addView(settingsPanel);
        
        // WebView for schedules (Today + Tomorrow)
        LinearLayout webContainer = new LinearLayout(this);
        webContainer.setOrientation(LinearLayout.VERTICAL);
        webContainer.setPadding(0, 16, 0, 0);
        webContainer.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1.0f
        ));
        
        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(false);
        ws.setSupportZoom(false);
        webView.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        webContainer.addView(webView);
        mainLayout.addView(webContainer);
        
        // Keyboard Button
        Button btn = new Button(this);
        btn.setText("⌨️ Keyboard Service Settings");
        btn.setTextSize(16);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });
        mainLayout.addView(btn);
        
        scrollView.addView(mainLayout);
        setContentView(scrollView);
        
        // Initial load
        refreshAll();
    }
    
    private void addSectionTitle(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16);
        tv.setTextColor(Color.parseColor("#0066cc"));
        tv.setPadding(0, 16, 0, 8);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        parent.addView(tv);
    }
    
    private void addDivider(LinearLayout parent) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 1));
        v.setBackgroundColor(Color.LTGRAY);
        v.setPadding(0, 16, 0, 16);
        parent.addView(v);
    }
    
    private boolean checkNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
                return false;
            }
        }
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            startActivity(intent);
            showMessage("કૃપા કરીને એકઝેક્ટ એલાર્મની પરવાનગી આપો (Please allow exact alarms)");
            return false;
        }
        return true;
    }
    
    private void showMessage(String msg) {
        new AlertDialog.Builder(this)
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show();
    }
    
    private void refreshAll() {
        webView.loadDataWithBaseURL(null, getScheduleHTML(), "text/html; charset=UTF-8", "UTF-8", null);
        updateNextAlertInfo();
    }
    
    private void updateNextAlertInfo() {
        StringBuilder sb = new StringBuilder("આગામી એલર્ટ:\n");
        
        if (prefs.getBoolean(KEY_MORNING_ALERT, false)) {
            sb.append("• સવારે 7:45 (Morning 7:45 AM)\n");
        }
        
        if (prefs.getBoolean(KEY_AREA_ALERT, false)) {
            int areaIdx = prefs.getInt(KEY_MY_AREA, 0);
            int mins = prefs.getInt(KEY_MINUTES_BEFORE, 15);
            String areaName = AREA_LABELS[areaIdx];
            sb.append("• ").append(areaName).append(" (").append(mins).append(" min before)\n");
        }
        
        if (!prefs.getBoolean(KEY_MORNING_ALERT, false) && !prefs.getBoolean(KEY_AREA_ALERT, false)) {
            sb.append("કોઈ એલર્ટ સેટ નથી (No alerts set)");
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            }
        }
    }
    
    private void scheduleAreaAlerts(boolean enable) {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        
        // Cancel existing area alerts
        for (int i = 0; i < 4; i++) {
            Intent intent = new Intent(this, NotificationReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(this, REQ_CODE_AREA_BASE + i, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            if (am != null) am.cancel(pi);
        }
        
        if (!enable) return;
        
        int areaIdx = prefs.getInt(KEY_MY_AREA, 0);
        int minsBefore = prefs.getInt(KEY_MINUTES_BEFORE, 15);
        String targetArea = AREAS[areaIdx]; // "Yadav", "Mafat", etc.
        
        // Calculate today's times
        Calendar now = Calendar.getInstance();
        int todayDay = now.get(Calendar.DAY_OF_MONTH);
        int daysSince = (int)((now.getTimeInMillis() - SEED_DATE.getTimeInMillis()) / (1000*60*60*24));
        
        boolean firstHalf = todayDay <= 15;
        boolean isBorewell = (daysSince % 2 == 0);
        boolean swap = prefs.getBoolean(KEY_SWAP_MS, false);
        boolean tankFillToday = prefs.getBoolean(KEY_TANK_FILL_TEMP, false) || 
                               prefs.getBoolean(KEY_TANK_FILL_PERSIST, false);
        
        String first = (daysSince % 2 == 0) ? "society" : "mafat";
        if (swap) first = first.equals("society") ? "mafat" : "society";
        String second = first.equals("society") ? "mafat" : "society";
        
        int[] startHours = new int[4];
        int[] startMins = new int[4];
        String[] slotAreas = new String[4]; // Which areas covered in this slot
        
        if (firstHalf) {
            if (tankFillToday) {
                // 06:00, 09:00, 11:00, 12:30
                startHours[0] = 6; startMins[0] = 0; slotAreas[0] = "remaining";
                startHours[1] = 9; startMins[1] = 0; slotAreas[1] = "tank"; // skip or special
                startHours[2] = 11; startMins[2] = 0; slotAreas[2] = "yadav_" + first;
                startHours[3] = 12; startMins[3] = 30; slotAreas[3] = second;
            } else {
                // 06:00, 09:00, 10:30, 12:00
                startHours[0] = 6; startMins[0] = 0; slotAreas[0] = "remaining";
                startHours[1] = 9; startMins[1] = 0; slotAreas[1] = first;
                startHours[2] = 10; startMins[2] = 30; slotAreas[2] = second;
                startHours[3] = 12; startMins[3] = 0; slotAreas[3] = "yadav";
            }
        } else {
            // 06:00, 07:30, 09:00
            startHours[0] = 6; startMins[0] = 0; slotAreas[0] = "yadav_" + first;
            startHours[1] = 7; startMins[1] = 30; slotAreas[1] = second;
            startHours[2] = 9; startMins[2] = 0; slotAreas[2] = "remaining";
            startHours[3] = -1; // unused
        }
        
        int alarmIdx = 0;
        for (int i = 0; i < 4 && startHours[i] >= 0 && alarmIdx < 3; i++) {
            if (matchesArea(slotAreas[i], targetArea)) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, startHours[i]);
                cal.set(Calendar.MINUTE, startMins[i]);
                cal.set(Calendar.SECOND, 0);
                cal.add(Calendar.MINUTE, -minsBefore);
                
                if (cal.getTimeInMillis() > now.getTime
