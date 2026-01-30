#!/bin/bash

set -e

echo "=========================================="
echo "Instagram Keyboard Helper - COMPLETE FIX"
echo "=========================================="
echo ""

if [ ! -f "build.gradle" ] || [ ! -d "app/src/main" ]; then
    echo "❌ ERROR: Run from project root"
    exit 1
fi

echo "✓ Project detected"

# Backup
BACKUP="backup_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP"

for file in \
    "app/src/main/AndroidManifest.xml" \
    "app/src/main/res/xml/accessibility_service.xml" \
    "app/src/main/java/com/example/chris/fstest/KeyboardHelperService.java"
do
    if [ -f "$file" ]; then
        cp "$file" "$BACKUP/"
    fi
done

echo "✓ Backup: $BACKUP/"
echo ""
echo "Applying fixes..."

# Fix AndroidManifest.xml - CRITICAL
cat > "app/src/main/AndroidManifest.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.chris.fstest">

    <application
        android:label="IG Keyboard Helper"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher">

        <activity android:name=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>

        <service
            android:name=".KeyboardHelperService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="false">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService"/>
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service"/>
        </service>

    </application>
</manifest>
EOF

echo "✓ AndroidManifest.xml fixed"

# Fix accessibility_service.xml - CRITICAL
cat > "app/src/main/res/xml/accessibility_service.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="100"
    android:canRequestFilterKeyEvents="true"
    android:accessibilityFlags="flagRequestFilterKeyEvents|flagReportViewIds" />
EOF

echo "✓ accessibility_service.xml fixed"

# Update KeyboardHelperService with debug logs
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
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class KeyboardHelperService extends AccessibilityService
        implements InputManager.InputDeviceListener {

    private static final String TAG = "IGKeyboard";
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
        Log.d(TAG, "Service connected");
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
                    Log.d(TAG, "Physical keyboard found: " + dev.getName());
                    found = true;
                    break;
                }
            }
        }
        
        if (found != hasKeyboard) {
            hasKeyboard = found;
            Log.d(TAG, "Keyboard state changed: " + hasKeyboard);
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
        Log.d(TAG, "Notification updated: kbd=" + hasKeyboard + " ig=" + isInstagram);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }

        CharSequence pkg = event.getPackageName();
        if (pkg != null) {
            Log.d(TAG, "Window changed: " + pkg.toString());
        }
        
        boolean nowIG = (pkg != null && INSTAGRAM.equals(pkg.toString()));
        
        if (nowIG != isInstagram) {
            isInstagram = nowIG;
            Log.d(TAG, "Instagram state changed: " + isInstagram);
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

        Log.d(TAG, "ENTER pressed - action: " + event.getAction());

        // Tap on UP, but consume both DOWN and UP
        if (event.getAction() == KeyEvent.ACTION_UP) {
            tapSend();
        }
        
        return true; // Consume ENTER to prevent newline
    }

    private void tapSend() {
        if (Build.VERSION.SDK_INT < 24) {
            Log.d(TAG, "API level too low for gesture");
            return;
        }

        Log.d(TAG, "Tapping at (" + SEND_X + "," + SEND_Y + ")");

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
        Log.d(TAG, "Input device added: " + id);
        checkKeyboard();
    }

    @Override
    public void onInputDeviceRemoved(int id) {
        Log.d(TAG, "Input device removed: " + id);
        checkKeyboard();
    }

    @Override
    public void onInputDeviceChanged(int id) {
        checkKeyboard();
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Service interrupted");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed");
        if (inputManager != null) {
            inputManager.unregisterInputDeviceListener(this);
        }
        notifyManager.cancel(NOTIFY_ID);
        super.onDestroy();
    }
}
EOF

echo "✓ KeyboardHelperService.java updated with debug logs"

# Remove old/conflicting files
rm -f "app/src/main/res/xml/accessibility_service_config.xml"
echo "✓ Removed old config files"

echo ""
echo "=========================================="
echo "✓ PATCH APPLIED SUCCESSFULLY"
echo "=========================================="
echo ""
echo "CRITICAL FIXES:"
echo "  ✓ Manifest now points to KeyboardHelperService"
echo "  ✓ Manifest now points to accessibility_service.xml"
echo "  ✓ Accessibility config has correct event types"
echo "  ✓ Debug logs added for troubleshooting"
echo ""
echo "BUILD & INSTALL:"
echo "  ./gradlew clean"
echo "  ./gradlew assembleDebug"
echo "  adb install -r app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "ENABLE SERVICE:"
echo "  1. Settings → Accessibility"
echo "  2. Find 'KeyboardHelperService' or 'IG Keyboard Helper'"
echo "  3. Turn ON"
echo ""
echo "DEBUG LOGS:"
echo "  adb logcat | grep IGKeyboard"
echo ""
echo "Backup: $BACKUP/"
echo ""
