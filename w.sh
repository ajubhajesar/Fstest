#!/bin/bash
set -e

echo "================================================"
echo "MINIMAL WORKING FIX - BACK TO BASICS"
echo "================================================"
echo ""

if [ ! -f "build.gradle" ]; then
    echo "❌ Run from project root"
    exit 1
fi

BACKUP="backup_minimal_fix_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP"

for f in \
    "app/src/main/java/com/example/chris/fstest/KeyboardTapService.java"
do
    [ -f "$f" ] && cp "$f" "$BACKUP/"
done

echo "✓ Backup: $BACKUP/"
echo ""
echo "Applying minimal working fix..."

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
    
    // Send button coordinates - ADJUST THESE FOR YOUR DEVICE!
    private static final int SEND_X = 990;
    private static final int SEND_Y = 2313;
    
    // Screen center for swipe gestures
    private static final int CENTER_X = 540;
    private static final int CENTER_Y = 1170;

    private InputManager im;
    private NotificationManager nm;
    private boolean kbd = false;
    private boolean ig = false;

    @Override
    public void onServiceConnected() {
        Log.d(TAG, "=== SERVICE CONNECTED ===");
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
        for (int id : ids) {
            InputDevice d = InputDevice.getDevice(id);
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
            
            // Update notification only if we have keyboard
            if (kbd) {
                showNotification();
            } else {
                // Remove notification when keyboard disconnected
                nm.cancel(1);
                Log.d(TAG, "Notification removed (no keyboard)");
            }
        }
    }

    private void showNotification() {
        // Only show if we have keyboard
        if (!kbd) {
            nm.cancel(1);
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
        Log.d(TAG, "Notification: " + txt);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        // Check window state changes
        if (e.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence pkg = e.getPackageName();
            if (pkg != null) {
                boolean now = IG.equals(pkg.toString());
                if (now != ig) {
                    ig = now;
                    Log.d(TAG, "IG: " + ig);
                    
                    // Only update notification if we have keyboard
                    if (kbd) {
                        showNotification();
                    }
                }
            }
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        // Basic check: only when keyboard connected AND Instagram active
        if (!kbd || !ig) {
            return false;
        }
        
        int key = e.getKeyCode();
        int action = e.getAction();
        
        Log.d(TAG, "Key: " + key + " action: " + action);
        
        // Handle ENTER key
        if (key == KeyEvent.KEYCODE_ENTER) {
            // For ENTER key, we need to return true for BOTH down and up
            // to prevent the newline
            if (action == KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "ENTER down - consuming");
                return true;  // Consume the down event
            } else if (action == KeyEvent.ACTION_UP) {
                Log.d(TAG, "ENTER up - tapping");
                tapAt(SEND_X, SEND_Y, 50);
                return true;  // Consume the up event
            }
        }
        
        // UP arrow - Swipe down (previous reel)
        if (key == KeyEvent.KEYCODE_DPAD_UP) {
            if (action == KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "UP arrow - swipe down");
                swipe(CENTER_X, CENTER_Y - 400, CENTER_X, CENTER_Y + 400, 300);
            }
            return true;
        }
        
        // DOWN arrow - Swipe up (next reel)
        if (key == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (action == KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "DOWN arrow - swipe up");
                swipe(CENTER_X, CENTER_Y + 400, CENTER_X, CENTER_Y - 400, 300);
            }
            return true;
        }
        
        return false;
    }

    private void tapAt(int x, int y, int duration) {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(x, y);
        GestureDescription.StrokeDescription s = 
            new GestureDescription.StrokeDescription(p, 0, duration);
        dispatchGesture(new GestureDescription.Builder().addStroke(s).build(), null, null);
    }

    private void swipe(int x1, int y1, int x2, int y2, int duration) {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        GestureDescription.StrokeDescription s = 
            new GestureDescription.StrokeDescription(p, 0, duration);
        dispatchGesture(new GestureDescription.Builder().addStroke(s).build(), null, null);
    }

    @Override public void onInputDeviceAdded(int id) { checkKbd(); }
    @Override public void onInputDeviceRemoved(int id) { checkKbd(); }
    @Override public void onInputDeviceChanged(int id) { checkKbd(); }
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
echo "✓ MINIMAL FIX APPLIED"
echo "================================================"
echo ""
echo "WHAT THIS FIX DOES:"
echo "  1. SIMPLE ENTER handling - Returns true for both DOWN and UP"
echo "  2. NOTIFICATION FIX - Only shows when keyboard is actually connected"
echo "  3. UP/DOWN arrows work for Reels"
echo ""
echo "IMPORTANT: COORDINATES MAY BE WRONG!"
echo ""
echo "If ENTER doesn't tap the right spot, you need to find the correct"
echo "send button coordinates for YOUR device:"
echo ""
echo "HOW TO FIND CORRECT COORDINATES:"
echo "  1. Enable Developer Options"
echo "  2. Enable 'Pointer location'"
echo "  3. Open Instagram DM"
echo "  4. Tap the send button"
echo "  5. Note the X,Y coordinates shown at top of screen"
echo "  6. Update lines 22-23 in the code above"
echo ""
echo "DEFAULT COORDINATES (Pixel 6):"
echo "  Send button: (990, 2313)"
echo "  Screen center: (540, 1170)"
echo ""
echo "If your screen is different, these won't work!"
echo ""
echo "BUILD AND TEST:"
echo "  ./gradlew clean assembleDebug"
echo "  adb install -r app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "DEBUG LOGS:"
echo "  adb logcat | grep IGKbd"
echo ""
echo "Expected behavior:"
echo "  1. Connect keyboard → 'Kbd state: true' + notification"
echo "  2. Open Instagram → 'IG: true' + notification updates"
echo "  3. In DM, press ENTER → 'ENTER down - consuming' then 'ENTER up - tapping'"
echo "  4. If wrong coordinates, message won't send but also won't insert newline"
echo ""
echo "Backup saved to: $BACKUP/"
echo "To restore: cp $BACKUP/KeyboardTapService.java app/src/main/java/com/example/chris/fstest/"
echo ""
