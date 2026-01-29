package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class KeyboardSendService extends AccessibilityService {

    private static final int SEND_X = 990;
    private static final int SEND_Y = 2313;

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) return false;
        if (event.getKeyCode() != KeyEvent.KEYCODE_ENTER) return false;

        CharSequence pkg = getRootInActiveWindow() != null
                ? getRootInActiveWindow().getPackageName() : null;

        if (pkg == null || !pkg.toString().contains("instagram")) return false;

        tap(SEND_X, SEND_Y);
        return true;
    }

    private void tap(int x, int y) {
        if (Build.VERSION.SDK_INT < 24) return;

        Path p = new Path();
        p.moveTo(x, y);

        GestureDescription.StrokeDescription s =
                new GestureDescription.StrokeDescription(p, 0, 40);

        GestureDescription g =
                new GestureDescription.Builder().addStroke(s).build();

        dispatchGesture(g, null, null);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {}
    @Override public void onInterrupt() {}
}
