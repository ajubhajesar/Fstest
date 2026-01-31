#!/bin/bash
set -e

echo "================================================"
echo "FIX: Build Error - notify() Method Conflict"
echo "================================================"
echo ""

if [ ! -f "build.gradle" ]; then
    echo "❌ Run from project root"
    exit 1
fi

BACKUP="backup_compile_fix_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP"

for f in \
    "app/src/main/java/com/example/chris/fstest/KeyboardTapService.java"
do
    [ -f "$f" ] && cp "$f" "$BACKUP/"
done

echo "✓ Backup: $BACKUP/"
echo ""
echo "Fixing build error..."

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
            updateNotif();
        }
    }

    private void updateNotif() {
        if (!kbd) {
            nm.cancel(1);
            Log.d(TAG, "Notification CANCELLED (no keyboard)");
            return;
        }
        
        String txt = ig ? "IG active - Keys enabled" : "Waiting for IG";
        
        Log.d(TAG, "Creating notification: kbd=" + kbd + " ig=" + ig + " text=" + txt);
        
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
                    updateNotif();
                }
            }
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        if (e.getDevice() != null && e.getDevice().isVirtual()) return false;
        if (!kbd || !ig) return false;
        
        int key = e.getKeyCode();
        int action = e.getAction();
        
        if (key == KeyEvent.KEYCODE_SHIFT_LEFT || key == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            shiftHeld = (action == KeyEvent.ACTION_DOWN);
            Log.d(TAG, "Shift " + (shiftHeld ? "DOWN" : "UP"));
            return false;
        }
        
        if (key == KeyEvent.KEYCODE_ENTER) {
            if (action == KeyEvent.ACTION_UP) {
                Log.d(TAG, "ENTER -> tap Send");
                tapAt(SEND_X, SEND_Y, 50);
            }
            return true;
        }
        
        if (key == KeyEvent.KEYCODE_DPAD_UP) {
            if (action == KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "UP -> swipe down");
                swipe(CENTER_X, CENTER_Y, CENTER_X, CENTER_Y + SWIPE_DOWN, 300);
            }
            return true;
        }
        
        if (key == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (action == KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "DOWN -> swipe up");
                swipe(CENTER_X, CENTER_Y, CENTER_X, CENTER_Y + SWIPE_UP, 300);
            }
            return true;
        }
        
        if (shiftHeld && action == KeyEvent.ACTION_DOWN) {
            Log.d(TAG, "Shift held -> long press");
            longPress(CENTER_X, CENTER_Y, 2000);
            return true;
        }
        
        return false;
    }

    private void tapAt(int x, int y, int dur) {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(x, y);
        dispatchGesture(new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, dur))
            .build(), null, null);
    }

    private void swipe(int x1, int y1, int x2, int y2, int dur) {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        dispatchGesture(new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, dur))
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

    @Override public void onInputDeviceAdded(int id) { 
        Log.d(TAG, "Device ADDED: " + id);
        checkKbd(); 
    }
    
    @Override public void onInputDeviceRemoved(int id) { 
        Log.d(TAG, "Device REMOVED: " + id);
        checkKbd(); 
    }
    
    @Override public void onInputDeviceChanged(int id) { checkKbd(); }
    @Override public void onInterrupt() {}
    
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
echo "✓ BUILD ERROR FIXED"
echo "================================================"
echo ""
echo "WHAT WAS FIXED:"
echo "  ✓ Renamed notify() to updateNotif()"
echo "  ✓ Avoided Object.notify() method conflict"
echo ""
echo "BUILD:"
echo "  ./gradlew clean assembleDebug"
echo ""
echo "Backup: $BACKUP/"
echo ""
