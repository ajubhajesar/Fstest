#!/bin/bash
set -e

echo "================================================"
echo "SIMPLIFIED WORKING FIX - BASED ON WORKING CODE"
echo "================================================"
echo ""

if [ ! -f "build.gradle" ]; then
    echo "❌ Run from project root"
    exit 1
fi

BACKUP="backup_working_fix_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP"

for f in \
    "app/src/main/java/com/example/chris/fstest/KeyboardTapService.java"
do
    [ -f "$f" ] && cp "$f" "$BACKUP/"
done

echo "✓ Backup: $BACKUP/"
echo ""
echo "Applying working fix based on original code..."

cat > "app/src/main/java/com/example/chris/fstest/KeyboardTapService.java" << 'ENDOFFILE'
package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Path;
import android.hardware.input.InputManager;
import android.os.Build;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class KeyboardTapService extends AccessibilityService
        implements InputManager.InputDeviceListener {

    private static final String TAG = "IGKbd";
    private static final String IG = "com.instagram.android";
    private static final int X = 990;
    private static final int Y = 2313;

    private InputManager im;
    private NotificationManager nm;
    private boolean kbd = false;
    private boolean ig = false;

    @Override
    public void onServiceConnected() {
        Log.d(TAG, "Service started");
        im = (InputManager) getSystemService(INPUT_SERVICE);
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("kbd", "Keyboard", 
                NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
        
        im.registerInputDeviceListener(this, null);
        checkKbd();
    }

    private void checkKbd() {
        boolean found = false;
        int[] ids = InputDevice.getDeviceIds();
        for (int i = 0; i < ids.length; i++) {
            InputDevice d = InputDevice.getDevice(ids[i]);
            if (d != null && !d.isVirtual() && 
                (d.getSources() & InputDevice.SOURCE_KEYBOARD) != 0) {
                Log.d(TAG, "Keyboard: " + d.getName());
                found = true;
                break;
            }
        }
        if (found != kbd) {
            kbd = found;
            Log.d(TAG, "Kbd state: " + kbd);
            updateNotification();
        }
    }

    private void updateNotification() {
        // CRITICAL: Only show notification if keyboard is connected
        if (!kbd) {
            nm.cancel(1);
            Log.d(TAG, "Notification cancelled - no keyboard");
            return;
        }
        
        String txt = ig ? "IG active - ENTER sends" : "Waiting for IG";
        Notification n;
        if (Build.VERSION.SDK_INT >= 26) {
            n = new Notification.Builder(this, "kbd")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Keyboard connected")
                .setContentText(txt)
                .setOngoing(true)
                .build();
        } else {
            n = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Keyboard connected")
                .setContentText(txt)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .build();
        }
        nm.notify(1, n);
        Log.d(TAG, "Notification shown: " + txt);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence pkg = e.getPackageName();
            boolean now = pkg != null && IG.equals(pkg.toString());
            if (now != ig) {
                ig = now;
                Log.d(TAG, "IG: " + ig + " pkg: " + pkg);
                
                // CRITICAL: Only update notification if keyboard is connected
                if (kbd) {
                    updateNotification();
                } else {
                    Log.d(TAG, "Instagram state changed but no keyboard - skipping notification");
                }
            }
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        // CRITICAL FIX: Don't check if device is virtual - just check if physical keyboard is connected
        // The virtual keyboard being shown doesn't mean this key event is from it
        
        if (!kbd || !ig) return false;
        
        int keyCode = e.getKeyCode();
        int action = e.getAction();
        
        Log.d(TAG, "Key: " + keyCode + " action: " + action);
        
        // Only handle ENTER key
        if (keyCode != KeyEvent.KEYCODE_ENTER) return false;
        
        // CRITICAL: Return true for BOTH ACTION_DOWN and ACTION_UP to prevent newline
        if (action == KeyEvent.ACTION_DOWN) {
            Log.d(TAG, "ENTER DOWN - consuming to prevent newline");
            return true;
        } else if (action == KeyEvent.ACTION_UP) {
            Log.d(TAG, "ENTER UP - tapping at (" + X + "," + Y + ")");
            tap();
            return true;
        }
        
        return false;
    }

    private void tap() {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(X, Y);
        GestureDescription.StrokeDescription s = 
            new GestureDescription.StrokeDescription(p, 0, 50);
        boolean dispatched = dispatchGesture(new GestureDescription.Builder().addStroke(s).build(), null, null);
        Log.d(TAG, "Tap dispatched: " + dispatched);
    }

    @Override public void onInputDeviceAdded(int id) { 
        Log.d(TAG, "Device added: " + id);
        checkKbd(); 
    }
    
    @Override public void onInputDeviceRemoved(int id) { 
        Log.d(TAG, "Device removed: " + id);
        checkKbd(); 
    }
    
    @Override public void onInputDeviceChanged(int id) { 
        checkKbd(); 
    }
    
    @Override public void onInterrupt() {}
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed");
        if (im != null) im.unregisterInputDeviceListener(this);
        nm.cancel(1);
        super.onDestroy();
    }
}
ENDOFFILE

