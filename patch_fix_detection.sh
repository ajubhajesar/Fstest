#!/bin/bash
set -e

echo "================================================"
echo "FIX: Instagram Detection Not Working"
echo "================================================"
echo ""

if [ ! -f "build.gradle" ]; then
    echo "❌ Run from project root"
    exit 1
fi

BACKUP="backup_fix_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP"

for f in \
    "app/src/main/java/com/example/chris/fstest/KeyboardTapService.java"
do
    [ -f "$f" ] && cp "$f" "$BACKUP/"
done

echo "✓ Backup: $BACKUP/"
echo ""
echo "Applying fix..."

cat > "app/src/main/java/com/example/chris/fstest/KeyboardTapService.java" << 'EOF'
package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
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
    
    // DM Send button coordinates
    private static final int SEND_X = 990;
    private static final int SEND_Y = 2313;
    
    // Screen center for swipe gestures
    private static final int CENTER_X = 540;
    private static final int CENTER_Y = 1170;
    
    // Swipe distances
    private static final int SWIPE_UP = -800;
    private static final int SWIPE_DOWN = 800;

    private InputManager im;
    private NotificationManager nm;
    private boolean kbd = false;
    private boolean ig = false;
    private boolean shiftHeld = false;

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
        
        Log.d(TAG, "Service initialized successfully");
    }

    private void checkKbd() {
        boolean found = false;
        int[] ids = InputDevice.getDeviceIds();
        
        Log.d(TAG, "Checking " + ids.length + " input devices");
        
        for (int i = 0; i < ids.length; i++) {
            InputDevice d = InputDevice.getDevice(ids[i]);
            if (d != null) {
                boolean isVirtual = d.isVirtual();
                boolean hasKeyboard = (d.getSources() & InputDevice.SOURCE_KEYBOARD) != 0;
                
                Log.d(TAG, "Device " + i + ": " + d.getName() + 
                      " virtual=" + isVirtual + " keyboard=" + hasKeyboard);
                
                if (!isVirtual && hasKeyboard) {
                    found = true;
                }
            }
        }
        
        if (found != kbd) {
            kbd = found;
            Log.d(TAG, "*** KEYBOARD STATE CHANGED: " + kbd + " ***");
            notify();
        }
    }

    private void notify() {
        if (!kbd) {
            nm.cancel(1);
            Log.d(TAG, "Notification CANCELLED (no keyboard)");
            return;
        }
        
        String txt = ig ? "IG active - Keys enabled" : "Waiting for IG";
        
        Log.d(TAG, "Creating notification: kbd=" + kbd + " ig=" + ig + " text=" + txt);
        
        // Create intent to open Instagram
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(IG);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= 23) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT);
        }
        
        Notification n;
        if (Build.VERSION.SDK_INT >= 26) {
            n = new Notification.Builder(this, "kbd")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Keyboard connected")
                .setContentText(txt)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
        } else {
            n = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Keyboard connected")
                .setContentText(txt)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .build();
        }
        
        nm.notify(1, n);
        Log.d(TAG, "Notification SHOWN: " + txt);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        int eventType = e.getEventType();
        CharSequence pkg = e.getPackageName();
        
        Log.d(TAG, "Event: type=" + eventType + " pkg=" + pkg);
        
        if (pkg != null) {
            boolean nowIG = IG.equals(pkg.toString());
            
            if (nowIG != ig) {
                ig = nowIG;
                Log.d(TAG, "*** INSTAGRAM STATE CHANGED: " + ig + " ***");
                if (kbd) {
                    notify();
                }
            }
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        if (e.getDevice() != null && e.getDevice().isVirtual()) {
            return false;
        }
        
        if (!kbd || !ig) {
            return false;
        }
        
        int key = e.getKeyCode();
        int action = e.getAction();
        
        // Track Shift
        if (key == KeyEvent.KEYCODE_SHIFT_LEFT || key == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            if (action == KeyEvent.ACTION_DOWN) {
                shiftHeld = true;
                Log.d(TAG, "Shift DOWN");
            } else if (action == KeyEvent.ACTION_UP) {
                shiftHeld = false;
                Log.d(TAG, "Shift UP");
            }
            return false;
        }
        
        // ENTER -> Send
        if (key == KeyEvent.KEYCODE_ENTER) {
            if (action == KeyEvent.ACTION_UP) {
                Log.d(TAG, "ENTER -> tap Send at (" + SEND_X + "," + SEND_Y + ")");
                tapAt(SEND_X, SEND_Y, 50);
            }
            return true;
        }
        
        // UP -> Previous reel
        if (key == KeyEvent.KEYCODE_DPAD_UP) {
            if (action == KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "UP -> swipe down (previous reel)");
                swipe(CENTER_X, CENTER_Y, CENTER_X, CENTER_Y + SWIPE_DOWN, 300);
            }
            return true;
        }
        
        // DOWN -> Next reel
        if (key == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (action == KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "DOWN -> swipe up (next reel)");
                swipe(CENTER_X, CENTER_Y, CENTER_X, CENTER_Y + SWIPE_UP, 300);
            }
            return true;
        }
        
        // SHIFT held -> Long press
        if (shiftHeld && action == KeyEvent.ACTION_DOWN) {
            Log.d(TAG, "Shift held -> long press for fast forward");
            longPress(CENTER_X, CENTER_Y, 2000);
            return true;
        }
        
        return false;
    }

    private void tapAt(int x, int y, int duration) {
        if (Build.VERSION.SDK_INT < 24) {
            Log.d(TAG, "Gesture API not available (API < 24)");
            return;
        }
        Path p = new Path();
        p.moveTo(x, y);
        GestureDescription.StrokeDescription s = 
            new GestureDescription.StrokeDescription(p, 0, duration);
        boolean dispatched = dispatchGesture(
            new GestureDescription.Builder().addStroke(s).build(), null, null);
        Log.d(TAG, "Tap dispatched: " + dispatched);
    }

    private void swipe(int x1, int y1, int x2, int y2, int duration) {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        GestureDescription.StrokeDescription s = 
            new GestureDescription.StrokeDescription(p, 0, duration);
        boolean dispatched = dispatchGesture(
            new GestureDescription.Builder().addStroke(s).build(), null, null);
        Log.d(TAG, "Swipe dispatched: " + dispatched);
    }

    private void longPress(int x, int y, int duration) {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(x, y);
        GestureDescription.StrokeDescription s = 
            new GestureDescription.StrokeDescription(p, 0, duration);
        boolean dispatched = dispatchGesture(
            new GestureDescription.Builder().addStroke(s).build(), null, null);
        Log.d(TAG, "Long press dispatched: " + dispatched);
    }

    @Override 
    public void onInputDeviceAdded(int id) { 
        Log.d(TAG, ">>> Device ADDED: " + id);
        checkKbd(); 
    }
    
    @Override 
    public void onInputDeviceRemoved(int id) { 
        Log.d(TAG, ">>> Device REMOVED: " + id);
        checkKbd(); 
    }
    
    @Override 
    public void onInputDeviceChanged(int id) { 
        Log.d(TAG, ">>> Device CHANGED: " + id);
        checkKbd(); 
    }
    
    @Override 
    public void onInterrupt() {
        Log.d(TAG, "!!! SERVICE INTERRUPTED !!!");
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "=== SERVICE DESTROYED ===");
        if (im != null) im.unregisterInputDeviceListener(this);
        nm.cancel(1);
        super.onDestroy();
    }
}
EOF

