package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.hardware.input.InputManager;
import android.support.v4.app.NotificationCompat;

public class KeyboardHelperService extends AccessibilityService
        implements InputManager.InputDeviceListener {

    private static final String IG_PACKAGE = "com.instagram.android";
    private static final int NOTIF_ID = 101;

    private boolean keyboardConnected = false;
    private boolean instagramActive = false;

    private InputManager inputManager;
    private NotificationManager notificationManager;

    @Override
    public void onServiceConnected() {
        inputManager = (InputManager) getSystemService(INPUT_SERVICE);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        inputManager.registerInputDeviceListener(this, null);
        checkKeyboard();
    }

    /* ---------------- KEYBOARD DETECTION ---------------- */

    private void checkKeyboard() {
        boolean found = false;
        int[] ids = InputDevice.getDeviceIds();
        for (int i = 0; i < ids.length; i++) {
            InputDevice d = InputDevice.getDevice(ids[i]);
            if (d != null
                    && !d.isVirtual()
                    && (d.getSources() & InputDevice.SOURCE_KEYBOARD) != 0
                    && d.getKeyboardType() == InputDevice.KEYBOARD_TYPE_ALPHABETIC) {
                found = true;
                break;
            }
        }

        if (found != keyboardConnected) {
            keyboardConnected = found;
            if (keyboardConnected) {
                updateNotification("Keyboard connected | Waiting for Instagram");
            } else {
                instagramActive = false;
                notificationManager.cancel(NOTIF_ID);
            }
        }
    }

    /* ---------------- INSTAGRAM DETECTION ---------------- */

    private void updateInstagramState() {
        if (!keyboardConnected) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null) return;

        boolean nowActive = IG_PACKAGE.contentEquals(root.getPackageName());
        if (nowActive != instagramActive) {
            instagramActive = nowActive;
            if (instagramActive) {
                updateNotification("Keyboard connected | Instagram detected");
            } else {
                updateNotification("Keyboard connected | Waiting for Instagram");
            }
        }
    }

    /* ---------------- NOTIFICATION ---------------- */

    private void updateNotification(String text) {
        if (!keyboardConnected) return;

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    "kbd",
                    "Keyboard Helper",
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationManager.createNotificationChannel(ch);
        }

        notificationManager.notify(
                NOTIF_ID,
                new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.drawable.stat_notify_more)
                        .setOngoing(true)
                        .setContentTitle("Keyboard Helper")
                        .setContentText(text)
                        .build()
        );
    }

    /* ---------------- ACCESSIBILITY EVENTS ---------------- */

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!keyboardConnected) return;

        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                updateInstagramState();
                break;
        }
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (!keyboardConnected) return false;
        if (!instagramActive) return false;

        if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                && event.getAction() == KeyEvent.ACTION_DOWN) {

            updateNotification("IG detected | ENTER intercepted");
            return true; // consume ENTER → no newline
        }

        return false;
    }

    @Override public void onInterrupt() {}

    @Override public void onInputDeviceAdded(int deviceId) { checkKeyboard(); }
    @Override public void onInputDeviceRemoved(int deviceId) { checkKeyboard(); }
    @Override public void onInputDeviceChanged(int deviceId) { checkKeyboard(); }
        }
