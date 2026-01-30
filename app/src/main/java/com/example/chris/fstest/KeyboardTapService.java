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

    private InputManager im;
    private NotificationManager nm;
    private boolean kbd = false;
    private boolean ig = false;

    @Override
    public void onServiceConnected() {
        Log.d(TAG, "Service Connected");

        // Force the configuration to include all necessary event types
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) info = new AccessibilityServiceInfo();
        
        // Listen to everything that might give us a package name
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | 
                         AccessibilityEvent.TYPE_WINDOWS_CHANGED | 
                         AccessibilityEvent.TYPE_VIEW_FOCUSED;
                         
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 50;
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        
        setServiceInfo(info);

        im = (InputManager) getSystemService(INPUT_SERVICE);
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("kbd", "Keyboard", 
                NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(ch);
        }
        
        im.registerInputDeviceListener(this, null);
        checkKbd();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        // Robust detection: Look for package name in ANY valid event
        CharSequence pkg = e.getPackageName();
        if (pkg != null) {
            String currentPkg = pkg.toString();
            boolean now = IG.equals(currentPkg);
            
            if (now != ig) {
                ig = now;
                Log.d(TAG, "Detection -> IG Active: " + ig + " (Pkg: " + currentPkg + ")");
                if (kbd) updateNotification();
            }
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        // Log to see if keys are actually being caught
        if (e.getAction() == KeyEvent.ACTION_UP && e.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            Log.d(TAG, "Enter pressed. Kbd: " + kbd + " IG: " + ig);
            if (kbd && ig) {
                tapAt(SEND_X, SEND_Y);
                return true; // Consume the key
            }
        }
        return false;
    }

    private void updateNotification() {
        if (!kbd) {
            nm.cancel(1);
            return;
        }
        String txt = ig ? "INSTAGRAM ACTIVE" : "Waiting for Instagram...";
        
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            builder = new Notification.Builder(this, "kbd");
        } else {
            builder = new Notification.Builder(this);
        }

        Notification n = builder
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Keyboard Service")
            .setContentText(txt)
            .setOngoing(true)
            .build();
            
        nm.notify(1, n);
    }

    private void tapAt(int x, int y) {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(x, y);
        GestureDescription.StrokeDescription s = new GestureDescription.StrokeDescription(p, 0, 50);
        dispatchGesture(new GestureDescription.Builder().addStroke(s).build(), null, null);
    }

    private void checkKbd() {
        boolean found = false;
        int[] ids = InputDevice.getDeviceIds();
        for (int id : ids) {
            InputDevice d = InputDevice.getDevice(id);
            if (d != null && !d.isVirtual() && (d.getSources() & InputDevice.SOURCE_KEYBOARD) != 0) {
                found = true;
                break;
            }
        }
        if (found != kbd) {
            kbd = found;
            updateNotification();
        }
    }

    @Override public void onInputDeviceAdded(int id) { checkKbd(); }
    @Override public void onInputDeviceRemoved(int id) { checkKbd(); }
    @Override public void onInputDeviceChanged(int id) { checkKbd(); }
    @Override public void onInterrupt() {}
        }
