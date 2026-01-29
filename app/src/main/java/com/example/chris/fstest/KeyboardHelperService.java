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

    private static final String IG = "com.instagram.android";
    private static final String CH = "kbd";
    private static final int ID = 1;

    // PORTRAIT COORDINATES
    private static final int X = 990;
    private static final int Y = 2313;

    private boolean kb = false;
    private boolean ig = false;

    private InputManager im;
    private NotificationManager nm;

    @Override
    public void onServiceConnected() {
        im = (InputManager) getSystemService(INPUT_SERVICE);
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c =
                new NotificationChannel(CH, "Keyboard Helper",
                        NotificationManager.IMPORTANCE_LOW);
            c.setShowBadge(false);
            nm.createNotificationChannel(c);
        }

        im.registerInputDeviceListener(this, null);
        check();
    }

    private void check() {
        boolean f = false;
        for (int i : InputDevice.getDeviceIds()) {
            InputDevice d = InputDevice.getDevice(i);
            if (d != null && !d.isVirtual() &&
                (d.getSources() & InputDevice.SOURCE_KEYBOARD) != 0) {
                f = true;
                break;
            }
        }
        if (f != kb) {
            kb = f;
            notifyState();
        }
    }

    private void notifyState() {
        if (!kb) {
            nm.cancel(ID);
            return;
        }
        String t = ig ? "Instagram detected – ENTER sends"
                      : "Keyboard connected – waiting for Instagram";

        Notification n = Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(this, CH)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle("Keyboard Helper")
                .setContentText(t)
                .setOngoing(true).build()
            : new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle("Keyboard Helper")
                .setContentText(t)
                .setOngoing(true).build();

        nm.notify(ID, n);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() ==
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            boolean n = e.getPackageName() != null &&
                        IG.contentEquals(e.getPackageName());

            if (n != ig) {
                ig = n;
                if (kb) notifyState();
            }
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        if (kb && ig && e.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            if (e.getAction() == KeyEvent.ACTION_UP) send();
            return true;
        }
        return false;
    }

    private void send() {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(X, Y);
        GestureDescription g =
            new GestureDescription.Builder()
                .addStroke(new GestureDescription
                    .StrokeDescription(p, 0, 40))
                .build();
        dispatchGesture(g, null, null);
    }

    @Override public void onInterrupt() {}
    @Override public void onInputDeviceAdded(int i){ check(); }
    @Override public void onInputDeviceRemoved(int i){ check(); }
    @Override public void onInputDeviceChanged(int i){ check(); }

    @Override
    public void onDestroy() {
        im.unregisterInputDeviceListener(this);
        nm.cancel(ID);
        super.onDestroy();
    }
}
