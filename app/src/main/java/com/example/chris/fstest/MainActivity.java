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
        
        TextView title = new TextView(this);
        title.setText("પાણી સમયપત્રક સેટિંગ્સ");
        title.setTextSize(22);
        title.setTextColor(Color.parseColor("#222222"));
        title.setPadding(0, 0, 0, 16);
        mainLayout.addView(title);
        
        LinearLayout settingsPanel = new LinearLayout(this);
        settingsPanel.setOrientation(LinearLayout.VERTICAL);
        settingsPanel.setBackgroundColor(Color.WHITE);
        settingsPanel.setPadding(16, 16, 16, 16);
        settingsPanel.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        
        addSectionTitle(settingsPanel, "સમયપત્રક સેટિંગ્સ (Schedule Settings)");
        
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
        addSectionTitle(settingsPanel, "નોટિફિકેશન સેટિંગ્સ (Notifications)");
        
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
        
        tvNextAlert = new TextView(this);
        tvNextAlert.setTextSize(12);
        tvNextAlert.setTextColor(Color.parseColor("#666666"));
        tvNextAlert.setPadding(0, 8, 0, 0);
        settingsPanel.addView(tvNextAlert);
        
        addDivider(settingsPanel);
        
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
        // API 28 compatible - no runtime permission checks needed
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
            if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            }
        }
    }
    
    private void scheduleAreaAlerts(boolean enable) {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        
        for (int i = 0; i < 4; i++) {
            Intent intent = new Intent(this, NotificationReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(this, REQ_CODE_AREA_BASE + i, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            if (am != null) am.cancel(pi);
        }
        
        if (!enable) return;
        
        int areaIdx = prefs.getInt(KEY_MY_AREA, 0);
        int minsBefore = prefs.getInt(KEY_MINUTES_BEFORE, 15);
        String targetArea = AREAS[areaIdx];
        
        Calendar now = Calendar.getInstance();
        int todayDay = now.get(Calendar.DAY_OF_MONTH);
        long diffMillis = now.getTimeInMillis() - SEED_DATE.getTimeInMillis();
        int days = (int) (diffMillis / (1000 * 60 * 60 * 24));
        
        boolean firstHalf = todayDay <= 15;
        boolean swap = prefs.getBoolean(KEY_SWAP_MS, false);
        boolean tankFillToday = prefs.getBoolean(KEY_TANK_FILL_TEMP, false) || 
                               prefs.getBoolean(KEY_TANK_FILL_PERSIST, false);
        
        String first = (days % 2 == 0) ? "society" : "mafat";
        if (swap) first = first.equals("society") ? "mafat" : "society";
        String second = first.equals("society") ? "mafat" : "society";
        
        int[] startHours = new int[4];
        int[] startMins = new int[4];
        String[] slotAreas = new String[4];
        
        if (firstHalf) {
            if (tankFillToday) {
                startHours[0] = 6; startMins[0] = 0; slotAreas[0] = "remaining";
                startHours[1] = 9; startMins[1] = 0; slotAreas[1] = "tank";
                startHours[2] = 11; startMins[2] = 0; slotAreas[2] = "yadav_" + first;
                startHours[3] = 12; startMins[3] = 30; slotAreas[3] = second;
            } else {
                startHours[0] = 6; startMins[0] = 0; slotAreas[0] = "remaining";
                startHours[1] = 9; startMins[1] = 0; slotAreas[1] = first;
                startHours[2] = 10; startMins[2] = 30; slotAreas[2] = second;
                startHours[3] = 12; startMins[3] = 0; slotAreas[3] = "yadav";
            }
        } else {
            startHours[0] = 6; startMins[0] = 0; slotAreas[0] = "yadav_" + first;
            startHours[1] = 7; startMins[1] = 30; slotAreas[1] = second;
            startHours[2] = 9; startMins[2] = 0; slotAreas[2] = "remaining";
            startHours[3] = -1;
        }
        
        int alarmIdx = 0;
        for (int i = 0; i < 4 && startHours[i] >= 0 && alarmIdx < 3; i++) {
            if (matchesArea(slotAreas[i], targetArea)) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, startHours[i]);
                cal.set(Calendar.MINUTE, startMins[i]);
                cal.set(Calendar.SECOND, 0);
                cal.add(Calendar.MINUTE, -minsBefore);
                
                if (cal.getTimeInMillis() > now.getTimeInMillis()) {
                    Intent intent = new Intent(this, NotificationReceiver.class);
                    intent.setAction("AREA_ALERT");
                    intent.putExtra("area", AREA_LABELS[areaIdx]);
                    intent.putExtra("time", String.format("%02d:%02d", startHours[i], startMins[i]));
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
                    alarmIdx++;
                }
            }
        }
    }
    
    private boolean matchesArea(String slot, String target) {
        if (target.equals("All Areas")) return true;
        if (slot.contains(target.toLowerCase())) return true;
        if (target.equals("Remaining") && slot.equals("remaining")) return true;
        if (target.equals("Yadav") && slot.contains("yadav")) return true;
        if (target.equals("Mafat") && slot.equals("mafat")) return true;
        if (target.equals("Society") && slot.equals("society")) return true;
        return false;
    }
    
    private String getScheduleHTML() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'>");
        html.append("<style>body{font-family:system-ui;background:rgb(245,245,245);padding:12px;margin:0}");
        html.append(".card{background:white;border-radius:12px;padding:16px;margin-bottom:12px;box-shadow:0 2px 8px rgba(0,0,0,.08)}");
        html.append("h2{font-size:18px;margin:0 0 12px 0;color:rgb(34,34,34)}.badge{color:rgb(0,102,204);background:rgb(230,243,255);padding:4px 8px;border-radius:6px;font-size:13px;font-weight:600}");
        html.append(".date{color:rgb(102,102,102);font-size:13px;margin:8px 0 12px 0}");
        html.append(".slot{display:flex;padding:10px 0;border-top:1px solid rgb(238,238,238)}.slot:first-child{border-top:none}");
        html.append(".time{min-width:90px;font-weight:600;color:rgb(0,102,204);font-size:14px}");
        html.append(".label{flex:1;color:rgb(51,51,51);font-size:14px}");
        html.append(".note{background:rgb(255,249,230);padding:12px;border-radius:8px;font-size:12px;color:rgb(133,100,4);margin-top:12px}</style></head><body>");
        
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
        card.append("<div class='note'>વીજળી, મોટર સમસ્યાથી સમય બદલાઈ શકે છે</div></div>");
        
        return card.toString();
    }
    
    private void addSlotHtml(StringBuilder sb, String time, String label) {
        sb.append("<div class='slot'><div class='time'>").append(time).append("</div><div class='label'>").append(label).append("</div></div>");
    }
}
