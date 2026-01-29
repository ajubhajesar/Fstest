package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Path;
import android.os.Build;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class KeyboardHelperService extends AccessibilityService {

    private static final String IG = "com.instagram.android";
    private static final String CHANNEL = "kbd_helper";
    private static final int NOTIF_ID = 1;

    // PORTRAIT SEND BUTTON
    private static final int SEND_X = 990;
    private static final int SEND_Y = 2313;

    private boolean instagramActive = false;
    private boolean usedOnce = false;

    private NotificationManager nm;

    @Override
    public void onServiceConnected() {
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL,
                    "Keyboard Helper",
                    NotificationManager.IMPORTANCE_LOW
            );
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            boolean nowIG = e.getPackageName() != null &&
                    IG.contentEquals(e.getPackageName());

            if (nowIG != instagramActive) {
                instagramActive = nowIG;
                if (!instagramActive) {
                    nm.cancel(NOTIF_ID);
                }
            }
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        if (!instagramActive) return false;
        if (e.getKeyCode() != KeyEvent.KEYCODE_ENTER) return false;

        if (e.getAction() == KeyEvent.ACTION_UP) {
            tapSend();

            if (!usedOnce) {
                usedOnce = true;
                showNotification();
            }
        }
        return false; // allow newline, we don't block
    }

    private void showNotification() {
        Notification n = Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle("Keyboard Helper")
                .setContentText("Instagram detected – ENTER sends")
                .setOngoing(true)
                .build()
            : new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle("Keyboard Helper")
                .setContentText("Instagram detected – ENTER sends")
                .setOngoing(true)
                .build();

        nm.notify(NOTIF_ID, n);
    }

    private void tapSend() {
        if (Build.VERSION.SDK_INT < 24) return;

        Path p = new Path();
        p.moveTo(SEND_X, SEND_Y);
        p.lineTo(SEND_X, SEND_Y);

        GestureDescription g = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(p, 0, 35))
                .build();

        dispatchGesture(g, null, null);
    }

    @Override public void onInterrupt() {}
}