echo "✓ KeyboardTapService.java updated"
echo ""
echo "================================================"
echo "✓ FIXES APPLIED"
echo "================================================"
echo ""
echo "FIX 1: ENTER Key Consumption"
echo "  • Now returns true for BOTH ACTION_DOWN and ACTION_UP"
echo "  • ACTION_DOWN: Logs and consumes to prevent newline"
echo "  • ACTION_UP: Logs and performs tap at coordinates"
echo "  • This should completely prevent newline insertion"
echo ""
echo "FIX 2: Notification Issue"
echo "  • CRITICAL: updateNotification() checks if (!kbd) first and returns early"
echo "  • CRITICAL: onAccessibilityEvent() only calls updateNotification() if kbd is true"
echo "  • Triple-checked: Notification ONLY shows when physical keyboard is connected"
echo ""
echo "FIX 3: Removed Virtual Keyboard Check"
echo "  • Removed: if (e.getDevice() != null && e.getDevice().isVirtual()) return false;"
echo "  • Reason: When typing in Instagram, virtual keyboard is shown but ENTER"
echo "    should still be intercepted if physical keyboard is connected"
echo ""
echo "KEY FEATURES:"
echo "  • Simple: Based on your original working code"
echo "  • Only handles ENTER key (removed all other key logic)"
echo "  • Tap at fixed coordinates (990, 2313)"
echo "  • Proper logging for debugging"
echo ""
echo "COORDINATES:"
echo "  • Fixed: (990, 2313) - from your working code"
echo "  • These are for Pixel 6 (1080x2340 screen)"
echo ""
echo "ADJUST COORDINATES IF NEEDED:"
echo "  To find correct send button coordinates:"
echo "  1. Enable Developer Options"
echo "  2. Enable 'Pointer location'"
echo "  3. Open Instagram DM"
echo "  4. Tap the send button"
echo "  5. Note X,Y coordinates from top-left corner"
echo "  6. Update lines 22-23 in the code"
echo ""
echo "BUILD AND TEST:"
echo "  ./gradlew clean assembleDebug"
echo "  adb install -r app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "TEST PROCEDURE:"
echo ""
echo "1. WITHOUT keyboard:"
echo "   - Open Instagram"
echo "   - Should see NO notification"
echo "   - Log: 'Instagram state changed but no keyboard - skipping notification'"
echo ""
echo "2. WITH keyboard:"
echo "   - Connect keyboard"
echo "   - Should see notification: 'Waiting for IG'"
echo "   - Log: 'Kbd state: true'"
echo ""
echo "3. Open Instagram DM:"
echo "   - Type message"
echo "   - Press ENTER"
echo "   - Should send WITHOUT newline"
echo "   - Log: 'ENTER DOWN - consuming to prevent newline'"
echo "          'ENTER UP - tapping at (990,2313)'"
echo "          'Tap dispatched: true'"
echo ""
echo "DEBUG LOGS:"
echo "  adb logcat | grep IGKbd"
echo ""
echo "If ENTER still inserts newline:"
echo "  1. Check logs to ensure 'ENTER DOWN - consuming' appears"
echo "  2. Check if 'kbd' and 'ig' are both true"
echo "  3. Make sure accessibility service is enabled"
echo ""
echo "Backup saved to: $BACKUP/"
echo "To restore: cp $BACKUP/KeyboardTapService.java app/src/main/java/com/example/chris/fstest/"
echo ""
