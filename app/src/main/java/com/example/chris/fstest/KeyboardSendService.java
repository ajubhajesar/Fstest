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

        if (getRootInActiveWindow() == null) return false;

        CharSequence pkgCs = getRootInActiveWindow().getPackageName();
        if (pkgCs == null) return false;

        String pkg = pkgCs.toString();
        if (!pkg.contains("instagram")) return false;

        tap(SEND_X, SEND_Y);
        return true;
    }

    private void tap(int x, int y) {
        if (Build.VERSION.SDK_INT < 24) return;

        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, 50);

        GestureDescription gesture =
                new GestureDescription.Builder().addStroke(stroke).build();

        dispatchGesture(gesture, null, null);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}
}
