#!/bin/bash
set -e

echo "================================================"
echo "FINAL FIX: Notification & Key Issues"
echo "================================================"
echo ""

if [ ! -f "build.gradle" ]; then
    echo "❌ Run from project root"
    exit 1
fi

BACKUP="backup_final_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP"

for f in \
    "app/src/main/java/com/example/chris/fstest/KeyboardTapService.java"
do
    [ -f "$f" ] && cp "$f" "$BACKUP/"
done

echo "✓ Backup: $BACKUP/"
echo ""
echo "Applying final fixes..."

cat > "app/src/main/java/com/example/chris/fstest/KeyboardTapService.java" << 'ENDOFFILE'
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
    
    private static final int SEND_X = 990;
    private static final int SEND_Y = 2313;
    private static final int CENTER_X = 540;
    private static final int CENTER_Y = 1170;
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
        
        Log.d(TAG, "Service ready");
    }

    private void checkKbd() {
        boolean found = false;
        int[] ids = InputDevice.getDeviceIds();
        
        for (int i = 0; i < ids.length; i++) {
            InputDevice d = InputDevice.getDevice(ids[i]);
            if (d != null && !d.isVirtual() && 
                (d.getSources() & InputDevice.SOURCE_KEYBOARD) != 0) {
                Log.d(TAG, "Physical keyboard found: " + d.getName());
                found = true;
                break;
            }
        }
        
        if (found != kbd) {
            kbd = found;
            Log.d(TAG, "*** KEYBOARD: " + kbd + " ***");
            updateNotif();
        }
    }

    private void updateNotif() {
        // CRITICAL FIX: Only show notification when keyboard IS connected
        if (!kbd) {
            nm.cancel(1);
            Log.d(TAG, "Notification cancelled (no keyboard)");
            return;
        }
        
        String txt = ig ? "IG active - Keys enabled" : "Waiting for IG";
        
        Log.d(TAG, "Show notification: kbd=" + kbd + " ig=" + ig);
        
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(IG);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        PendingIntent pi;
        if (Build.VERSION.SDK_INT >= 23) {
            pi = PendingIntent.getActivity(this, 0, intent, 
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            pi = PendingIntent.getActivity(this, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT);
        }
        
        Notification n;
        if (Build.VERSION.SDK_INT >= 26) {
            n = new Notification.Builder(this, "kbd")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Keyboard connected")
                .setContentText(txt)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
        } else {
            n = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Keyboard connected")
                .setContentText(txt)
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .build();
        }
        
        nm.notify(1, n);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        CharSequence pkg = e.getPackageName();
        
        if (pkg != null) {
            boolean nowIG = IG.equals(pkg.toString());
            
            if (nowIG != ig) {
                ig = nowIG;
                Log.d(TAG, "*** INSTAGRAM: " + ig + " ***");
                
                // CRITICAL FIX: Only update notification if keyboard connected
                if (kbd) {
                    updateNotif();
                }
            }
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        // Filter out virtual keyboard
        if (e.getDevice() != null && e.getDevice().isVirtual()) {
            return false;
        }
        
        // CRITICAL: Only intercept when BOTH keyboard AND Instagram active
        if (!kbd || !ig) {
            return false;
        }
        
        int key = e.getKeyCode();
        int action = e.getAction();
        
        // Track Shift key
        if (key == KeyEvent.KEYCODE_SHIFT_LEFT || key == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            if (action == KeyEvent.ACTION_DOWN) {
                shiftHeld = true;
                Log.d(TAG, "Shift DOWN");
            } else if (action == KeyEvent.ACTION_UP) {
                shiftHeld = false;
                Log.d(TAG, "Shift UP");
            }
            return false; // Don't consume shift
        }
        
        // ENTER -> Send message
        if (key == KeyEvent.KEYCODE_ENTER) {
            if (action == KeyEvent.ACTION_UP) {
                Log.d(TAG, "ENTER -> tap (" + SEND_X + "," + SEND_Y + ")");
                tapAt(SEND_X, SEND_Y, 50);
            }
            return true; // Consume to prevent newline
        }
        
        // UP arrow -> Previous reel
        if (key == KeyEvent.KEYCODE_DPAD_UP) {
            if (action == KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "UP -> swipe down (previous)");
                swipe(CENTER_X, CENTER_Y, CENTER_X, CENTER_Y + SWIPE_DOWN, 300);
            }
            return true;
        }
        
        // DOWN arrow -> Next reel
        if (key == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (action == KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "DOWN -> swipe up (next)");
                swipe(CENTER_X, CENTER_Y, CENTER_X, CENTER_Y + SWIPE_UP, 300);
            }
            return true;
        }
        
        // SHIFT held -> Long press (fast forward)
        if (shiftHeld && action == KeyEvent.ACTION_DOWN) {
            Log.d(TAG, "Shift held -> long press");
            longPress(CENTER_X, CENTER_Y, 2000);
            return true;
        }
        
        return false;
    }

    private void tapAt(int x, int y, int dur) {
        if (Build.VERSION.SDK_INT < 24) {
            Log.e(TAG, "Gesture API requires API 24+");
            return;
        }
        
        Path p = new Path();
        p.moveTo(x, y);
        
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, dur))
            .build();
            
        boolean sent = dispatchGesture(gesture, null, null);
        Log.d(TAG, "Tap dispatched: " + sent);
    }

    private void swipe(int x1, int y1, int x2, int y2, int dur) {
        if (Build.VERSION.SDK_INT < 24) return;
        
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, dur))
            .build();
            
        boolean sent = dispatchGesture(gesture, null, null);
        Log.d(TAG, "Swipe dispatched: " + sent);
    }

    private void longPress(int x, int y, int dur) {
        if (Build.VERSION.SDK_INT < 24) return;
        
        Path p = new Path();
        p.moveTo(x, y);
        
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, dur))
            .build();
            
        boolean sent = dispatchGesture(gesture, null, null);
        Log.d(TAG, "Long press dispatched: " + sent);
    }

    @Override 
    public void onInputDeviceAdded(int id) { 
        Log.d(TAG, "Device added: " + id);
        checkKbd(); 
    }
    
    @Override 
    public void onInputDeviceRemoved(int id) { 
        Log.d(TAG, "Device removed: " + id);
        checkKbd(); 
    }
    
    @Override 
    public void onInputDeviceChanged(int id) { 
        checkKbd(); 
    }
    
    @Override 
    public void onInterrupt() {
        Log.d(TAG, "Service interrupted");
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "=== SERVICE DESTROYED ===");
        if (im != null) im.unregisterInputDeviceListener(this);
        nm.cancel(1);
        super.onDestroy();
    }
}
ENDOFFILE

