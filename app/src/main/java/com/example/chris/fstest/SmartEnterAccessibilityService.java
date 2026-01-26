package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.res.Configuration;
import android.graphics.Path;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class SmartEnterAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        // Only act when a hardware keyboard is attached
        if (getResources().getConfiguration().keyboard != Configuration.KEYBOARD_QWERTY) return false;

        // Only intercept Enter key
        if (event.getKeyCode() != KeyEvent.KEYCODE_ENTER) return false;

        // Allow Shift+Enter to pass through (newline)
        if (event.isShiftPressed()) return false;

        // Trigger ONLY once per physical press (Key Mapper style)
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            Path path = new Path();
            path.moveTo(990, 2313);

            GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 40))
                .build();

            dispatchGesture(gesture, null, null);
        }

        // ALWAYS consume Enter so the app never receives it
        return true;
    }

    @Override
    public void onInterrupt() {}
}
