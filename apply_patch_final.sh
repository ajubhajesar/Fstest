#!/bin/bash
set -e

echo "Applying FINAL Instagram ENTER → SEND patch"

BASE=app/src/main

# ===============================
# KeyboardHelperService.java
# ===============================
cat <<'JAVA' > $BASE/java/com/example/chris/fstest/KeyboardHelperService.java
package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.NotificationChannel;

public class KeyboardHelperService extends AccessibilityService
        implements InputManager.InputDeviceListener {

    private static final String IG_PKG = "com.instagram.android";
    private static final int NOTIF_ID = 1001;
    private static final String CH_ID = "kbd";

    // CONFIRMED COORDINATES (PORTRAIT ONLY)
    private static final int SEND_X = 990;
    private static final int SEND_Y = 2313;

    private boolean keyboardConnected = false;
    private boolean instagramForeground = false;
    private boolean notifVisible = false;

    private NotificationManager nm;
    private InputManager im;
    private Handler h = new Handler(Looper.getMainLooper());

    @Override
    protected void onServiceConnected() {
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        im = (InputManager) getSystemService(Context.INPUT_SERVICE);
        im.registerInputDeviceListener(this, null);

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CH_ID, "Keyboard Helper",
                    NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(ch);
        }

        checkKeyboard();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence pkg = e.getPackageName();
            boolean ig = pkg != null && IG_PKG.contentEquals(pkg);
            if (ig != instagramForeground) {
                instagramForeground = ig;
                updateNotif();
            }
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (!keyboardConnected) return false;
        if (!instagramForeground) return false;

        if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                event.getAction() == KeyEvent.ACTION_DOWN) {

            h.postDelayed(new Runnable() {
                @Override public void run() {
                    tap(SEND_X, SEND_Y);
                }
            }, 70);

            return true; // CONSUME ENTER — NO NEW LINE
        }
        return false;
    }

    private void tap(int x, int y) {
        if (Build.VERSION.SDK_INT < 24) return;

        Path p = new Path();
        p.moveTo(x, y);

        GestureDescription.StrokeDescription s =
                new GestureDescription.StrokeDescription(p, 0, 50);

        GestureDescription g =
                new GestureDescription.Builder().addStroke(s).build();

        dispatchGesture(g, null, null);
    }

    private void checkKeyboard() {
        boolean found = false;
        int[] ids = InputDevice.getDeviceIds();
        for (int id : ids) {
            InputDevice d = InputDevice.getDevice(id);
            if (d != null &&
                d.supportsSource(InputDevice.SOURCE_KEYBOARD) &&
                !d.isVirtual()) {
                found = true;
                break;
            }
        }

        if (found != keyboardConnected) {
            keyboardConnected = found;
            if (!keyboardConnected) instagramForeground = false;
            updateNotif();
        }
    }

    private void updateNotif() {
        if (!keyboardConnected) {
            nm.cancel(NOTIF_ID);
            notifVisible = false;
            return;
        }

        String text = instagramForeground
                ? "Instagram detected – ENTER sends"
                : "Keyboard connected – waiting for Instagram";

        Notification n;
        if (Build.VERSION.SDK_INT >= 26) {
            n = new Notification.Builder(this, CH_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_keyboard)
                    .setContentTitle("Keyboard Helper")
                    .setContentText(text)
                    .setOngoing(false)
                    .build();
        } else {
            n = new Notification.Builder(this)
                    .setSmallIcon(android.R.drawable.stat_sys_keyboard)
                    .setContentTitle("Keyboard Helper")
                    .setContentText(text)
                    .setOngoing(false)
                    .build();
        }

        nm.notify(NOTIF_ID, n);
        notifVisible = true;
    }

    @Override public void onInputDeviceAdded(int id) { checkKeyboard(); }
    @Override public void onInputDeviceRemoved(int id) { checkKeyboard(); }
    @Override public void onInputDeviceChanged(int id) { checkKeyboard(); }

    @Override public void onInterrupt() {}
}
JAVA

# ===============================
# MainActivity.java (NO AndroidX)
# ===============================
cat <<'JAVA' > $BASE/java/com/example/chris/fstest/MainActivity.java
package com.example.chris.fstest;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.content.Intent;
import android.provider.Settings;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        TextView t = new TextView(this);
        t.setText("Enable Accessibility Service\nThen connect keyboard and open Instagram");
        setContentView(t);
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }
}
JAVA

# ===============================
# AndroidManifest.xml
# ===============================
cat <<'XML' > $BASE/AndroidManifest.xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.chris.fstest">

    <application
        android:allowBackup="true"
        android:label="Fstest"
        android:supportsRtl="true">

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
                android:resource="@xml/accessibility_service_config"/>
        </service>

    </application>
</manifest>
XML

echo "DONE. Now run:"
echo "git add ."
echo "git commit -m \"FINAL: Instagram ENTER → SEND (gesture only)\""
