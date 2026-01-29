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
import android.view.accessibility.AccessibilityNodeInfo;

public class KeyboardHelperService extends AccessibilityService {

    private static final String PKG_INSTAGRAM = "com.instagram.android";
    private static final String PKG_CHATGPT   = "com.openai.chatgpt";

    private static final int SEND_X = 990;
    private static final int SEND_Y = 2313;

    private static final int TAP_DELAY_MS = 80;
    private static final int NOTIF_ID = 1;
    private static final String CHANNEL_ID = "enter_send";

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected boolean onKeyEvent(KeyEvent event) {

        // Ignore software keyboards
        if (event.getDevice() != null && event.getDevice().isVirtual()) {
            return false;
        }

        // ENTER only
        if (event.getKeyCode() != KeyEvent.KEYCODE_ENTER) {
            return false;
        }

        // KEY_UP only (critical)
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return false;
        }

        // Detect foreground app at action time (KeyMapper-style)
        if (!isAllowedForegroundApp()) {
            removeNotification();
            return false;
        }

        showNotification();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendTap();
            }
        }, TAP_DELAY_MS);

        return true; // consume ENTER
    }

    private boolean isAllowedForegroundApp() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;

        CharSequence pkg = root.getPackageName();
        if (pkg == null) return false;

        return PKG_INSTAGRAM.contentEquals(pkg) ||
               PKG_CHATGPT.contentEquals(pkg);
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

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not needed anymore
    }

    @Override
    public void onInterrupt() {
        removeNotification();
    }

    private void showNotification() {
        Notification n = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("ENTER → Send active")
                .setSmallIcon(android.R.drawable.ic_input_get)
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

    @Override
    protected void onServiceConnected() {
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
