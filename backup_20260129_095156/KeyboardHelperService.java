package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Path;
import android.hardware.input.InputManager;
import android.os.Build;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.support.v4.app.NotificationCompat;

public class KeyboardHelperService extends AccessibilityService
        implements InputManager.InputDeviceListener {

    private static final String IG = "com.instagram.android";
    private static final int X = 990;
    private static final int Y = 2313;

    private boolean keyboard = false;
    private boolean instagram = false;
    private InputManager im;
    private NotificationManager nm;

    @Override
    public void onServiceConnected() {
        im = (InputManager) getSystemService(INPUT_SERVICE);
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        im.registerInputDeviceListener(this, null);
        checkKeyboard();
    }

    private void checkKeyboard() {
        boolean found = false;
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice d = InputDevice.getDevice(id);
            if (d != null && !d.isVirtual()
                    && (d.getSources() & InputDevice.SOURCE_KEYBOARD) != 0) {
                found = true;
            }
        }
        if (found != keyboard) {
            keyboard = found;
            if (keyboard) showNotification();
            else nm.cancel(1);
        }
    }

    private void showNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch =
                    new NotificationChannel("kbd","Keyboard",
                            NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(ch);
        }
        nm.notify(1, new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setOngoing(true)
                .setContentTitle("Keyboard connected")
                .setContentText(instagram ? "Instagram detected" : "Waiting for Instagram")
                .build());
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            instagram = IG.contentEquals(e.getPackageName());
            if (keyboard) showNotification();
        }
    }

    @Override
    public boolean onKeyEvent(KeyEvent e) {
        if (keyboard && instagram &&
                e.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                e.getAction() == KeyEvent.ACTION_UP) {
            tapSend();
            return true;
        }
        return false;
    }

    private void tapSend() {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(X, Y);
        p.lineTo(X, Y);
        dispatchGesture(new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(p,0,50))
                .build(), null, null);
    }

    @Override public void onInterrupt() {}
    @Override public void onInputDeviceAdded(int i){checkKeyboard();}
    @Override public void onInputDeviceRemoved(int i){checkKeyboard();}
    @Override public void onInputDeviceChanged(int i){checkKeyboard();}
}
