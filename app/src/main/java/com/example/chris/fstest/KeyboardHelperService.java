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

    private static final String INSTAGRAM_PACKAGE = "com.instagram.android";
    private static final String CHANNEL_ID = "kbd_helper";
    private static final int NOTIFICATION_ID = 1;
    
    // Coordinates for Send button (portrait mode)
    private static final int SEND_X = 990;
    private static final int SEND_Y = 2313;

    private boolean physicalKeyboardConnected = false;
    private boolean instagramActive = false;
    private InputManager inputManager;
    private NotificationManager notificationManager;

    @Override
    public void onServiceConnected() {
        inputManager = (InputManager) getSystemService(INPUT_SERVICE);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        
        createNotificationChannel();
        inputManager.registerInputDeviceListener(this, null);
        checkPhysicalKeyboard();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Keyboard Helper",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Physical keyboard status");
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void checkPhysicalKeyboard() {
        boolean keyboardFound = false;
        int[] deviceIds = InputDevice.getDeviceIds();
        
        for (int i = 0; i < deviceIds.length; i++) {
            InputDevice device = InputDevice.getDevice(deviceIds[i]);
            if (device != null && !device.isVirtual()) {
                int sources = device.getSources();
                if ((sources & InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD) {
                    keyboardFound = true;
                    break;
                }
            }
        }
        
        if (keyboardFound != physicalKeyboardConnected) {
            physicalKeyboardConnected = keyboardFound;
            updateNotification();
        }
    }

    private void updateNotification() {
        if (!physicalKeyboardConnected) {
            notificationManager.cancel(NOTIFICATION_ID);
            return;
        }

        String title = "Physical keyboard connected";
        String text = instagramActive ? "Instagram active - Enter sends" : "Waiting for Instagram";

        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_notify_more)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setOngoing(true)
                    .build();
        } else {
            notification = new Notification.Builder(this)
                    .setSmallIcon(android.R.drawable.stat_notify_more)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_LOW)
                    .build();
        }

        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence packageName = event.getPackageName();
            boolean newState = packageName != null && INSTAGRAM_PACKAGE.contentEquals(packageName);
            
            if (newState != instagramActive) {
                instagramActive = newState;
                if (physicalKeyboardConnected) {
                    updateNotification();
                }
            }
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        // Only intercept ENTER key on physical keyboard when Instagram is active
        if (physicalKeyboardConnected && instagramActive) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    performSendTap();
                }
                // Consume both DOWN and UP to prevent newline insertion
                return true;
            }
        }
        return false;
    }

    private void performSendTap() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }

        Path path = new Path();
        path.moveTo(SEND_X, SEND_Y);
        path.lineTo(SEND_X, SEND_Y);

        GestureDescription.StrokeDescription stroke = 
                new GestureDescription.StrokeDescription(path, 0, 50);
        
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke)
                .build();

        dispatchGesture(gesture, null, null);
    }

    @Override
    public void onInterrupt() {
        // Required override - nothing to do
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        checkPhysicalKeyboard();
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        checkPhysicalKeyboard();
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        checkPhysicalKeyboard();
    }

    @Override
    public void onDestroy() {
        if (inputManager != null) {
            inputManager.unregisterInputDeviceListener(this);
        }
        notificationManager.cancel(NOTIFICATION_ID);
        super.onDestroy();
    }
}
