package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class SmartEnterAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public boolean onKeyEvent(KeyEvent event) {
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

    @Override
    public void onInterrupt() {}
}
