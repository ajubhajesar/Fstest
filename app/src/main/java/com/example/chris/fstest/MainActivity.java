package com.example.chris.fstest;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import android.widget.Toast;
import android.view.ViewGroup;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    
    private WebView webView;
    private TextView tvNextAlert;
    private SharedPreferences prefs;
    private LinearLayout notifContainer;
    private LinearLayout scheduleContainer;
    private boolean notifExpanded = false;
    private boolean scheduleExpanded = false;
    
    private static final String PREFS_NAME = "TankSchedulePrefs";
    private static final String KEY_TANK_FILL_TEMP = "tank_fill_today_temp";
    private static final String KEY_TANK_FILL_PERSIST = "tank_fill_persist";
    private static final String KEY_SWAP_MS = "mf_society_swap";
    private static final String KEY_MORNING_ALERT = "morning_alert_enabled";
    private static final String KEY_MORNING_SOURCE = "morning_source_pref";
    private static final String KEY_AREA_YADAV = "area_yadav";
    private static final String KEY_AREA_MAFAT = "area_mafat";
    private static final String KEY_AREA_SOCIETY = "area_society";
    private static final String KEY_AREA_REMAINING = "area_remaining";
    private static final String KEY_MINUTES_BEFORE = "minutes_before";
    private static final String KEY_NOTIF_EXPANDED = "notif_expanded";
    private static final String KEY_SCHED_EXPANDED = "sched_expanded";
    
    private static final int REQ_CODE_MORNING = 1000;
    private static final int REQ_CODE_AREA_BASE = 2000;
    private static final String CHANNEL_ID = "water_schedule_channel";
    
    private static final String[] SOURCE_LABELS = {"બંને (Both)", "માત્ર નર્મદા (Only Narmada)", "માત્ર બોરવેલ (Only Borewell)"};
    private static final String[] TEST_TYPES = {"🌅 સવારનો એલર્ટ", "💧 યાદવ નગરી + ચૌધરી ફરીયો", "💧 મફત નગરી", "💧 સોસાયટી", "💧 બાકીનો વિસ્તાર"};
    
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
        scheduleExpanded = prefs.getBoolean(KEY_SCHED_EXPANDED, false);
        
        createNotificationChannel();
        
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.BLACK);
        
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.BLACK);
        mainLayout.setPadding(16, 16, 16, 16);
        
        // WebView for schedules
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
        
        // Spacer
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 16));
        mainLayout.addView(spacer);
        
        // Notification Section (FIRST)
        final Button btnToggleNotif = new Button(this);
        btnToggleNotif.setText(notifExpanded ? "🔼 નોટિફિકેશન સેટિંગ્સ" : "🔽 નોટિફિકેશન સેટિંગ્સ");
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
                btnToggleNotif.setText(notifExpanded ? "🔼 નોટિફિકેશન સેટિંગ્સ" : "🔽 નોટિફિકેશન સેટિંગ્સ");
            }
        });
        mainLayout.addView(btnToggleNotif);
        
        notifContainer = createCard();
        notifContainer.setVisibility(notifExpanded ? View.VISIBLE : View.GONE);
        
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
        ArrayAdapter<String> sourceAdapter = new ArrayAdapter<String>(this, 
            android.R.layout.simple_spinner_item, SOURCE_LABELS);
        sourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnSource.setAdapter(sourceAdapter);
        spnSource.setBackgroundColor(Color.parseColor("#2a2a2a"));
        spnSource.setSelection(prefs.getInt(KEY_MORNING_SOURCE, 0));
        spnSource.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt(KEY_MORNING_SOURCE, position).apply();
                if (parent.getChildAt(0) != null) {
                    ((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
                }
                scheduleMorningAlert(prefs.getBoolean(KEY_MORNING_ALERT, false));
                refreshAll();
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        notifContainer.addView(spnSource);
        
        addDivider(notifContainer);
        
        // Area Selection Header
        TextView lblAreas = createLabel("તમારા વિસ્તારો:");
        notifContainer.addView(lblAreas);
        
        // Area Checkboxes
        CheckBox cbYadav = createCheckBox("યાદવ નગરી + ચૌધરી ફરીયો");
        cbYadav.setChecked(prefs.getBoolean(KEY_AREA_YADAV, false));
        cbYadav.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_AREA_YADAV, isChecked).apply();
                scheduleAreaAlerts();
                refreshAll();
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
                refreshAll();
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
                refreshAll();
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
                refreshAll();
            }
        });
        notifContainer.addView(cbRemaining);
        
        // Minutes Before
        LinearLayout minutesRow = new LinearLayout(this);
        minutesRow.setOrientation(LinearLayout.HORIZONTAL);
        minutesRow.setPadding(0, 16, 0, 0);
        
        TextView lblMin = createLabel("મિનિટ પહેલાં: ");
        minutesRow.addView(lblMin);
        
        final EditText etMinutes = new EditText(this);
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
                    refreshAll();
                } catch (Exception e) {}
            }
        });
        minutesRow.addView(etMinutes);
        notifContainer.addView(minutesRow);
        
        // Test Zone Divider
        addDivider(notifContainer);
        
        // Test Zone Header
        TextView testHeader = createLabel("🧪 ટેસ્ટ ઝોન");
        testHeader.setTextColor(Color.parseColor("#4fc3f7"));
        testHeader.setTextSize(16);
        testHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        notifContainer.addView(testHeader);
        
        // Test Type Selection
        TextView lblTestType = createLabel("ટેસ્ટ મોકલો:");
        notifContainer.addView(lblTestType);
        
        final Spinner spnTestType = new Spinner(this);
        ArrayAdapter<String> testAdapter = new ArrayAdapter<String>(this, 
            android.R.layout.simple_spinner_item, TEST_TYPES);
        testAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnTestType.setAdapter(testAdapter);
        spnTestType.setBackgroundColor(Color.parseColor("#2a2a2a"));
        spnTestType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getChildAt(0) != null) {
                    ((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
                }
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        notifContainer.addView(spnTestType);
        
        // Test Time Input
        LinearLayout testTimeRow = new LinearLayout(this);
        testTimeRow.setOrientation(LinearLayout.HORIZONTAL);
        testTimeRow.setPadding(0, 8, 0, 8);
        
        TextView lblTestTime = createLabel("સમય: ");
        testTimeRow.addView(lblTestTime);
        
        final EditText etTestTime = new EditText(this);
        etTestTime.setText("09:00");
        etTestTime.setTextColor(Color.WHITE);
        etTestTime.setBackgroundColor(Color.parseColor("#2a2a2a"));
        etTestTime.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        etTestTime.setPadding(16, 16, 16, 16);
        etTestTime.setLayoutParams(new LinearLayout.LayoutParams(200, ViewGroup.LayoutParams.WRAP_CONTENT));
        testTimeRow.addView(etTestTime);
        
        notifContainer.addView(testTimeRow);
        
        // Test Button
        Button btnTest = new Button(this);
        btnTest.setText("📲 એલર્ટ મોકલો");
        btnTest.setTextColor(Color.WHITE);
        btnTest.setBackgroundColor(Color.parseColor("#0d2b3a"));
        btnTest.setTextSize(14);
        btnTest.setPadding(16, 20, 16, 20);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendTestNotification(spnTestType.getSelectedItemPosition(), etTestTime.getText().toString());
            }
        });
        notifContainer.addView(btnTest);
        
        // Next Alert Info
        tvNextAlert = new TextView(this);
        tvNextAlert.setTextSize(13);
        tvNextAlert.setTextColor(Color.parseColor("#aaaaaa"));
        tvNextAlert.setPadding(0, 16, 0, 0);
        notifContainer.addView(tvNextAlert);
        
        mainLayout.addView(notifContainer);
        
        // Spacer
        View spacer2 = new View(this);
        spacer2.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 16));
        mainLayout.addView(spacer2);
        
        // Schedule Settings Section (SECOND)
        final Button btnToggleSched = new Button(this);
        btnToggleSched.setText(scheduleExpanded ? "🔼 સમયપત્રક સેટિંગ્સ" : "🔽 સમયપત્રક સેટિંગ્સ");
        btnToggleSched.setTextColor(Color.WHITE);
        btnToggleSched.setBackgroundColor(Color.parseColor("#1a1a1a"));
        btnToggleSched.setTextSize(16);
        btnToggleSched.setPadding(16, 24, 16, 24);
        btnToggleSched.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scheduleExpanded = !scheduleExpanded;
                prefs.edit().putBoolean(KEY_SCHED_EXPANDED, scheduleExpanded).apply();
                scheduleContainer.setVisibility(scheduleExpanded ? View.VISIBLE : View.GONE);
                btnToggleSched.setText(scheduleExpanded ? "🔼 સમયપત્રક સેટિંગ્સ" : "🔽 સમયપત્રક સેટિંગ્સ");
            }
        });
        mainLayout.addView(btnToggleSched);
        
        scheduleContainer = createCard();
        scheduleContainer.setVisibility(scheduleExpanded ? View.VISIBLE : View.GONE);
        
        CheckBox cbSwap = createCheckBox("મફત/સોસાયટી અદલાબદલી (Swap)");
        cbSwap.setChecked(prefs.getBoolean(KEY_SWAP_MS, false));
        cbSwap.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_SWAP_MS, isChecked).apply();
                refreshAll();
            }
        });
        scheduleContainer.addView(cbSwap);
        
        CheckBox cbTankPersist = createCheckBox("ટાંકી ભરવાની મોડ (Persistent)");
        cbTankPersist.setChecked(prefs.getBoolean(KEY_TANK_FILL_PERSIST, false));
        cbTankPersist.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_TANK_FILL_PERSIST, isChecked).apply();
                refreshAll();
            }
        });
        scheduleContainer.addView(cbTankPersist);
        
        CheckBox cbTankTemp = createCheckBox("ટાંકો ભરાઈ રહ્યો છે (Today Only)");
        cbTankTemp.setChecked(prefs.getBoolean(KEY_TANK_FILL_TEMP, false));
        cbTankTemp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_TANK_FILL_TEMP, isChecked).apply();
                refreshAll();
            }
        });
        scheduleContainer.addView(cbTankTemp);
        
        mainLayout.addView(scheduleContainer);
        
        // Spacer
        View spacer3 = new View(this);
        spacer3.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 16));
        mainLayout.addView(spacer3);
        
        // Keyboard Button (THIRD/BOTTOM)
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
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Water Schedule Alerts", 
                NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Water schedule notifications");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }
    
    private void sendTestNotification(int type, String timeStr) {
        // Parse time (HH:mm)
        int hour = 9, minute = 0;
        try {
            String[] parts = timeStr.split(":");
            hour = Integer.parseInt(parts[0]);
            minute = Integer.parseInt(parts[1]);
        } catch (Exception e) {
            Toast.makeText(this, "Invalid time format. Use HH:mm", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        
        // If time already passed today, schedule for 1 minute from now (immediate test)
        if (cal.getTimeInMillis() < System.currentTimeMillis()) {
            cal.add(Calendar.MINUTE, 1);
        }
        
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.setAction("TEST_NOTIFICATION");
        intent.putExtra("test_type", type);
        
        PendingIntent pi = PendingIntent.getBroadcast(this, 9999, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            
        if (am != null) {
            if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            }
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
        Toast.makeText(this, "Test scheduled for " + sdf.format(cal.getTime()), Toast.LENGTH_SHORT).show();
    }
    
    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#121212"));
        card.setPadding(16, 16, 16, 16);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 0);
        card.setLayoutParams(params);
        return card;
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
        tv.setTextColor(Color.parseColor("#aaaaaa"));
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
        StringBuilder passed = new StringBuilder();
        StringBuilder upcoming = new StringBuilder();
        
        boolean hasPassed = false;
        boolean hasUpcoming = false;
        
        Calendar now = Calendar.getInstance();
        int todayDay = now.get(Calendar.DAY_OF_MONTH);
        long diffMillis = now.getTimeInMillis() - SEED_DATE.getTimeInMillis();
        int days = (int) (diffMillis / (1000 * 60 * 60 * 24));
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMin = now.get(Calendar.MINUTE);
        int currentTime = currentHour * 60 + currentMin;
        
        boolean isBorewellToday = (days % 2 == 0);
        boolean firstHalf = todayDay <= 15;
        boolean swap = prefs.getBoolean(KEY_SWAP_MS, false);
        boolean tankFillToday = prefs.getBoolean(KEY_TANK_FILL_TEMP, false) || 
                               prefs.getBoolean(KEY_TANK_FILL_PERSIST, false);
        
        // Calculate today's slots
        String first = (days % 2 == 0) ? "society" : "mafat";
        if (swap) first = first.equals("society") ? "mafat" : "society";
        String second = first.equals("society") ? "mafat" : "society";
        String firstLabel = first.equals("society") ? "સોસાયટી" : "મફત નગરી";
        String secondLabel = second.equals("society") ? "સોસાયટી" : "મફત નગરી";
        
        // Morning alert check (7:45 AM = 465 minutes)
        int morningTime = 7 * 60 + 45;
        boolean morningSelected = prefs.getBoolean(KEY_MORNING_ALERT, false);
        int morningSourcePref = prefs.getInt(KEY_MORNING_SOURCE, 0);
        boolean morningShouldAlert = false;
        
        if (morningSourcePref == 0) morningShouldAlert = true;
        else if (morningSourcePref == 1 && !isBorewellToday) morningShouldAlert = true;
        else if (morningSourcePref == 2 && isBorewellToday) morningShouldAlert = true;
        
        // FIXED: Only show morning in lists if it should alert (no Chinese text)
        if (morningSelected && morningShouldAlert) {
            String source = isBorewellToday ? "બોરવેલ" : "નર્મદા";
            if (currentTime > morningTime) {
                passed.append("✓ 07:45 AM - સવારનો એલર્ટ (").append(source).append(")\n");
                hasPassed = true;
            } else {
                upcoming.append("⏳ 07:45 AM - સવારનો એલર્ટ (").append(source).append(")\n");
                hasUpcoming = true;
            }
        }
        
        // Today's area slots - FIXED: Proper hasPassed/hasUpcoming tracking
        if (firstHalf) {
            if (tankFillToday) {
                // 06:00 - Remaining, 11:00 - Yadav+First, 12:30 - Second
                if (checkSlot(passed, upcoming, currentTime, 6, 0, "બાકીનો વિસ્તાર", 
                    prefs.getBoolean(KEY_AREA_REMAINING, false))) {
                    if (currentTime > 6*60) hasPassed = true; else hasUpcoming = true;
                }
                
                String yadavSlot = "યાદવ નગરી + ચૌધરી ફરીયો";
                boolean yadavSelected = prefs.getBoolean(KEY_AREA_YADAV, false);
                boolean firstSelected = (first.equals("society") && prefs.getBoolean(KEY_AREA_SOCIETY, false)) ||
                                       (first.equals("mafat") && prefs.getBoolean(KEY_AREA_MAFAT, false));
                if ((yadavSelected || firstSelected)) {
                    String label = yadavSlot;
                    if (firstSelected) label += " + " + firstLabel;
                    if (checkSlot(passed, upcoming, currentTime, 11, 0, label, true)) {
                        if (currentTime > 11*60) hasPassed = true; else hasUpcoming = true;
                    }
                }
                
                if ((second.equals("society") && prefs.getBoolean(KEY_AREA_SOCIETY, false)) ||
                    (second.equals("mafat") && prefs.getBoolean(KEY_AREA_MAFAT, false))) {
                    String label = second.equals("society") ? "સોસાયટી" : "મફત નગરી";
                    if (checkSlot(passed, upcoming, currentTime, 12, 30, label, true)) {
                        if (currentTime > (12*60+30)) hasPassed = true; else hasUpcoming = true;
                    }
                }
            } else {
                // 06:00 - Remaining, 09:00 - First, 10:30 - Second, 12:00 - Yadav
                if (checkSlot(passed, upcoming, currentTime, 6, 0, "બાકીનો વિસ્તાર",
                    prefs.getBoolean(KEY_AREA_REMAINING, false))) {
                    if (currentTime > 6*60) hasPassed = true; else hasUpcoming = true;
                }
                
                if ((first.equals("society") && prefs.getBoolean(KEY_AREA_SOCIETY, false)) ||
                    (first.equals("mafat") && prefs.getBoolean(KEY_AREA_MAFAT, false))) {
                    if (checkSlot(passed, upcoming, currentTime, 9, 0, firstLabel, true)) {
                        if (currentTime > 9*60) hasPassed = true; else hasUpcoming = true;
                    }
                }
                
                if ((second.equals("society") && prefs.getBoolean(KEY_AREA_SOCIETY, false)) ||
                    (second.equals("mafat") && prefs.getBoolean(KEY_AREA_MAFAT, false))) {
                    if (checkSlot(passed, upcoming, currentTime, 10, 30, secondLabel, true)) {
                        if (currentTime > (10*60+30)) hasPassed = true; else hasUpcoming = true;
                    }
                }
                
                if (checkSlot(passed, upcoming, currentTime, 12, 0, "યાદવ નગરી + ચૌધરી ફરીયો",
                    prefs.getBoolean(KEY_AREA_YADAV, false))) {
                    if (currentTime > 12*60) hasPassed = true; else hasUpcoming = true;
                }
            }
        } else {
            // Second half: 06:00 - Yadav+First, 07:30 - Second, 09:00 - Remaining
            String yadavSlot = "યાદવ નગરી + ચૌધરી ફરીયો";
            boolean yadavSelected = prefs.getBoolean(KEY_AREA_YADAV, false);
            boolean firstSelected = (first.equals("society") && prefs.getBoolean(KEY_AREA_SOCIETY, false)) ||
                                   (first.equals("mafat") && prefs.getBoolean(KEY_AREA_MAFAT, false));
            
            if ((yadavSelected || firstSelected)) {
                String label = yadavSlot;
                if (firstSelected) label += " + " + firstLabel;
                if (checkSlot(passed, upcoming, currentTime, 6, 0, label, true)) {
                    if (currentTime > 6*60) hasPassed = true; else hasUpcoming = true;
                }
            }
            
            if ((second.equals("society") && prefs.getBoolean(KEY_AREA_SOCIETY, false)) ||
                (second.equals("mafat") && prefs.getBoolean(KEY_AREA_MAFAT, false))) {
                String label = second.equals("society") ? "સોસાયટી" : "મફત નગરી";
                if (checkSlot(passed, upcoming, currentTime, 7, 30, label, true)) {
                    if (currentTime > (7*60+30)) hasPassed = true; else hasUpcoming = true;
                }
            }
            
            if (checkSlot(passed, upcoming, currentTime, 9, 0, "બાકીનો વિસ્તાર",
                prefs.getBoolean(KEY_AREA_REMAINING, false))) {
                if (currentTime > 9*60) hasPassed = true; else hasUpcoming = true;
            }
        }
        
        // Tomorrow's schedule (always in upcoming)
        upcoming.append("\n📆 કાલે:\n");
        Calendar tomorrowCal = Calendar.getInstance();
        tomorrowCal.add(Calendar.DAY_OF_MONTH, 1);
        int tomorrowDay = tomorrowCal.get(Calendar.DAY_OF_MONTH);
        boolean tomorrowFirstHalf = tomorrowDay <= 15;
        boolean tomorrowIsBorewell = ((days + 1) % 2 == 0);
        boolean tomorrowTankFill = prefs.getBoolean(KEY_TANK_FILL_PERSIST, false);
        String tomorrowFirst = ((days + 1) % 2 == 0) ? "society" : "mafat";
        if (swap) tomorrowFirst = tomorrowFirst.equals("society") ? "mafat" : "society";
        String tomorrowSecond = tomorrowFirst.equals("society") ? "mafat" : "society";
        String tFirstLabel = tomorrowFirst.equals("society") ? "સોસાયટી" : "મફત નગરી";
        String tSecondLabel = tomorrowSecond.equals("society") ? "સોસાયટી" : "મફત નગરી";
        
        if (morningSelected) {
            String tSource = tomorrowIsBorewell ? "બોરવેલ" : "નર્મદા";
            boolean tShouldAlert = false;
            if (prefs.getInt(KEY_MORNING_SOURCE, 0) == 0) tShouldAlert = true;
            else if (prefs.getInt(KEY_MORNING_SOURCE, 0) == 1 && !tomorrowIsBorewell) tShouldAlert = true;
            else if (prefs.getInt(KEY_MORNING_SOURCE, 0) == 2 && tomorrowIsBorewell) tShouldAlert = true;
            
            if (tShouldAlert) {
                upcoming.append("⏳ 07:45 AM - સવારનો એલર્ટ (").append(tSource).append(")\n");
                hasUpcoming = true;
            }
        }
        
        // Tomorrow's slots (simplified)
        if (tomorrowFirstHalf) {
            if (tomorrowTankFill) {
                if (prefs.getBoolean(KEY_AREA_REMAINING, false)) upcoming.append("⏳ 06:00 AM - બાકીનો વિસ્તાર\n");
                // Add others as needed...
            } else {
                if (prefs.getBoolean(KEY_AREA_REMAINING, false)) upcoming.append("⏳ 06:00 AM - બાકીનો વિસ્તાર\n");
                if (tomorrowFirst.equals("mafat") && prefs.getBoolean(KEY_AREA_MAFAT, false)) upcoming.append("⏳ 09:00 AM - મફત નગરી\n");
                if (tomorrowFirst.equals("society") && prefs.getBoolean(KEY_AREA_SOCIETY, false)) upcoming.append("⏳ 09:00 AM - સોસાયટી\n");
                if (tomorrowSecond.equals("mafat") && prefs.getBoolean(KEY_AREA_MAFAT, false)) upcoming.append("⏳ 10:30 AM - મફત નગરી\n");
                if (tomorrowSecond.equals("society") && prefs.getBoolean(KEY_AREA_SOCIETY, false)) upcoming.append("⏳ 10:30 AM - સોસાયટી\n");
                if (prefs.getBoolean(KEY_AREA_YADAV, false)) upcoming.append("⏳ 12:00 PM - યાદવ નગરી + ચૌધરી ફરીયો\n");
            }
        } else {
            // Second half tomorrow...
            if (prefs.getBoolean(KEY_AREA_YADAV, false)) upcoming.append("⏳ 06:00 AM - યાદવ નગરી + ચૌધરી ફરીયો\n");
            if (tomorrowFirst.equals("society") && prefs.getBoolean(KEY_AREA_SOCIETY, false)) upcoming.append("⏳ 06:00 AM - સોસાયટી\n");
            if (tomorrowFirst.equals("mafat") && prefs.getBoolean(KEY_AREA_MAFAT, false)) upcoming.append("⏳ 06:00 AM - મફત નગરી\n");
            if (tomorrowSecond.equals("society") && prefs.getBoolean(KEY_AREA_SOCIETY, false)) upcoming.append("⏳ 07:30 AM - સોસાયટી\n");
            if (tomorrowSecond.equals("mafat") && prefs.getBoolean(KEY_AREA_MAFAT, false)) upcoming.append("⏳ 07:30 AM - મફત નગરી\n");
            if (prefs.getBoolean(KEY_AREA_REMAINING, false)) upcoming.append("⏳ 09:00 AM - બાકીનો વિસ્તાર\n");
        }
        
        StringBuilder finalText = new StringBuilder();
        // FIXED: Show passed section only if hasPassed is true (works for any alert type now)
        if (hasPassed) {
            finalText.append("📆 આજના પસાર થયેલા:\n").append(passed).append("\n");
        }
        if (hasUpcoming) {
            finalText.append("📆 આગામી:\n").append(upcoming);
        }
        
        int mins = prefs.getInt(KEY_MINUTES_BEFORE, 15);
        finalText.append("\n💡 એલર્ટ ").append(mins).append(" મિનિટ પહેલાં આવશે");
        
        tvNextAlert.setText(finalText.toString());
    }
    
    // FIXED: Returns true if item was selected (added to passed or upcoming)
    private boolean checkSlot(StringBuilder passed, StringBuilder upcoming, int currentTime, 
                          int hour, int minute, String label, boolean isSelected) {
        if (!isSelected) return false;
        int slotTime = hour * 60 + minute;
        if (currentTime > slotTime) {
            passed.append("✓ ").append(String.format("%02d:%02d", hour, minute))
                  .append(" - ").append(label).append("\n");
        } else {
            upcoming.append("⏳ ").append(String.format("%02d:%02d", hour, minute))
                   .append(" - ").append(label).append("\n");
        }
        return true;
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
        String firstLabel = first.equals("society") ? "સોસાયટી" : "મફત નગરી";
        String secondLabel = second.equals("society") ? "સોસાયટી" : "મફત નગરી";
        
        int alarmIdx = 0;
        
        if (firstHalf) {
            if (tankFill) {
                if (prefs.getBoolean(KEY_AREA_REMAINING, false)) {
                    alarmIdx = scheduleIfPossible(am, now, 6, 0, minsBefore, "બાકીનો વિસ્તાર", alarmIdx);
                }
                String yadavWithFirst = "યાદવ નગરી + ચૌધરી ફરીયો";
                if (first.equals("society") && prefs.getBoolean(KEY_AREA_SOCIETY, false)) {
                    yadavWithFirst += " + સોસાયટી";
                } else if (first.equals("mafat") && prefs.getBoolean(KEY_AREA_MAFAT, false)) {
                    yadavWithFirst += " + મફત નગરી";
                }
                if (prefs.getBoolean(KEY_AREA_YADAV, false) || 
                    (first.equals("society") && prefs.getBoolean(KEY_AREA_SOCIETY, false)) ||
                    (first.equals("mafat") && prefs.getBoolean(KEY_AREA_MAFAT, false))) {
                    alarmIdx = scheduleIfPossible(am, now, 11, 0, minsBefore, yadavWithFirst, alarmIdx);
                }
                if (second.equals("society") && prefs.getBoolean(KEY_AREA_SOCIETY, false)) {
                    alarmIdx = scheduleIfPossible(am, now, 12, 30, minsBefore, "સોસાયટી", alarmIdx);
                } else if (second.equals("mafat") && prefs.getBoolean(KEY_AREA_MAFAT, false)) {
                    alarmIdx = scheduleIfPossible(am, now, 12, 30, minsBefore, "મફત નગરી", alarmIdx);
                }
            } else {
                if (prefs.getBoolean(KEY_AREA_REMAINING, false)) {
                    alarmIdx = scheduleIfPossible(am, now, 6, 0, minsBefore, "બાકીનો વિસ્તાર", alarmIdx);
                }
                if (first.equals("society") && prefs.getBoolean(KEY_AREA_SOCIETY, false)) {
                    alarmIdx = scheduleIfPossible(am, now, 9, 0, minsBefore, "સોસાયટી", alarmIdx);
                } else if (first.equals("mafat") && prefs.getBoolean(KEY_AREA_MAFAT, false)) {
                    alarmIdx = scheduleIfPossible(am, now, 9, 0, minsBefore, "મફત નગરી", alarmIdx);
                }
                if (second.equals("society") && prefs.getBoolean(KEY_AREA_SOCIETY, false)) {
                    alarmIdx = scheduleIfPossible(am, now, 10, 30, minsBefore, "સોસાયટી", alarmIdx);
                } else if (second.equals("mafat") && prefs.getBoolean(KEY_AREA_MAFAT, false)) {
                    alarmIdx = scheduleIfPossible(am, now, 10, 30, minsBefore, "મફત નગરી", alarmIdx);
                }
                if (prefs.getBoolean(KEY_AREA_YADAV, false)) {
                    alarmIdx = scheduleIfPossible(am, now, 12, 0, minsBefore, "યાદવ નગરી + ચૌધરી ફરીયો", alarmIdx);
                }
            }
        } else {
            // Second half today (similar logic)
            if (prefs.getBoolean(KEY_AREA_YADAV, false)) {
                alarmIdx = scheduleIfPossible(am, now, 6, 0, minsBefore, "યાદવ નગરી + ચૌધરી ફરીયો", alarmIdx);
            }
            if (first.equals("society") && prefs.getBoolean(KEY_AREA_SOCIETY, false)) {
                alarmIdx = scheduleIfPossible(am, now, 6, 0, minsBefore, "સોસાયટી", alarmIdx);
            } else if (first.equals("mafat") && prefs.getBoolean(KEY_AREA_MAFAT, false)) {
                alarmIdx = scheduleIfPossible(am, now, 6, 0, minsBefore, "મફત નગરી", alarmIdx);
            }
            if (second.equals("society") && prefs.getBoolean(KEY_AREA_SOCIETY, false)) {
                alarmIdx = scheduleIfPossible(am, now, 7, 30, minsBefore, "સોસાયટી", alarmIdx);
            } else if (second.equals("mafat") && prefs.getBoolean(KEY_AREA_MAFAT, false)) {
                alarmIdx = scheduleIfPossible(am, now, 7, 30, minsBefore, "મફત નગરી", alarmIdx);
            }
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
        html.append("<style>");
        html.append("body{font-family:system-ui;background:#000000;padding:0;margin:0;color:#ffffff}");
        html.append(".card{background:#121212;border:1px solid #333333;border-radius:12px;padding:16px;margin-bottom:16px}");
        html.append("h2{font-size:18px;margin:0 0 12px 0;color:#ffffff;font-weight:bold}");
        html.append(".date{color:#aaaaaa;font-size:14px;font-weight:normal}");
        html.append(".badge{color:#4fc3f7;background:#0d2b3a;padding:4px 10px;border-radius:6px;font-size:13px;font-weight:600;display:inline-block;margin-bottom:8px}");
        html.append(".slot{display:flex;padding:12px 0;border-top:1px solid #333333}");
        html.append(".slot:first-child{border-top:none}");
        html.append(".time{min-width:90px;font-weight:600;color:#4fc3f7;font-size:14px}");
        html.append(".label{flex:1;color:#ffffff;font-size:14px;padding-left:8px}");
        html.append(".footer{color:#888888;font-size:12px;margin-top:16px;padding-top:16px;border-top:1px solid #333333}");
        html.append("</style></head><body>");
        
        html.append(getDayCardHtml(true));
        html.append(getDayCardHtml(false));
        
        html.append("<div class='footer'>ℹ️ બાકીનો વિસ્તાર = વથાણ ચોક, બજાર ચોક અને નજીકના વિસ્તારો</div>");
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
                addSlotHtml(slots, "11:00–12:30", "યાદવ નગરી + ચૌધરી ફરીયો + " + firstLabel);
                addSlotHtml(slots, "12:30–14:00", secondLabel);
            } else {
                addSlotHtml(slots, "06:00–09:00", "બાકીનો વિસ્તાર");
                addSlotHtml(slots, "09:00–10:30", firstLabel);
                addSlotHtml(slots, "10:30–12:00", secondLabel);
                addSlotHtml(slots, "12:00–13:30", "યાદવ નગરી + ચૌધરી ફરીયો");
            }
        } else {
            addSlotHtml(slots, "06:00–07:30", "યાદવ નગરી + ચૌધરી ફરીયો + " + firstLabel);
            addSlotHtml(slots, "07:30–09:00", secondLabel);
            addSlotHtml(slots, "09:00–12:00", "બાકીનો વિસ્તાર");
        }
        
        String dateStr = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH).format(cal.getTime());
        String title = isToday ? "📅 આજે | " : "📅 આવતીકાલે | ";
        
        StringBuilder card = new StringBuilder();
        card.append("<div class='card'><h2>").append(title).append("<span class='date'>").append(dateStr).append("</span></h2>");
        card.append("<span class='badge'>").append(source).append("</span>");
        card.append(slots);
        card.append("</div>");
        
        return card.toString();
    }
    
    private void addSlotHtml(StringBuilder sb, String time, String label) {
        sb.append("<div class='slot'><div class='time'>").append(time).append("</div><div class='label'>").append(label).append("</div></div>");
    }
}
