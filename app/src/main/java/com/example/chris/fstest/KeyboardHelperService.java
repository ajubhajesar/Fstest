package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class KeyboardHelperService extends AccessibilityService {

    // Adjust if needed (your confirmed working coords)
    private static final int SEND_X = 990;
    private static final int SEND_Y = 2313;

    @Override
    protected boolean onKeyEvent(KeyEvent event) {

        // Ignore virtual / software keyboards
        if (event.getDevice() != null && event.getDevice().isVirtual()) {
            return false;
        }

        // Only ENTER
        if (event.getKeyCode() != KeyEvent.KEYCODE_ENTER) {
            return false;
        }

        // Only on key release
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return false;
        }

        // MUST be Shift + Enter
        if (!event.isShiftPressed()) {
            return false; // let normal Enter pass through
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
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // intentionally empty
    }

    @Override
    public void onInterrupt() {
        // nothing
    }
}
