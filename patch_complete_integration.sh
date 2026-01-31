#!/bin/bash
set -e

echo "================================================================"
echo "COMPLETE UPDATE: Tank Schedule + Service Improvements"
echo "================================================================"
echo ""

if [ ! -f "build.gradle" ]; then
    echo "❌ Run from project root (Fstest directory)"
    exit 1
fi

BACKUP="backup_complete_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP"

# Backup files
for f in \
    "app/src/main/java/com/example/chris/fstest/MainActivity.java" \
    "app/src/main/java/com/example/chris/fstest/KeyboardTapService.java"
do
    [ -f "$f" ] && cp "$f" "$BACKUP/"
done

echo "✓ Backup: $BACKUP/"
echo ""

# ===== 1. NEW MAINACTIVITY WITH TANK SCHEDULE =====
echo "Creating MainActivity with tank schedule..."

cat > "app/src/main/java/com/example/chris/fstest/MainActivity.java" << 'EOF'
package com.example.chris.fstest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
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
        
        btn.setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
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
            "• <strong>DOWN ↓</strong> → Next reel" +
            "</div>" +
            
            "</div>" +
            "</body>" +
            "</html>";
    }
    
    private String getCurrentDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", java.util.Locale.ENGLISH);
        return sdf.format(new java.util.Date());
    }
}
EOF

echo "✓ MainActivity.java created"

# ===== 2. IMPROVED KEYBOARDTAPSERVICE =====
echo "Improving KeyboardTapService..."

cat > "app/src/main/java/com/example/chris/fstest/KeyboardTapService.java" << 'EOF'
package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class KeyboardTapService extends AccessibilityService
        implements InputManager.InputDeviceListener {

    private static final String TAG = "IGKbd";
    private static final String IG = "com.instagram.android";
    
    // Coordinates
    private static final int SEND_X = 990;
    private static final int SEND_Y = 2313;
    private static final int CENTER_X = 540;
    private static final int CENTER_Y = 1170;
    private static final int RIGHT_X = 810; // For SHIFT hold
    
    // Optimized swipe (200ms is smoother than 100ms)
    private static final int SWIPE_DURATION = 200;
    private static final int SWIPE_DISTANCE = 1000;

    private InputManager im;
    private boolean kbd = false;
    private boolean ig = false;
    private boolean shiftHeld = false;
    private Handler handler = new Handler();
    private Runnable shiftTask;

    @Override
    public void onServiceConnected() {
        Log.d(TAG, "=== SERVICE CONNECTED ===");
        im = (InputManager) getSystemService(INPUT_SERVICE);
        im.registerInputDeviceListener(this, null);
        checkKbd();
    }

    private void checkKbd() {
        boolean found = false;
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice d = InputDevice.getDevice(id);
            if (d != null && !d.isVirtual() && 
                (d.getSources() & InputDevice.SOURCE_KEYBOARD) != 0) {
                Log.d(TAG, "Physical keyboard: " + d.getName());
                found = true;
                break;
            }
        }
        if (found != kbd) {
            kbd = found;
            Log.d(TAG, "*** KEYBOARD: " + kbd + " ***");
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getPackageName() != null) {
            boolean nowIG = IG.equals(e.getPackageName().toString());
            if (nowIG != ig) {
                ig = nowIG;
                Log.d(TAG, "*** INSTAGRAM: " + ig + " ***");
            }
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        // Only intercept when BOTH conditions met
        if (!kbd || !ig) return false;
        
        int key = e.getKeyCode();
        int action = e.getAction();
        
        // SHIFT - continuous hold on RIGHT side
        if (key == KeyEvent.KEYCODE_SHIFT_LEFT || key == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            if (action == KeyEvent.ACTION_DOWN && !shiftHeld) {
                shiftHeld = true;
                Log.d(TAG, "SHIFT DOWN - starting continuous hold");
                startContinuousHold();
            } else if (action == KeyEvent.ACTION_UP) {
                shiftHeld = false;
                Log.d(TAG, "SHIFT UP - stopping hold");
                stopContinuousHold();
            }
            return false; // Don't consume shift
        }
        
        // ENTER - send message
        if (key == KeyEvent.KEYCODE_ENTER) {
            if (action == KeyEvent.ACTION_UP) {
                Log.d(TAG, "ENTER UP -> tap Send");
                tap(SEND_X, SEND_Y, 50);
            }
            // Consume BOTH to prevent newline
            return true;
        }
        
        // UP - previous reel
        if (key == KeyEvent.KEYCODE_DPAD_UP && action == KeyEvent.ACTION_DOWN) {
            Log.d(TAG, "UP -> swipe DOWN (previous)");
            smoothSwipe(CENTER_Y - SWIPE_DISTANCE/2, CENTER_Y + SWIPE_DISTANCE/2);
            return true;
        }
        
        // DOWN - next reel
        if (key == KeyEvent.KEYCODE_DPAD_DOWN && action == KeyEvent.ACTION_DOWN) {
            Log.d(TAG, "DOWN -> swipe UP (next)");
            smoothSwipe(CENTER_Y + SWIPE_DISTANCE/2, CENTER_Y - SWIPE_DISTANCE/2);
            return true;
        }
        
        return false;
    }

    private void startContinuousHold() {
        if (shiftTask != null) handler.removeCallbacks(shiftTask);
        
        shiftTask = new Runnable() {
            public void run() {
                if (shiftHeld) {
                    Log.d(TAG, "Hold on RIGHT side");
                    longPress(RIGHT_X, CENTER_Y, 500);
                    handler.postDelayed(this, 500);
                }
            }
        };
        handler.post(shiftTask);
    }

    private void stopContinuousHold() {
        if (shiftTask != null) {
            handler.removeCallbacks(shiftTask);
            shiftTask = null;
        }
    }

    private void tap(int x, int y, int dur) {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(x, y);
        dispatchGesture(new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, dur))
            .build(), null, null);
    }

    private void smoothSwipe(int startY, int endY) {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(CENTER_X, startY);
        p.lineTo(CENTER_X, endY);
        dispatchGesture(new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, SWIPE_DURATION))
            .build(), null, null);
    }

    private void longPress(int x, int y, int dur) {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(x, y);
        dispatchGesture(new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, dur))
            .build(), null, null);
    }

    @Override public void onInputDeviceAdded(int id) { checkKbd(); }
    @Override public void onInputDeviceRemoved(int id) { checkKbd(); }
    @Override public void onInputDeviceChanged(int id) { checkKbd(); }
    @Override public void onInterrupt() {}
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "=== SERVICE DESTROYED ===");
        stopContinuousHold();
        if (im != null) im.unregisterInputDeviceListener(this);
        super.onDestroy();
    }
}
EOF

