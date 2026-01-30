package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Path;
import android.hardware.input.InputManager;
import android.net.Uri;
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

    private InputManager im;
    private NotificationManager nm;
    private boolean kbd = false;
    private boolean ig = false;
    private boolean shiftHeld = false;

    @Override
    public void onServiceConnected() {
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) info = new AccessibilityServiceInfo();
        info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        setServiceInfo(info);

        im = (InputManager) getSystemService(INPUT_SERVICE);
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("kbd", "Keyboard", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(ch);
        }
        im.registerInputDeviceListener(this, null);
        checkKbd();
    }

    private void checkKbd() {
        boolean found = false;
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice d = InputDevice.getDevice(id);
            if (d != null && !d.isVirtual() && (d.getSources() & InputDevice.SOURCE_KEYBOARD) != 0) {
                found = true; break;
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
        String txt = ig ? "IG Active - Ready" : "Waiting for IG...";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("instagram://direct_v2_inbox"));
        intent.setPackage(IG);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        
        Notification.Builder b = (Build.VERSION.SDK_INT >= 26) ? new Notification.Builder(this, "kbd") : new Notification.Builder(this);
        Notification n = b.setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Keyboard connected")
            .setContentText(txt)
            .setContentIntent(pi)
            .setOngoing(true)
            .build();
        nm.notify(1, n);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getPackageName() != null) {
            boolean now = IG.equals(e.getPackageName().toString());
            if (now != ig) {
                ig = now;
                updateNotification();
            }
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        if (!kbd || !ig) return false;
        int key = e.getKeyCode();
        int action = e.getAction();

        if (key == KeyEvent.KEYCODE_SHIFT_LEFT || key == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            shiftHeld = (action == KeyEvent.ACTION_DOWN);
            if (shiftHeld) longPress(CENTER_X, CENTER_Y, 10000); // Trigger long press
            return false;
        }

        if (key == KeyEvent.KEYCODE_ENTER && action == KeyEvent.ACTION_UP) {
            tapAt(SEND_X, SEND_Y, 50);
            return true;
        }

        if (action == KeyEvent.ACTION_DOWN) {
            if (key == KeyEvent.KEYCODE_DPAD_UP) {
                swipe(CENTER_X, CENTER_Y + 400, CENTER_X, CENTER_Y - 400, 150);
                return true;
            }
            if (key == KeyEvent.KEYCODE_DPAD_DOWN) {
                swipe(CENTER_X, CENTER_Y - 400, CENTER_X, CENTER_Y + 400, 150);
                return true;
            }
        }
        return false;
    }

    private void tapAt(int x, int y, int duration) {
        Path p = new Path(); p.moveTo(x, y);
        dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(p, 0, duration)).build(), null, null);
    }

    private void swipe(int x1, int y1, int x2, int y2, int duration) {
        Path p = new Path(); p.moveTo(x1, y1); p.lineTo(x2, y2);
        dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(p, 0, duration)).build(), null, null);
    }

    private void longPress(int x, int y, int duration) {
        Path p = new Path(); p.moveTo(x, y);
        dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(p, 0, duration)).build(), null, null);
    }

    @Override public void onInputDeviceAdded(int id) { checkKbd(); }
    @Override public void onInputDeviceRemoved(int id) { checkKbd(); }
    @Override public void onInputDeviceChanged(int id) { checkKbd(); }
    @Override public void onInterrupt() {}
    @Override public void onDestroy() { nm.cancel(1); super.onDestroy(); }
}
