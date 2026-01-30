package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
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
    private static final String IG_PACKAGE = "com.instagram.android";
    
    // Coordinates (Adjust based on your screen resolution)
    private static final int SEND_X = 990;
    private static final int SEND_Y = 2313;
    private static final int CENTER_X = 540;
    private static final int CENTER_Y = 1170;

    private InputManager im;
    private NotificationManager nm;
    private boolean kbdConnected = false;
    private boolean igActive = false;
    private boolean isShiftPressed = false;

    @Override
    public void onServiceConnected() {
        Log.d(TAG, "Service Connected");

        // FORCE CONFIGURATION: This ensures the service detects app changes
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) info = new AccessibilityServiceInfo();
        
        // Listen for all window-related changes to catch Instagram
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | 
                         AccessibilityEvent.TYPE_WINDOWS_CHANGED | 
                         AccessibilityEvent.TYPE_VIEW_FOCUSED;
                         
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 50;
        
        // Essential flags for key filtering and gesture performance
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        
        setServiceInfo(info);

        im = (InputManager) getSystemService(INPUT_SERVICE);
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("kbd_chan", "Kbd Service", 
                NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(ch);
        }
        
        im.registerInputDeviceListener(this, null);
        checkKeyboardStatus();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        // Detect Instagram across various event types
        if (e.getPackageName() != null) {
            String pkgName = e.getPackageName().toString();
            boolean detected = pkgName.equals(IG_PACKAGE);
            
            if (detected != igActive) {
                igActive = detected;
                Log.d(TAG, "Instagram Focus: " + igActive);
                updateNotification();
            }
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        if (!kbdConnected || !igActive) return false;

        int keyCode = e.getKeyCode();
        int action = e.getAction();

        // 1. Track Shift Key State
        if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            isShiftPressed = (action == KeyEvent.ACTION_DOWN);
            return false; // Let shift pass through normally
        }

        // 2. Handle ENTER -> Send Tap
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (action == KeyEvent.ACTION_UP) {
                Log.d(TAG, "Action: Tap Send");
                performTap(SEND_X, SEND_Y, 50);
            }
            return true; // Consume Enter
        }

        // 3. Handle DPAD UP -> Swipe Down (Scroll Up)
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (action == KeyEvent.ACTION_DOWN) {
                performSwipe(CENTER_X, CENTER_Y, CENTER_X, CENTER_Y + 800);
            }
            return true;
        }

        // 4. Handle DPAD DOWN -> Swipe Up (Scroll Down)
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (action == KeyEvent.ACTION_DOWN) {
                performSwipe(CENTER_X, CENTER_Y, CENTER_X, CENTER_Y - 800);
            }
            return true;
        }

        // 5. Handle Shift + Any Key -> Long Press
        if (isShiftPressed && action == KeyEvent.ACTION_DOWN) {
            Log.d(TAG, "Action: Long Press");
            performTap(CENTER_X, CENTER_Y, 1500); 
            return true;
        }

        return false;
    }

    private void performTap(int x, int y, int duration) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, duration);
        dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(), null, null);
    }

    private void performSwipe(int x1, int y1, int x2, int y2) {
        Path path = new Path();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, 300);
        dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(), null, null);
    }

    private void updateNotification() {
        if (!kbdConnected) {
            nm.cancel(1);
            return;
        }
        String status = igActive ? "READY: IG Detected" : "WAITING: Open Instagram";
        Notification.Builder nb = (Build.VERSION.SDK_INT >= 26) ? 
            new Notification.Builder(this, "kbd_chan") : new Notification.Builder(this);

        nb.setContentTitle("Keyboard Macro Service")
          .setContentText(status)
          .setSmallIcon(android.R.drawable.ic_dialog_info)
          .setOngoing(true);
        
        nm.notify(1, nb.build());
    }

    private void checkKeyboardStatus() {
        boolean found = false;
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice d = InputDevice.getDevice(id);
            if (d != null && !d.isVirtual() && (d.getSources() & InputDevice.SOURCE_KEYBOARD) != 0) {
                found = true;
                break;
            }
        }
        if (found != kbdConnected) {
            kbdConnected = found;
            updateNotification();
        }
    }

    @Override public void onInputDeviceAdded(int id) { checkKeyboardStatus(); }
    @Override public void onInputDeviceRemoved(int id) { checkKeyboardStatus(); }
    @Override public void onInputDeviceChanged(int id) { checkKeyboardStatus(); }
    @Override public void onInterrupt() {}
                }
