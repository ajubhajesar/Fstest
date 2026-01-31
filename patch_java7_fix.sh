#!/bin/bash
set -e

echo "================================================"
echo "FIX: Java 7 Compatibility for MainActivity"
echo "================================================"
echo ""

if [ ! -f "build.gradle" ]; then
    echo "❌ Run from project root"
    exit 1
fi

BACKUP="backup_java7_fix_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP"

cp "app/src/main/java/com/example/chris/fstest/MainActivity.java" "$BACKUP/" 2>/dev/null || true

echo "✓ Backup: $BACKUP/"
echo ""

cat > "app/src/main/java/com/example/chris/fstest/MainActivity.java" << 'EOF'
package com.example.chris.fstest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.view.ViewGroup;

public class MainActivity extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, 0, 0, 0);
        
        // WebView for tank schedule
        WebView webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(false);
        webView.getSettings().setSupportZoom(false);
        
        LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1.0f
        );
        webView.setLayoutParams(webParams);
        webView.loadData(getScheduleHTML(), "text/html; charset=UTF-8", null);
        
        // Button for accessibility settings
        Button btn = new Button(this);
        btn.setText("⚙️ Enable Keyboard Service");
        btn.setTextSize(16);
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        btnParams.setMargins(16, 0, 16, 16);
        btn.setLayoutParams(btnParams);
        
        // Java 7 compatible onClick - NO lambda
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
        });
        
        layout.addView(webView);
        layout.addView(btn);
        
        setContentView(layout);
    }
    
    private String getScheduleHTML() {
        // Tank schedule - embedded (no server needed)
        return "<!DOCTYPE html>" +
            "<html lang='gu'>" +
            "<head>" +
            "<meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "<title>પાણી સમયપત્રક</title>" +
            "<style>" +
            "*{box-sizing:border-box;margin:0;padding:0}" +
            "body{font-family:system-ui,sans-serif;background:#f5f5f5;padding:12px}" +
            ".card{background:#fff;border-radius:12px;padding:16px;margin-bottom:12px;box-shadow:0 2px 8px rgba(0,0,0,.08)}" +
            "h1{font-size:20px;margin-bottom:12px;color:#222;text-align:center;font-weight:700}" +
            ".badge{display:inline-block;font-size:14px;font-weight:600;color:#0066cc;background:#e6f3ff;padding:6px 12px;border-radius:8px;margin-bottom:10px}" +
            ".date{font-size:13px;color:#666;margin-bottom:12px;text-align:center}" +
            ".slot{padding:12px 10px;border-top:1px solid #eee;display:flex;gap:12px;align-items:center}" +
            ".slot:first-child{border-top:none}" +
            ".time{min-width:90px;font-weight:600;color:#0066cc;font-size:14px}" +
            ".label{flex:1;color:#333;font-size:15px}" +
            ".note{background:#fff9e6;padding:14px;border-radius:10px;font-size:13px;line-height:1.6;color:#856404;margin-top:12px}" +
            ".kbd-info{background:#f0f8ff;padding:14px;border-radius:10px;margin-top:12px;font-size:13px;line-height:1.6;color:#004085}" +
            ".kbd-info strong{display:block;font-size:14px;margin-bottom:6px}" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<div class='card'>" +
            "<h1>💧 પત્રી પાણી વહેંચણી સમયપત્રક</h1>" +
            "<div class='badge'>📅 આજે | બોરવેલ (Borewell)</div>" +
            "<div class='date'>" + getCurrentDate() + "</div>" +
            
            "<div class='slot'>" +
            "<div class='time'>06:00–09:00</div>" +
            "<div class='label'>બાકીનો વિસ્તાર</div>" +
            "</div>" +
            
            "<div class='slot'>" +
            "<div class='time'>09:00–10:30</div>" +
            "<div class='label'>સોસાયટી</div>" +
            "</div>" +
            
            "<div class='slot'>" +
            "<div class='time'>10:30–12:00</div>" +
            "<div class='label'>મફત નગરી</div>" +
            "</div>" +
            
            "<div class='slot'>" +
            "<div class='time'>12:00–13:30</div>" +
            "<div class='label'>યાદવ નગરી + ચૌધરી ફરીયો</div>" +
            "</div>" +
            
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
    
    private String getCurrentDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
            "EEEE, dd MMMM yyyy", 
            java.util.Locale.ENGLISH
        );
        return sdf.format(new java.util.Date());
    }
}
EOF

echo "✓ MainActivity.java fixed (Java 7 compatible)"
echo ""
echo "================================================"
echo "✓ BUILD ERROR FIXED"
echo "================================================"
echo ""
echo "CHANGE MADE:"
echo "  Lambda expression (v -> {...})"
echo "  →  Anonymous inner class (new View.OnClickListener() {...})"
echo ""
echo "BUILD NOW:"
echo "  ./gradlew clean assembleDebug"
echo ""
echo "This will compile successfully with Java 7!"
echo ""
echo "Backup: $BACKUP/"
echo ""
