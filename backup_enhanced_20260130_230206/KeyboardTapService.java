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

public class KeyboardTapService extends AccessibilityService
        implements InputManager.InputDeviceListener {

    private static final String TAG = "IGKbd";
    private static final String IG = "com.instagram.android";
    private static final int X = 990;
    private static final int Y = 2313;

    private InputManager im;
    private NotificationManager nm;
    private boolean kbd = false;
    private boolean ig = false;

    @Override
    public void onServiceConnected() {
        Log.d(TAG, "Service started");
        im = (InputManager) getSystemService(INPUT_SERVICE);
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("kbd", "Keyboard", 
                NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
        
        im.registerInputDeviceListener(this, null);
        checkKbd();
    }

    private void checkKbd() {
        boolean found = false;
        int[] ids = InputDevice.getDeviceIds();
        for (int i = 0; i < ids.length; i++) {
            InputDevice d = InputDevice.getDevice(ids[i]);
            if (d != null && !d.isVirtual() && 
                (d.getSources() & InputDevice.SOURCE_KEYBOARD) != 0) {
                Log.d(TAG, "Keyboard: " + d.getName());
                found = true;
                break;
            }
        }
        if (found != kbd) {
            kbd = found;
            Log.d(TAG, "Kbd state: " + kbd);
            updateNotification();
        }
    }

    private void updateNotification() {
        if (!kbd) {
            nm.cancel(1);
            return;
        }
        String txt = ig ? "IG active - ENTER sends" : "Waiting for IG";
        Notification n;
        if (Build.VERSION.SDK_INT >= 26) {
            n = new Notification.Builder(this, "kbd")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Keyboard connected")
                .setContentText(txt)
                .setOngoing(true)
                .build();
        } else {
            n = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Keyboard connected")
                .setContentText(txt)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .build();
        }
        nm.notify(1, n);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence pkg = e.getPackageName();
            boolean now = pkg != null && IG.equals(pkg.toString());
            if (now != ig) {
                ig = now;
                Log.d(TAG, "IG: " + ig + " pkg: " + pkg);
                if (kbd) updateNotification();
            }
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        if (e.getDevice() != null && e.getDevice().isVirtual()) return false;
        if (!kbd || !ig) return false;
        if (e.getKeyCode() != KeyEvent.KEYCODE_ENTER) return false;
        
        if (e.getAction() == KeyEvent.ACTION_UP) {
            Log.d(TAG, "ENTER -> tap");
            tap();
        }
        return true;
    }

    private void tap() {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(X, Y);
        GestureDescription.StrokeDescription s = 
            new GestureDescription.StrokeDescription(p, 0, 50);
        dispatchGesture(new GestureDescription.Builder().addStroke(s).build(), null, null);
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