echo "✓ KeyboardTapService.java improved"
echo ""

echo "================================================================"
echo "✓ UPDATE COMPLETE"
echo "================================================================"
echo ""
echo "CHANGES MADE:"
echo ""
echo "1. MAINACTIVITY - Tank Schedule Integrated"
echo "   ✓ Shows beautiful water distribution schedule"
echo "   ✓ Gujarati language support"
echo "   ✓ Clean, modern UI"
echo "   ✓ Button to enable keyboard service"
echo "   ✓ Shows keyboard helper info"
echo ""
echo "2. KEYBOARDTAPSERVICE - Major Improvements"
echo "   ✓ Removed notification (as you requested)"
echo "   ✓ Removed ENTER cooldown (blocks fast typing)"
echo "   ✓ Removed complex text input detection"
echo "   ✓ Added SHIFT hold for fast forward (RIGHT side)"
echo "   ✓ Optimized swipe (200ms smoother than 100ms)"
echo "   ✓ Simplified, cleaner code"
echo "   ✓ Better logging"
echo ""
echo "KEY BINDINGS:"
echo "   ENTER       → Send message in DM"
echo "   UP arrow    → Previous reel (swipe down)"
echo "   DOWN arrow  → Next reel (swipe up)"
echo "   SHIFT hold  → Fast forward (hold on RIGHT side)"
echo ""
echo "BUILD & INSTALL:"
echo "   ./gradlew clean assembleDebug"
echo "   adb install -r app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "APP USAGE:"
echo "   1. Open app → See tank schedule"
echo "   2. Tap button → Enable keyboard service"
echo "   3. Connect keyboard → Service activates"
echo "   4. Open Instagram → Keys work!"
echo ""
echo "DEBUG:"
echo "   adb logcat | grep IGKbd"
echo ""
echo "Backup saved in: $BACKUP/"
echo ""
