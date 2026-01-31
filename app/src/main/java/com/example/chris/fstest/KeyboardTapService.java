package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class KeyboardTapService extends AccessibilityService
        implements InputManager.InputDeviceListener {

    private static final String TAG = "IGKbd";
    private static final String IG = "com.instagram.android";
    
    // Coordinates
    private static final int SEND_X = 990;
    private static final int SEND_Y = 2313;
    private static final int CENTER_X = 540;
    private static final int CENTER_Y = 1170;
    private static final int RIGHT_X = 810; // For SHIFT hold
    
    // Optimized swipe (200ms is smoother than 100ms)
    private static final int SWIPE_DURATION = 200;
    private static final int SWIPE_DISTANCE = 1000;

    private InputManager im;
    private boolean kbd = false;
    private boolean ig = false;
    private boolean shiftHeld = false;
    private Handler handler = new Handler();
    private Runnable shiftTask;

    @Override
    public void onServiceConnected() {
        Log.d(TAG, "=== SERVICE CONNECTED ===");
        im = (InputManager) getSystemService(INPUT_SERVICE);
        im.registerInputDeviceListener(this, null);
        checkKbd();
    }

    private void checkKbd() {
        boolean found = false;
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice d = InputDevice.getDevice(id);
            if (d != null && !d.isVirtual() && 
                (d.getSources() & InputDevice.SOURCE_KEYBOARD) != 0) {
                Log.d(TAG, "Physical keyboard: " + d.getName());
                found = true;
                break;
            }
        }
        if (found != kbd) {
            kbd = found;
            Log.d(TAG, "*** KEYBOARD: " + kbd + " ***");
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getPackageName() != null) {
            boolean nowIG = IG.equals(e.getPackageName().toString());
            if (nowIG != ig) {
                ig = nowIG;
                Log.d(TAG, "*** INSTAGRAM: " + ig + " ***");
            }
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        // Only intercept when BOTH conditions met
        if (!kbd || !ig) return false;
        
        int key = e.getKeyCode();
        int action = e.getAction();
        
        // SHIFT - continuous hold on RIGHT side
        if (key == KeyEvent.KEYCODE_SHIFT_LEFT || key == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            if (action == KeyEvent.ACTION_DOWN && !shiftHeld) {
                shiftHeld = true;
                Log.d(TAG, "SHIFT DOWN - starting continuous hold");
                startContinuousHold();
            } else if (action == KeyEvent.ACTION_UP) {
                shiftHeld = false;
                Log.d(TAG, "SHIFT UP - stopping hold");
                stopContinuousHold();
            }
            return false; // Don't consume shift
        }
        
        // ENTER - send message
        if (key == KeyEvent.KEYCODE_ENTER) {
            if (action == KeyEvent.ACTION_UP) {
                Log.d(TAG, "ENTER UP -> tap Send");
                tap(SEND_X, SEND_Y, 50);
            }
            // Consume BOTH to prevent newline
            return true;
        }
        
        // UP - previous reel
        if (key == KeyEvent.KEYCODE_DPAD_UP && action == KeyEvent.ACTION_DOWN) {
            Log.d(TAG, "UP -> swipe DOWN (previous)");
            smoothSwipe(CENTER_Y - SWIPE_DISTANCE/2, CENTER_Y + SWIPE_DISTANCE/2);
            return true;
        }
        
        // DOWN - next reel
        if (key == KeyEvent.KEYCODE_DPAD_DOWN && action == KeyEvent.ACTION_DOWN) {
            Log.d(TAG, "DOWN -> swipe UP (next)");
            smoothSwipe(CENTER_Y + SWIPE_DISTANCE/2, CENTER_Y - SWIPE_DISTANCE/2);
            return true;
        }
        
        return false;
    }

    private void startContinuousHold() {
        if (shiftTask != null) handler.removeCallbacks(shiftTask);
        
        shiftTask = new Runnable() {
            public void run() {
                if (shiftHeld) {
                    Log.d(TAG, "Hold on RIGHT side");
                    longPress(RIGHT_X, CENTER_Y, 500);
                    handler.postDelayed(this, 500);
                }
            }
        };
        handler.post(shiftTask);
    }

    private void stopContinuousHold() {
        if (shiftTask != null) {
            handler.removeCallbacks(shiftTask);
            shiftTask = null;
        }
    }

    private void tap(int x, int y, int dur) {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(x, y);
        dispatchGesture(new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, dur))
            .build(), null, null);
    }

    private void smoothSwipe(int startY, int endY) {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(CENTER_X, startY);
        p.lineTo(CENTER_X, endY);
        dispatchGesture(new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, SWIPE_DURATION))
            .build(), null, null);
    }

    private void longPress(int x, int y, int dur) {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(x, y);
        dispatchGesture(new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, dur))
            .build(), null, null);
    }

    @Override public void onInputDeviceAdded(int id) { checkKbd(); }
    @Override public void onInputDeviceRemoved(int id) { checkKbd(); }
    @Override public void onInputDeviceChanged(int id) { checkKbd(); }
    @Override public void onInterrupt() {}
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "=== SERVICE DESTROYED ===");
        stopContinuousHold();
        if (im != null) im.unregisterInputDeviceListener(this);
        super.onDestroy();
    }
}
