package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class KeyboardHelperService extends AccessibilityService {

    private static final int SEND_X = 990;
    private static final int SEND_Y = 2313;

    private boolean shiftHeld = false;

    @Override
    protected boolean onKeyEvent(KeyEvent event) {

        if (event.getDevice() != null && event.getDevice().isVirtual()) {
            return false;
        }

        int key = event.getKeyCode();

        // Track Shift manually
        if (key == KeyEvent.KEYCODE_SHIFT_LEFT || key == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                shiftHeld = true;
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                shiftHeld = false;
            }
            return false;
        }

        // Only ENTER
        if (key != KeyEvent.KEYCODE_ENTER) {
            return false;
        }

        // Only KEY_UP
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return false;
        }

        // MUST be Shift + Enter
        if (!shiftHeld) {
            return false; // plain Enter passes through
        }

        sendTap();
        return true; // consume Shift+Enter only
    }

    private void sendTap() {
        Path path = new Path();
        path.moveTo(SEND_X, SEND_Y);

        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, 40);

        GestureDescription gesture =
                new GestureDescription.Builder()
                        .addStroke(stroke)
                        .build();

        dispatchGesture(gesture, null, null);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}
}
