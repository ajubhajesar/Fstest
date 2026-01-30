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
    
    // DM Send button coordinates
    private static final int SEND_X = 990;
    private static final int SEND_Y = 2313;
    
    // Screen center for swipe gestures (adjust if needed)
    private static final int CENTER_X = 540;
    private static final int CENTER_Y = 1170;
    
    // Swipe distances
    private static final int SWIPE_UP = -800;    // Swipe up distance
    private static final int SWIPE_DOWN = 800;   // Swipe down distance

    private InputManager im;
    private NotificationManager nm;
    private boolean kbd = false;
    private boolean ig = false;
    private boolean shiftHeld = false;

    @Override
    public void onServiceConnected() {
        Log.d(TAG, "Service started");

        // FIX: Explicitly configure the service to ensure Window State events are delivered
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) info = new AccessibilityServiceInfo();
        
        // Ensure we listen for window changes AND key events
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        info.notificationTimeout = 100;
        setServiceInfo(info);

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
            Log.d(TAG, "Notification cancelled");
            return;
        }
        
        String txt = ig ? "IG active - Keys enabled" : "Waiting for IG";
        
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(IG);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        PendingIntent pendingIntent = null;
        if (Build.VERSION.SDK_INT >= 23) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT);
        }
        
        Notification n;
        if (Build.VERSION.SDK_INT >= 26) {
            n = new Notification.Builder(this, "kbd")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Keyboard connected")
                .setContentText(txt)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
        } else {
            n = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Keyboard connected")
                .setContentText(txt)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .build();
        }
        nm.notify(1, n);
        Log.d(TAG, "Notification shown: " + txt);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        // Log all package changes to debug detection
        if (e.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence pkg = e.getPackageName();
            if (pkg != null) {
                boolean now = IG.equals(pkg.toString());
                if (now != ig) {
                    ig = now;
                    Log.d(TAG, "IG state change: " + ig + " (Current Pkg: " + pkg + ")");
                    if (kbd) updateNotification();
                }
            }
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        if (e.getDevice() != null && e.getDevice().isVirtual()) return false;
        
        // If Instagram isn't detected, don't consume keys
        if (!kbd || !ig) return false;
        
        int key = e.getKeyCode();
        int action = e.getAction();
        
        // Track Shift state
        if (key == KeyEvent.KEYCODE_SHIFT_LEFT || key == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            if (action == KeyEvent.ACTION_DOWN) {
                shiftHeld = true;
            } else if (action == KeyEvent.ACTION_UP) {
                shiftHeld = false;
            }
            return false; 
        }
        
        // ENTER key
        if (key == KeyEvent.KEYCODE_ENTER) {
            if (action == KeyEvent.ACTION_UP) {
                tapAt(SEND_X, SEND_Y, 50);
            }
            return true;
        }
        
        // UP arrow
        if (key == KeyEvent.KEYCODE_DPAD_UP) {
            if (action == KeyEvent.ACTION_DOWN) {
                swipe(CENTER_X, CENTER_Y, CENTER_X, CENTER_Y + SWIPE_DOWN, 300);
            }
            return true;
        }
        
        // DOWN arrow
        if (key == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (action == KeyEvent.ACTION_DOWN) {
                swipe(CENTER_X, CENTER_Y, CENTER_X, CENTER_Y + SWIPE_UP, 300);
            }
            return true;
        }
        
        // SHIFT held long press logic
        if (shiftHeld && action == KeyEvent.ACTION_DOWN) {
            longPress(CENTER_X, CENTER_Y, 2000); 
            return true;
        }
        
        return false;
    }

    private void tapAt(int x, int y, int duration) {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(x, y);
        GestureDescription.StrokeDescription s = 
            new GestureDescription.StrokeDescription(p, 0, duration);
        dispatchGesture(new GestureDescription.Builder().addStroke(s).build(), null, null);
    }

    private void swipe(int x1, int y1, int x2, int y2, int duration) {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        GestureDescription.StrokeDescription s = 
            new GestureDescription.StrokeDescription(p, 0, duration);
        dispatchGesture(new GestureDescription.Builder().addStroke(s).build(), null, null);
    }

    private void longPress(int x, int y, int duration) {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(x, y);
        GestureDescription.StrokeDescription s = 
            new GestureDescription.StrokeDescription(p, 0, duration);
        dispatchGesture(new GestureDescription.Builder().addStroke(s).build(), null, null);
    }

    @Override public void onInputDeviceAdded(int id) { checkKbd(); }
    @Override public void onInputDeviceRemoved(int id) { checkKbd(); }
    @Override public void onInputDeviceChanged(int id) { checkKbd(); }
    @Override public void onInterrupt() { Log.d(TAG, "Service interrupted"); }
    
    @Override
    public void onDestroy() {
        if (im != null) im.unregisterInputDeviceListener(this);
        nm.cancel(1);
        super.onDestroy();
    }
                    }
            
