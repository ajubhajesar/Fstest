package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class KeyboardHelperService extends AccessibilityService {

    // Allowed apps
    private static final String PKG_INSTAGRAM = "com.instagram.android";
    private static final String PKG_CHATGPT   = "com.openai.chatgpt";

    // Send button coordinates (portrait)
    private static final int SEND_X = 990;
    private static final int SEND_Y = 2313;

    private static final int TAP_DELAY_MS = 80;
    private static final int NOTIF_ID = 1;
    private static final String CHANNEL_ID = "enter_send";

    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean physicalKeyboardActive = false;
    private boolean allowedAppActive = false;

    @Override
    protected void onServiceConnected() {
        createNotificationChannel();
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {

        // Detect physical keyboard (Java 7 safe)
        if (event.getDevice() != null && event.getDevice().isVirtual()) {
            return false;
        }

        // Mark keyboard as active on first real key
        physicalKeyboardActive = true;

        // Only act if allowed app is active
        if (!allowedAppActive) {
            return false;
        }

        // ENTER key only
        if (event.getKeyCode() != KeyEvent.KEYCODE_ENTER) {
            return false;
        }

        // KEY_UP only (critical)
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return false;
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendTap();
            }
        }, TAP_DELAY_MS);

        return true; // consume ENTER
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if (!physicalKeyboardActive) return;

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence pkg = event.getPackageName();
            boolean nowAllowed =
                    pkg != null &&
                    (PKG_INSTAGRAM.contentEquals(pkg) || PKG_CHATGPT.contentEquals(pkg));

            if (nowAllowed != allowedAppActive) {
                allowedAppActive = nowAllowed;
                updateNotification();
            }
        }
    }

    @Override
    public void onInterrupt() {
        removeNotification();
    }

    private void sendTap() {
        Path path = new Path();
        path.moveTo(SEND_X, SEND_Y);

        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, 50);

        GestureDescription gesture =
                new GestureDescription.Builder()
                        .addStroke(stroke)
                        .build();

        dispatchGesture(gesture, null, null);
    }

    private void updateNotification() {
        if (allowedAppActive) {
            showNotification();
        } else {
            removeNotification();
        }
    }

    private void showNotification() {
        Notification n = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("ENTER → Send active")
                .setSmallIcon(android.R.drawable.ic_input_get)
                .setOngoing(false)
                .setPriority(Notification.PRIORITY_MIN)
                .build();

        getNotificationManager().notify(NOTIF_ID, n);
    }

    private void removeNotification() {
        getNotificationManager().cancel(NOTIF_ID);
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "ENTER Send",
                    NotificationManager.IMPORTANCE_MIN
            );
            ch.setSound(null, null);
            getNotificationManager().createNotificationChannel(ch);
        }
    }
}
