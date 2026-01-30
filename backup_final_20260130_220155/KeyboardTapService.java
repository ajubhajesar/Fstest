package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class KeyboardTapService extends AccessibilityService {

    // FIXED COORDINATES (PORTRAIT)
    private static final int TAP_X = 990;
    private static final int TAP_Y = 2313;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
            event.getAction() == KeyEvent.ACTION_DOWN) {

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    tap(TAP_X, TAP_Y);
                }
            }, 70);

            return true; // consume ENTER
        }
        return false;
    }

    private void tap(int x, int y) {
        if (Build.VERSION.SDK_INT < 24) return;

        Path p = new Path();
        p.moveTo(x, y);

        GestureDescription.StrokeDescription s =
                new GestureDescription.StrokeDescription(p, 0, 50);

        GestureDescription g =
                new GestureDescription.Builder().addStroke(s).build();

        dispatchGesture(g, null, null);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() {}
}
