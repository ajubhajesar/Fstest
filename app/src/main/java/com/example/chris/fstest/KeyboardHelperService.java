package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class KeyboardHelperService extends AccessibilityService {

    // SEND button coordinates (portrait)
    private static final int SEND_X = 990;
    private static final int SEND_Y = 2313;

    // Key Mapper–style delay
    private static final int TAP_DELAY_MS = 80;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected boolean onKeyEvent(KeyEvent event) {

        // Only physical keyboard
        if (event.isVirtual()) return false;

        // ENTER key
        if (event.getKeyCode() != KeyEvent.KEYCODE_ENTER) return false;

        // Only react on KEY_UP (CRITICAL)
        if (event.getAction() != KeyEvent.ACTION_UP) return false;

        // Delay tap like Key Mapper
        handler.postDelayed(this::sendTap, TAP_DELAY_MS);

        // Consume ENTER (no newline)
        return true;
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
        // Not used
    }

    @Override
    public void onInterrupt() {
        // Not used
    }
}