echo "✓ KeyboardTapService.java fixed"
echo ""
echo "================================================"
echo "✓ FIX APPLIED"
echo "================================================"
echo ""
echo "WHAT WAS FIXED:"
echo "  ✓ Removed setServiceInfo() override that broke event handling"
echo "  ✓ Added extensive debug logging"
echo "  ✓ Now relies on XML configuration (which is correct)"
echo ""
echo "FEATURES:"
echo "  ✓ ENTER - Send in DM"
echo "  ✓ UP - Previous reel"
echo "  ✓ DOWN - Next reel"
echo "  ✓ SHIFT hold - Fast forward"
echo "  ✓ Notification tap - Opens Instagram"
echo ""
echo "BUILD:"
echo "  ./gradlew clean assembleDebug"
echo "  adb install -r app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "TESTING:"
echo "  1. In separate terminal: adb logcat | grep IGKbd"
echo "  2. Connect keyboard - should see device detection logs"
echo "  3. Open Instagram - should see 'INSTAGRAM STATE CHANGED: true'"
echo "  4. Notification should update to 'IG active - Keys enabled'"
echo ""
echo "If Instagram STILL not detected:"
echo "  Check logs for 'Event: type=X pkg=Y'"
echo "  You should see events when switching apps"
echo "  Package should be 'com.instagram.android'"
echo ""
echo "Backup: $BACKUP/"
echo ""
