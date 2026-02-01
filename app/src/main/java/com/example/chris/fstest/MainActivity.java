package com.example.chris.fstest;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.view.ViewGroup;
import android.graphics.Color;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    
    private WebView webView;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "TankSchedulePrefs";
    private static final String KEY_TANK_FILL = "tank_fill_today";
    private static final String KEY_SWAP_MS = "mf_society_swap";
    
    // Schedule constants (matches minimal.py logic)
    private static final String AREA_YADAV = "યાદવ નગરી + ચૌધરી ફરીયો";
    private static final String AREA_MAFAT = "મફત નગરી";
    private static final String AREA_SOCIETY = "સોસાયટી";
    private static final String AREA_REMAINING = "બાકીનો વિસ્તાર";
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
        
        // Root layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#f5f5f5"));
        layout.setPadding(16, 16, 16, 16);
        
        // Settings Panel (Checkboxes)
        LinearLayout settingsPanel = new LinearLayout(this);
        settingsPanel.setOrientation(LinearLayout.VERTICAL);
        settingsPanel.setBackgroundColor(Color.parseColor("#e0e0e0"));
        settingsPanel.setPadding(16, 16, 16, 16);
        
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        settingsParams.setMargins(0, 0, 0, 16);
        settingsPanel.setLayoutParams(settingsParams);
        
        // Tank Fill Checkbox
        CheckBox cbTankFill = new CheckBox(this);
        cbTankFill.setText("ટાંકો ભરાઈ રહ્યો છે (Tank filling today)");
        cbTankFill.setChecked(prefs.getBoolean(KEY_TANK_FILL, false));
        cbTankFill.setTextSize(16);
        cbTankFill.setPadding(8, 8, 8, 8);
        cbTankFill.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_TANK_FILL, isChecked).apply();
                refreshSchedule();
            }
        });
        
        // Swap Checkbox (MF_SOCIETY_FORCE_SWAP logic)
        CheckBox cbSwap = new CheckBox(this);
        cbSwap.setText("મફત/સોસાયટી અદલાબદલી (Swap Mafat/Society order)");
        cbSwap.setChecked(prefs.getBoolean(KEY_SWAP_MS, false));
        cbSwap.setTextSize(16);
        cbSwap.setPadding(8, 8, 8, 8);
        cbSwap.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_SWAP_MS, isChecked).apply();
                refreshSchedule();
            }
        });
        
        settingsPanel.addView(cbTankFill);
        settingsPanel.addView(cbSwap);
        layout.addView(settingsPanel);
        
        // Card-like container for WebView
        LinearLayout webViewContainer = new LinearLayout(this);
        webViewContainer.setOrientation(LinearLayout.VERTICAL);
        webViewContainer.setBackgroundColor(Color.WHITE);
        webViewContainer.setPadding(0, 0, 0, 0);
        
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1.0f
        );
        containerParams.setMargins(0, 0, 0, 16);
        webViewContainer.setLayoutParams(containerParams);
        
        // WebView for schedule display
        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(false);
        ws.setSupportZoom(false);
        ws.setDefaultTextEncodingName("utf-8");
        
        LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        webView.setLayoutParams(webParams);
        
        webViewContainer.addView(webView);
        layout.addView(webViewContainer);
        
        // Enable Keyboard Service Button
        Button btn = new Button(this);
        btn.setText("⚙️ Enable Keyboard Service");
        btn.setTextSize(16);
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        btn.setLayoutParams(btnParams);
        
        // Java 7 compatible onClick - NO lambda
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
        });
        
        layout.addView(btn);
        setContentView(layout);
        
        // Initial load with delay to ensure WebView is ready
        webView.post(new Runnable() {
            @Override
            public void run() {
                refreshSchedule();
            }
        });
    }
    
    private void refreshSchedule() {
        String html = getScheduleHTML();
        // Use loadDataWithBaseURL to properly handle special characters
        webView.loadDataWithBaseURL(null, html, "text/html; charset=UTF-8", "UTF-8", null);
    }
    
    private String getScheduleHTML() {
        boolean tankFill = prefs.getBoolean(KEY_TANK_FILL, false);
        boolean swap = prefs.getBoolean(KEY_SWAP_MS, false);
        
        // Calculate days since seed (for alternation)
        Calendar today = Calendar.getInstance();
        long diffMillis = today.getTimeInMillis() - SEED_DATE.getTimeInMillis();
        int days = (int) (diffMillis / (1000 * 60 * 60 * 24));
        
        // Source alternates daily (Borewell on even days, Narmada on odd)
        String source = (days % 2 == 0) ? "બોરવેલ (Borewell)" : "નર્મદા (Narmada)";
        
        // Day of month determines first half or second half schedule
        int dayOfMonth = today.get(Calendar.DAY_OF_MONTH);
        boolean firstHalf = dayOfMonth <= 15;
        
        // Partner alternation (from tankschedule.py)
        String first = (days % 2 == 0) ? "society" : "mafat";
        
        // Apply swap if enabled (matches MF_SOCIETY_FORCE_SWAP)
        if (swap) {
            first = first.equals("society") ? "mafat" : "society";
        }
        String second = first.equals("society") ? "mafat" : "society";
        
        String firstLabel = first.equals("society") ? AREA_SOCIETY : AREA_MAFAT;
        String secondLabel = second.equals("society") ? AREA_SOCIETY : AREA_MAFAT;
        
        // Build slots HTML
        StringBuilder slotsHtml = new StringBuilder();
        
        if (firstHalf) {
            if (tankFill) {
                // Tank filling mode (09:00-11:00 blocked)
                addSlot(slotsHtml, "06:00–09:00", AREA_REMAINING);
                addSlot(slotsHtml, "09:00–11:00", "ટાંકો ભરાઈ રહ્યો છે");
                addSlot(slotsHtml, "11:00–12:30", AREA_YADAV + " + " + firstLabel);
                addSlot(slotsHtml, "12:30–14:00", secondLabel);
            } else {
                // Normal first-half schedule
                addSlot(slotsHtml, "06:00–09:00", AREA_REMAINING);
                addSlot(slotsHtml, "09:00–10:30", firstLabel);
                addSlot(slotsHtml, "10:30–12:00", secondLabel);
                addSlot(slotsHtml, "12:00–13:30", AREA_YADAV);
            }
        } else {
            // Second half schedule (days 16-31)
            addSlot(slotsHtml, "06:00–07:30", AREA_YADAV + " + " + firstLabel);
            addSlot(slotsHtml, "07:30–09:00", secondLabel);
            addSlot(slotsHtml, "09:00–12:00", AREA_REMAINING);
        }
        
        // Current date string
        String dateStr = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH).format(new Date());
        
        // Full HTML - removed # from CSS colors to avoid encoding issues, using rgb() instead
        return "<!DOCTYPE html>" +
            "<html lang='gu'>" +
            "<head>" +
            "<meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "<title>પાણી સમયપત્રક</title>" +
            "<style>" +
            "*{box-sizing:border-box;margin:0;padding:0}" +
            "body{font-family:system-ui,sans-serif;background:rgb(245,245,245);padding:12px}" +
            ".card{background:rgb(255,255,255);border-radius:12px;padding:16px;margin-bottom:12px;box-shadow:0 2px 8px rgba(0,0,0,.08)}" +
            "h1{font-size:20px;margin-bottom:12px;color:rgb(34,34,34);text-align:center;font-weight:700}" +
            ".badge{display:inline-block;font-size:14px;font-weight:600;color:rgb(0,102,204);background:rgb(230,243,255);padding:6px 12px;border-radius:8px;margin-bottom:10px}" +
            ".date{font-size:13px;color:rgb(102,102,102);margin-bottom:12px;text-align:center}" +
            ".slot{padding:12px 10px;border-top:1px solid rgb(238,238,238);display:flex;gap:12px;align-items:center}" +
            ".slot:first-child{border-top:none}" +
            ".time{min-width:90px;font-weight:600;color:rgb(0,102,204);font-size:14px}" +
            ".label{flex:1;color:rgb(51,51,51);font-size:15px}" +
            ".note{background:rgb(255,249,230);padding:14px;border-radius:10px;font-size:13px;line-height:1.6;color:rgb(133,100,4);margin-top:12px}" +
            ".kbd-info{background:rgb(240,248,255);padding:14px;border-radius:10px;margin-top:12px;font-size:13px;line-height:1.6;color:rgb(0,64,133)}" +
            ".kbd-info strong{display:block;font-size:14px;margin-bottom:6px}" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<div class='card'>" +
            "<h1>💧 પત્રી પાણી વહેંચણી સમયપત્રક</h1>" +
            "<div class='badge'>📅 " + source + "</div>" +
            "<div class='date'>" + dateStr + "</div>" +
            
            slotsHtml.toString() +
            
            "<div class='note'>" +
            "📝 <strong>નોંધ:</strong> વીજળી, મોટર સમસ્યા અથવા અન્ય આકસ્મિક કારણોથી સમયમાં ફેરફાર થઈ શકે છે.<br>" +
            "ℹ️ <strong>બાકીનો વિસ્તાર</strong> = વથાણ ચોક, બજાર ચોક અને નજીકના વિસ્તારો" +
            "</div>" +
            
            "<div class='kbd-info'>" +
            "<strong>⌨️ Instagram Keyboard Helper Active</strong>" +
            "Physical keyboard સાથે Instagram માં:<br>" +
            "• <strong>ENTER</strong> → Send message (DM માં)<br>" +
            "• <strong>UP ↑</strong> → Previous reel<br>" +
            "• <strong>DOWN ↓</strong> → Next reel<br>" +
            "• <strong>SHIFT (hold)</strong> → Fast forward" +
            "</div>" +
            
            "</div>" +
            "</body>" +
            "</html>";
    }
    
    private void addSlot(StringBuilder sb, String time, String label) {
        sb.append("<div class='slot'>");
        sb.append("<div class='time'>").append(time).append("</div>");
        sb.append("<div class='label'>").append(label).append("</div>");
        sb.append("</div>");
    }
                                  }
                    
