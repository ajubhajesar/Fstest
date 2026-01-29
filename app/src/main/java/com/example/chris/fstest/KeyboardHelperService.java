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

import androidx.core.app.NotificationCompat;

public class KeyboardHelperService extends AccessibilityService
        implements InputManager.InputDeviceListener {

    private static final String IG_PACKAGE = "com.instagram.android";
    private static final int NOTIF_ID = 101;
    private static final int SEND_X = 990;
    private static final int SEND_Y = 2313;

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

    private void checkKeyboard() {
        boolean found = false;
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice d = InputDevice.getDevice(id);
            if (d != null && !d.isVirtual()
                    && (d.getSources() & InputDevice.SOURCE_KEYBOARD) != 0
                    && d.getKeyboardType() == InputDevice.KEYBOARD_TYPE_ALPHABETIC) {
                found = true;
                break;
            }
        }
        if (found != keyboardConnected) {
            keyboardConnected = found;
            if (keyboardConnected) showNotification();
            else notificationManager.cancel(NOTIF_ID);
        }
    }

    private void showNotification() {
        String ch = "kbd";
        if (Build.VERSION.SDK_INT >= 26) {
            notificationManager.createNotificationChannel(
                    new NotificationChannel(ch, "Keyboard Helper",
                            NotificationManager.IMPORTANCE_LOW));
        }
        Notification n = new NotificationCompat.Builder(this, ch)
                .setContentTitle("Keyboard connected")
                .setContentText(instagramActive ? "Instagram detected" : "Waiting for Instagram")
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setOngoing(true)
                .build();
        notificationManager.notify(NOTIF_ID, n);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence p = e.getPackageName();
            instagramActive = p != null && IG_PACKAGE.contentEquals(p);
            if (keyboardConnected) showNotification();
        }
    }

    @Override
    public boolean onKeyEvent(KeyEvent e) {
        if (!keyboardConnected || !instagramActive) return false;
        if (e.getKeyCode() == KeyEvent.KEYCODE_ENTER
                && e.getAction() == KeyEvent.ACTION_UP) {
            tap(SEND_X, SEND_Y);
            return true;
        }
        return false;
    }

    private void tap(int x, int y) {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(x, y);
        p.lineTo(x, y);
        dispatchGesture(new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(p, 0, 50))
                .build(), null, null);
    }

    @Override public void onInterrupt() {}
    @Override public void onInputDeviceAdded(int i) { checkKeyboard(); }
    @Override public void onInputDeviceRemoved(int i) { checkKeyboard(); }
    @Override public void onInputDeviceChanged(int i) { checkKeyboard(); }
}