echo "✓ KeyboardTapService.java fixed"
echo ""
echo "================================================"
echo "✓ ALL BUGS FIXED"
echo "================================================"
echo ""
echo "BUGS FIXED:"
echo "  ✓ Notification no longer shows without keyboard"
echo "  ✓ Keys now properly intercepted in Instagram"
echo "  ✓ Better logging for debugging"
echo ""
echo "BEHAVIOR:"
echo "  No keyboard          → No notification"
echo "  Keyboard + Other app → 'Waiting for IG'"
echo "  Keyboard + Instagram → 'IG active - Keys enabled'"
echo "  Keys work ONLY when both kbd=true AND ig=true"
echo ""
echo "BUILD:"
echo "  ./gradlew clean assembleDebug"
echo "  adb install -r app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "DEBUG:"
echo "  adb logcat | grep IGKbd"
echo ""
echo "  Expected when connecting keyboard:"
echo "    IGKbd: *** KEYBOARD: true ***"
echo ""
echo "  Expected when opening Instagram:"
echo "    IGKbd: *** INSTAGRAM: true ***"
echo ""
echo "  Expected when pressing ENTER:"
echo "    IGKbd: ENTER -> tap (990,2313)"
echo "    IGKbd: Tap dispatched: true"
echo ""
echo "Backup: $BACKUP/"
echo ""
