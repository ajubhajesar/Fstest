package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class KeyboardTapService extends AccessibilityService {
    private static final String TAG = "IG";
    private static final String IG_PKG = "com.instagram.android";
    
    // Coordinates
    private static final int SEND_X = 990;
    private static final int SEND_Y = 2313;
    private static final int CENTER_X = 540;
    private static final int CENTER_Y = 1170;
    
    // ULTRA FAST settings (1ms = minimum possible)
    private static final int TAP_DURATION = 1;
    private static final int SWIPE_DURATION = 1;
    private static final int SWIPE_DIST = 1000;
    
    private boolean igActive = false;
    private Handler h = new Handler();
    private long lastEnter = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence pkg = e.getPackageName();
            igActive = pkg != null && IG_PKG.equals(pkg.toString());
            Log.d(TAG, "IG: " + igActive);
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        if (!igActive) return false;
        
        int key = e.getKeyCode();
        int action = e.getAction();
        
        // ENTER - Send (with debounce)
        if (key == KeyEvent.KEYCODE_ENTER) {
            if (action == KeyEvent.ACTION_DOWN) {
                long now = System.currentTimeMillis();
                if (now - lastEnter < 200) return true; // Debounce
                lastEnter = now;
                h.postDelayed(() -> tap(SEND_X, SEND_Y), 30);
                return true; // Block newline
            }
            return true; // Block UP too
        }
        
        // UP - Previous reel (swipe down fast)
        if (key == KeyEvent.KEYCODE_DPAD_UP && action == KeyEvent.ACTION_DOWN) {
            swipe(CENTER_X, CENTER_Y - 400, CENTER_X, CENTER_Y + 400);
            return true;
        }
        
        // DOWN - Next reel (swipe up fast)
        if (key == KeyEvent.KEYCODE_DPAD_DOWN && action == KeyEvent.ACTION_DOWN) {
            swipe(CENTER_X, CENTER_Y + 400, CENTER_X, CENTER_Y - 400);
            return true;
        }
        
        return false;
    }

    private void tap(int x, int y) {
        Path p = new Path();
        p.moveTo(x, y);
        dispatchGesture(new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, TAP_DURATION))
            .build(), null, null);
    }

    private void swipe(int x1, int y1, int x2, int y2) {
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        dispatchGesture(new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, SWIPE_DURATION))
            .build(), null, null);
    }

    @Override public void onInterrupt() {}
}
