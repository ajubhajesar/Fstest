package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.res.Configuration;
import android.graphics.Path;
import android.os.Build;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class SmartEnterAccessibilityService extends AccessibilityService {

    private static final String CHANNEL_ID = "keyboard_status";
    private static final int NOTIF_ID = 1001;
    private boolean keyboardActive = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        updateKeyboardState();
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (!keyboardActive) return false;
        if (event.getAction() != KeyEvent.ACTION_UP) return false;
        if (event.getKeyCode() != KeyEvent.KEYCODE_ENTER) return false;
        if (event.isShiftPressed()) return false;

        Path path = new Path();
        path.moveTo(990, 2313);

        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(path, 0, 50))
            .build();

        dispatchGesture(gesture, null, null);
        return true;
    }

    private void updateKeyboardState() {
        boolean nowActive = getResources().getConfiguration().keyboard == Configuration.KEYBOARD_QWERTY;

        if (nowActive == keyboardActive) return;
        keyboardActive = nowActive;

        NotificationManager nm = getSystemService(NotificationManager.class);

        if (nowActive) {
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Keyboard Status",
                    NotificationManager.IMPORTANCE_LOW
                );
                nm.createNotificationChannel(ch);
            }

            Notification n = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Keyboard active")
                .setContentText("Enter = Send")
                .setSmallIcon(android.R.drawable.ic_input_add)
                .setOngoing(true)
                .build();

            nm.notify(NOTIF_ID, n);
        } else {
            nm.cancel(NOTIF_ID);
        }
    }

    @Override
    public void onInterrupt() {}
}
