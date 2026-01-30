#!/bin/bash

# Instagram Keyboard Helper - Fixed Patch
# Fixes: Instagram detection, ENTER (not Shift+Enter), notifications, battery optimization

set -e

echo "================================================"
echo "Instagram Keyboard Helper - FIXED PATCH"
echo "================================================"
echo ""

if [ ! -f "build.gradle" ] || [ ! -d "app/src/main" ]; then
    echo "❌ ERROR: Run this from project root directory"
    exit 1
fi

echo "✓ Project detected"

# Backup
BACKUP="backup_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP"

if [ -f "app/src/main/java/com/example/chris/fstest/KeyboardHelperService.java" ]; then
    cp "app/src/main/java/com/example/chris/fstest/KeyboardHelperService.java" "$BACKUP/"
fi

echo "✓ Backup: $BACKUP/"
echo ""
echo "Applying fixes..."

# Update KeyboardHelperService.java
cat > "app/src/main/java/com/example/chris/fstest/KeyboardHelperService.java" << 'EOF'
package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Path;
import android.hardware.input.InputManager;
import android.os.Build;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class KeyboardHelperService extends AccessibilityService
        implements InputManager.InputDeviceListener {

    private static final String INSTAGRAM = "com.instagram.android";
    private static final String CHANNEL_ID = "ig_kbd";
    private static final int NOTIFY_ID = 100;
    
    private static final int SEND_X = 990;
    private static final int SEND_Y = 2313;

    private InputManager inputManager;
    private NotificationManager notifyManager;
    
    private boolean hasKeyboard = false;
    private boolean isInstagram = false;

    @Override
    public void onServiceConnected() {
        inputManager = (InputManager) getSystemService(INPUT_SERVICE);
        notifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        
        setupChannel();
        inputManager.registerInputDeviceListener(this, null);
        checkKeyboard();
    }

    private void setupChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, 
                "Instagram Keyboard", 
                NotificationManager.IMPORTANCE_LOW
            );
            ch.setShowBadge(false);
            notifyManager.createNotificationChannel(ch);
        }
    }

    private void checkKeyboard() {
        boolean found = false;
        int[] ids = InputDevice.getDeviceIds();
        
        for (int i = 0; i < ids.length; i++) {
            InputDevice dev = InputDevice.getDevice(ids[i]);
            if (dev != null && !dev.isVirtual()) {
                if ((dev.getSources() & InputDevice.SOURCE_KEYBOARD) != 0) {
                    found = true;
                    break;
                }
            }
        }
        
        if (found != hasKeyboard) {
            hasKeyboard = found;
            updateNotify();
        }
    }

    private void updateNotify() {
        if (!hasKeyboard) {
            notifyManager.cancel(NOTIFY_ID);
            return;
        }

        String text = isInstagram ? 
            "Instagram active - ENTER sends" : 
            "Waiting for Instagram";

        Notification n;
        if (Build.VERSION.SDK_INT >= 26) {
            n = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Physical keyboard connected")
                .setContentText(text)
                .setOngoing(true)
                .build();
        } else {
            n = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Physical keyboard connected")
                .setContentText(text)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .build();
        }

        notifyManager.notify(NOTIFY_ID, n);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Only check window state changes
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }

        CharSequence pkg = event.getPackageName();
        boolean nowIG = (pkg != null && INSTAGRAM.equals(pkg.toString()));
        
        if (nowIG != isInstagram) {
            isInstagram = nowIG;
            if (hasKeyboard) {
                updateNotify();
            }
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        // Ignore virtual keyboard
        if (event.getDevice() != null && event.getDevice().isVirtual()) {
            return false;
        }

        // Only when keyboard connected AND Instagram active
        if (!hasKeyboard || !isInstagram) {
            return false;
        }

        // Only ENTER key
        if (event.getKeyCode() != KeyEvent.KEYCODE_ENTER) {
            return false;
        }

        // Tap on UP, but consume both DOWN and UP
        if (event.getAction() == KeyEvent.ACTION_UP) {
            tapSend();
        }
        
        return true; // Consume ENTER to prevent newline
    }

    private void tapSend() {
        if (Build.VERSION.SDK_INT < 24) {
            return;
        }

        Path p = new Path();
        p.moveTo(SEND_X, SEND_Y);
        
        GestureDescription.StrokeDescription stroke = 
            new GestureDescription.StrokeDescription(p, 0, 50);
        
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(stroke)
            .build();

        dispatchGesture(gesture, null, null);
    }

    @Override
    public void onInputDeviceAdded(int id) {
        checkKeyboard();
    }

    @Override
    public void onInputDeviceRemoved(int id) {
        checkKeyboard();
    }

    @Override
    public void onInputDeviceChanged(int id) {
        checkKeyboard();
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        if (inputManager != null) {
            inputManager.unregisterInputDeviceListener(this);
        }
        notifyManager.cancel(NOTIFY_ID);
        super.onDestroy();
    }
}
EOF

echo "✓ KeyboardHelperService.java updated"
echo ""
echo "================================================"
echo "✓ PATCH APPLIED SUCCESSFULLY"
echo "================================================"
echo ""
echo "FIXES APPLIED:"
echo "  ✓ Instagram detection using TYPE_WINDOW_STATE_CHANGED"
echo "  ✓ ENTER key only (removed Shift requirement)"
echo "  ✓ ENTER only works when Instagram is active"
echo "  ✓ Notification shows keyboard + Instagram status"
echo "  ✓ Battery optimized (event-driven, no polling)"
echo ""
echo "BEHAVIOR:"
echo "  • Keyboard connected → notification appears"
echo "  • Open Instagram → notification updates"
echo "  • ENTER in Instagram → sends message"
echo "  • ENTER outside Instagram → normal behavior"
echo ""
echo "BUILD:"
echo "  ./gradlew assembleDebug"
echo ""
echo "Backup saved: $BACKUP/"
echo ""
