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
import android.os.Handler;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class KeyboardTapService extends AccessibilityService
        implements InputManager.InputDeviceListener {

    private static final String TAG = "IGKbd";
    private static final String IG = "com.instagram.android";

    // ====== ADJUST ONLY IF DEVICE CHANGES ======
    private static final int SCREEN_W = 1080;
    private static final int SCREEN_H = 2340;

    private static final int SEND_X = 990;
    private static final int SEND_Y = 2313;

    private static final int CENTER_X = SCREEN_W / 2;
    private static final int CENTER_Y = SCREEN_H / 2;
    private static final int RIGHT_X  = (SCREEN_W * 3) / 4;

    private static final int SWIPE_DURATION = 180;
    // ==========================================

    private InputManager im;
    private NotificationManager nm;
    private boolean kbd = false;
    private boolean ig = false;
    private boolean shiftHeld = false;

    private Handler handler = new Handler();
    private Runnable holdTask;

    @Override
    public void onServiceConnected() {
        im = (InputManager) getSystemService(INPUT_SERVICE);
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                "kbd", "Keyboard",
                NotificationManager.IMPORTANCE_LOW
            );
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }

        im.registerInputDeviceListener(this, null);
        checkKeyboard();
        Log.d(TAG, "Service connected");
    }

    private void checkKeyboard() {
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
            updateNotification();
        }
    }

    private void updateNotification() {
        if (!kbd) {
            nm.cancel(1);
            return;
        }

        String txt = ig ? "IG active - Keys enabled" : "Waiting for IG";

        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.setPackage(IG);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pi = PendingIntent.getActivity(
            this, 0, i,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification n = new Notification.Builder(this, "kbd")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Keyboard connected")
            .setContentText(txt)
            .setOngoing(true)
            .setContentIntent(pi)
            .build();

        nm.notify(1, n);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getPackageName() == null) return;

        boolean nowIG = IG.equals(e.getPackageName().toString());
        if (nowIG != ig) {
            ig = nowIG;
            if (kbd) updateNotification();
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        if (!kbd || !ig) return false;
        if (e.getDevice() != null && e.getDevice().isVirtual()) return false;

        int key = e.getKeyCode();
        int act = e.getAction();

        // ===== SHIFT HOLD (continuous fast-forward) =====
        if (key == KeyEvent.KEYCODE_SHIFT_LEFT || key == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            if (act == KeyEvent.ACTION_DOWN && !shiftHeld) {
                shiftHeld = true;
                startHold();
            } else if (act == KeyEvent.ACTION_UP) {
                shiftHeld = false;
                stopHold();
            }
            return false;
        }

        // ===== ENTER SEND (NO NEWLINE) =====
        if (key == KeyEvent.KEYCODE_ENTER) {
            if (act == KeyEvent.ACTION_UP) {
                tap(SEND_X, SEND_Y, 50);
            }
            return true; // CONSUME DOWN + UP
        }

        // ===== REELS SCROLL =====
        if (key == KeyEvent.KEYCODE_DPAD_DOWN && act == KeyEvent.ACTION_DOWN) {
            swipeUp();
            return true;
        }

        if (key == KeyEvent.KEYCODE_DPAD_UP && act == KeyEvent.ACTION_DOWN) {
            swipeDown();
            return true;
        }

        return false;
    }

    private void startHold() {
        holdTask = new Runnable() {
            public void run() {
                if (shiftHeld) {
                    longPress(RIGHT_X, CENTER_Y, 400);
                    handler.postDelayed(this, 400);
                }
            }
        };
        handler.post(holdTask);
    }

    private void stopHold() {
        if (holdTask != null) {
            handler.removeCallbacks(holdTask);
            holdTask = null;
        }
    }

    private void swipeUp() {
        gesture(CENTER_X, SCREEN_H * 2 / 3, CENTER_X, SCREEN_H / 3);
    }

    private void swipeDown() {
        gesture(CENTER_X, SCREEN_H / 3, CENTER_X, SCREEN_H * 2 / 3);
    }

    private void gesture(int x1, int y1, int x2, int y2) {
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        dispatchGesture(new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, SWIPE_DURATION))
            .build(), null, null);
    }

    private void tap(int x, int y, int d) {
        Path p = new Path();
        p.moveTo(x, y);
        dispatchGesture(new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, d))
            .build(), null, null);
    }

    private void longPress(int x, int y, int d) {
        Path p = new Path();
        p.moveTo(x, y);
        dispatchGesture(new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, d))
            .build(), null, null);
    }

    @Override public void onInputDeviceAdded(int id) { checkKeyboard(); }
    @Override public void onInputDeviceRemoved(int id) { checkKeyboard(); }
    @Override public void onInputDeviceChanged(int id) { checkKeyboard(); }
    @Override public void onInterrupt() {}
}
