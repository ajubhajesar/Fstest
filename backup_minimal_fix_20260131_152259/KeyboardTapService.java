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
        // CRITICAL: Only show notification if keyboard is connected
        if (!kbd) {
            nm.cancel(1);
            Log.d(TAG, "Notification cancelled - no keyboard");
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
        Log.d(TAG, "Notification shown: " + txt);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence pkg = e.getPackageName();
            boolean now = pkg != null && IG.equals(pkg.toString());
            if (now != ig) {
                ig = now;
                Log.d(TAG, "IG: " + ig + " pkg: " + pkg);
                
                // CRITICAL: Only update notification if keyboard is connected
                if (kbd) {
                    updateNotification();
                } else {
                    Log.d(TAG, "Instagram state changed but no keyboard - skipping notification");
                }
            }
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        // CRITICAL FIX: Don't check if device is virtual - just check if physical keyboard is connected
        // The virtual keyboard being shown doesn't mean this key event is from it
        
        if (!kbd || !ig) return false;
        
        int keyCode = e.getKeyCode();
        int action = e.getAction();
        
        Log.d(TAG, "Key: " + keyCode + " action: " + action);
        
        // Only handle ENTER key
        if (keyCode != KeyEvent.KEYCODE_ENTER) return false;
        
        // CRITICAL: Return true for BOTH ACTION_DOWN and ACTION_UP to prevent newline
        if (action == KeyEvent.ACTION_DOWN) {
            Log.d(TAG, "ENTER DOWN - consuming to prevent newline");
            return true;
        } else if (action == KeyEvent.ACTION_UP) {
            Log.d(TAG, "ENTER UP - tapping at (" + X + "," + Y + ")");
            tap();
            return true;
        }
        
        return false;
    }

    private void tap() {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(X, Y);
        GestureDescription.StrokeDescription s = 
            new GestureDescription.StrokeDescription(p, 0, 50);
        boolean dispatched = dispatchGesture(new GestureDescription.Builder().addStroke(s).build(), null, null);
        Log.d(TAG, "Tap dispatched: " + dispatched);
    }

    @Override public void onInputDeviceAdded(int id) { 
        Log.d(TAG, "Device added: " + id);
        checkKbd(); 
    }
    
    @Override public void onInputDeviceRemoved(int id) { 
        Log.d(TAG, "Device removed: " + id);
        checkKbd(); 
    }
    
    @Override public void onInputDeviceChanged(int id) { 
        checkKbd(); 
    }
    
    @Override public void onInterrupt() {}
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed");
        if (im != null) im.unregisterInputDeviceListener(this);
        nm.cancel(1);
        super.onDestroy();
    }
}
