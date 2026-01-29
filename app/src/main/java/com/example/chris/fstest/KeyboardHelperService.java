package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.List;

public class KeyboardHelperService extends AccessibilityService {

    private static final String PKG_INSTAGRAM = "com.instagram.android";

    private static final int SEND_X = 990;
    private static final int SEND_Y = 2313;

    private static final int TAP_DELAY_MS = 50;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected boolean onKeyEvent(KeyEvent event) {

        // Ignore software keyboard events
        if (event.getDevice() != null && event.getDevice().isVirtual()) {
            return false;
        }

        // ENTER only
        if (event.getKeyCode() != KeyEvent.KEYCODE_ENTER) {
            return false;
        }

        // KEY_UP only
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return false;
        }

        // Must be Instagram
        AccessibilityWindowInfo active = getActiveWindow();
        if (active == null) return false;

        CharSequence pkg = active.getRoot() != null ? active.getRoot().getPackageName() : null;
        if (pkg == null || !PKG_INSTAGRAM.contentEquals(pkg)) {
            return false;
        }

        // IME must be active (text input context)
        if (!isImeWindowVisible()) {
            return false;
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendTap();
            }
        }, TAP_DELAY_MS);

        return true; // consume ENTER
    }

    private boolean isImeWindowVisible() {
        List<AccessibilityWindowInfo> windows = getWindows();
        if (windows == null) return false;

        for (AccessibilityWindowInfo w : windows) {
            if (w != null && w.getType() == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                return true;
            }
        }
        return false;
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
        // intentionally unused
    }

    @Override
    public void onInterrupt() {
        // nothing
    }
}
