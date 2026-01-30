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

    // Screen center
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
        Log.d(TAG, "Service started");

        im = (InputManager) getSystemService(INPUT_SERVICE);
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    "kbd", "Keyboard", NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }

        im.registerInputDeviceListener(this, null);
        checkKbd();
    }

    private void checkKbd() {
        boolean found = false;
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice d = InputDevice.getDevice(id);
            if (d != null && !d.isVirtual()
                    && (d.getSources() & InputDevice.SOURCE_KEYBOARD) != 0) {
                found = true;
                break;
            }
        }

        if (found != kbd) {
            kbd = found;
            Log.d(TAG, "Kbd state: " + kbd);
            notifyState();
        }
    }

    private void notifyState() {
        if (!kbd) {
            nm.cancel(1);
            return;
        }

        String text = ig ? "IG active - Keys enabled" : "Waiting for IG";

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(IG);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pi = PendingIntent.getActivity(
                this, 0, intent,
                Build.VERSION.SDK_INT >= 23
                        ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification n = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, "kbd")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Keyboard connected")
                    .setContentText(text)
                    .setOngoing(true)
                    .setContentIntent(pi)
                    .build()
                : new Notification.Builder(this)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Keyboard connected")
                    .setContentText(text)
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_LOW)
                    .setContentIntent(pi)
                    .build();

        nm.notify(1, n);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        int type = e.getEventType();

        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                || type == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {

            CharSequence pkg = e.getPackageName();
            boolean now = pkg != null && pkg.toString().startsWith(IG);

            if (now != ig) {
                ig = now;
                Log.d(TAG, "IG: " + ig + " pkg: " + pkg);
                if (kbd) notifyState();
            }
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        if (e.getDevice() != null && e.getDevice().isVirtual()) return false;
        if (!kbd || !ig) return false;

        int key = e.getKeyCode();
        int action = e.getAction();

        if (key == KeyEvent.KEYCODE_SHIFT_LEFT
                || key == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            shiftHeld = (action == KeyEvent.ACTION_DOWN);
            return false;
        }

        if (key == KeyEvent.KEYCODE_ENTER) {
            if (action == KeyEvent.ACTION_UP) {
                tapAt(SEND_X, SEND_Y, 50);
            }
            return true;
        }

        if (key == KeyEvent.KEYCODE_DPAD_UP && action == KeyEvent.ACTION_DOWN) {
            swipe(CENTER_X, CENTER_Y, CENTER_X, CENTER_Y + SWIPE_DOWN, 300);
            return true;
        }

        if (key == KeyEvent.KEYCODE_DPAD_DOWN && action == KeyEvent.ACTION_DOWN) {
            swipe(CENTER_X, CENTER_Y, CENTER_X, CENTER_Y + SWIPE_UP, 300);
            return true;
        }

        if (shiftHeld && action == KeyEvent.ACTION_DOWN) {
            longPress(CENTER_X, CENTER_Y, 2000);
            return true;
        }

        return false;
    }

    private void tapAt(int x, int y, int d) {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(x, y);
        dispatchGesture(new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(p, 0, d))
                .build(), null, null);
    }

    private void swipe(int x1, int y1, int x2, int y2, int d) {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        dispatchGesture(new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(p, 0, d))
                .build(), null, null);
    }

    private void longPress(int x, int y, int d) {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(x, y);
        dispatchGesture(new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(p, 0, d))
                .build(), null, null);
    }

    @Override public void onInputDeviceAdded(int id) { checkKbd(); }
    @Override public void onInputDeviceRemoved(int id) { checkKbd(); }
    @Override public void onInputDeviceChanged(int id) { checkKbd(); }

    @Override public void onInterrupt() {}

    @Override
    public void onDestroy() {
        if (im != null) im.unregisterInputDeviceListener(this);
        nm.cancel(1);
        super.onDestroy();
    }
    }
