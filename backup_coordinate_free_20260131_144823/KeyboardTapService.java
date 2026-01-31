package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
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
    
    // DM Send button coordinates (adjust for your device)
    private static final int SEND_X = 990;
    private static final int SEND_Y = 2313;
    
    // Screen dimensions for Pixel 6 (1080x2340) - ADJUST FOR YOUR DEVICE
    private static final int SCREEN_WIDTH = 1080;
    private static final int SCREEN_HEIGHT = 2340;
    
    // Gesture coordinates
    private static final int CENTER_X = SCREEN_WIDTH / 2;
    private static final int CENTER_Y = SCREEN_HEIGHT / 2;
    private static final int RIGHT_SIDE_X = (SCREEN_WIDTH * 3) / 4; // 75% from left
    
    // Enhanced swipe parameters
    private static final int SWIPE_DISTANCE = 900;  // Large swipe distance
    private static final int SWIPE_DURATION = 150;  // Fast swipes (was 200)
    
    // Shift hold parameters
    private static final int SHIFT_HOLD_DURATION = 3000; // 3 seconds hold
    private static final int SHIFT_HOLD_INTERVAL = 800;  // Repeat every 800ms

    private InputManager im;
    private NotificationManager nm;
    private boolean kbd = false;
    private boolean ig = false;
    private boolean shiftHeld = false;
    private Handler handler = new Handler();
    private Runnable shiftHoldTask;
    private boolean isTypingWindow = false;
    private long lastEnterTime = 0;

    @Override
    public void onServiceConnected() {
        Log.d(TAG, "=== SERVICE CONNECTED ===");
        
        im = (InputManager) getSystemService(INPUT_SERVICE);
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("kbd", "Keyboard", 
                NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
        
        im.registerInputDeviceListener(this, null);
        checkKbd();
        
        Log.d(TAG, "Service ready - Instagram keyboard helper");
    }

    private void checkKbd() {
        boolean found = false;
        int[] ids = InputDevice.getDeviceIds();
        
        for (int i = 0; i < ids.length; i++) {
            InputDevice d = InputDevice.getDevice(ids[i]);
            if (d != null && !d.isVirtual() && 
                (d.getSources() & InputDevice.SOURCE_KEYBOARD) != 0) {
                Log.d(TAG, "Physical keyboard detected: " + d.getName());
                found = true;
                break;
            }
        }
        
        if (found != kbd) {
            kbd = found;
            Log.d(TAG, "*** KEYBOARD STATE: " + kbd + " ***");
            updateNotification();
        }
    }

    private void updateNotification() {
        // CRITICAL FIX: Only show notification if keyboard is actually connected
        if (!kbd) {
            nm.cancel(1);
            Log.d(TAG, "Notification removed - no keyboard");
            return;
        }
        
        String txt = ig ? "Instagram active - Keys enabled" : "Waiting for Instagram";
        
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(IG);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        PendingIntent pi;
        if (Build.VERSION.SDK_INT >= 23) {
            pi = PendingIntent.getActivity(this, 0, intent, 
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            pi = PendingIntent.getActivity(this, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT);
        }
        
        Notification n;
        if (Build.VERSION.SDK_INT >= 26) {
            n = new Notification.Builder(this, "kbd")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Keyboard Helper")
                .setContentText(txt)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
        } else {
            n = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Keyboard Helper")
                .setContentText(txt)
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .build();
        }
        
        nm.notify(1, n);
        Log.d(TAG, "Notification shown: " + txt);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        CharSequence pkg = e.getPackageName();
        
        if (pkg != null) {
            boolean nowIG = IG.equals(pkg.toString());
            
            if (nowIG != ig) {
                ig = nowIG;
                Log.d(TAG, "*** INSTAGRAM STATE: " + ig + " ***");
                
                // Detect typing window by checking event details
                if (ig && e.getClassName() != null) {
                    String className = e.getClassName().toString();
                    isTypingWindow = className.contains("EditText") || 
                                     className.contains("MessageInputView") ||
                                     e.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED;
                    
                    Log.d(TAG, "Typing window detected: " + isTypingWindow + " class: " + className);
                }
                
                // Update notification only if keyboard is connected
                if (kbd) {
                    updateNotification();
                } else {
                    Log.d(TAG, "IG changed but no keyboard - ignoring notification");
                }
            }
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        // Only intercept when both conditions are met
        if (!kbd || !ig) {
            return false;
        }
        
        int key = e.getKeyCode();
        int action = e.getAction();
        
        Log.d(TAG, "Key: " + key + " action: " + action + " typing: " + isTypingWindow);
        
        // Handle Shift key for continuous hold
        if (key == KeyEvent.KEYCODE_SHIFT_LEFT || key == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            if (action == KeyEvent.ACTION_DOWN && !shiftHeld) {
                shiftHeld = true;
                Log.d(TAG, "Shift DOWN - starting continuous hold");
                startContinuousHold();
            } else if (action == KeyEvent.ACTION_UP) {
                shiftHeld = false;
                Log.d(TAG, "Shift UP - stopping hold");
                stopContinuousHold();
            }
            return false; // Don't consume shift key
        }
        
        // FIX 1: ENTER key handling - ALWAYS consume and tap
        if (key == KeyEvent.KEYCODE_ENTER) {
            long currentTime = System.currentTimeMillis();
            
            // Prevent double-tap issues
            if (currentTime - lastEnterTime < 500) {
                Log.d(TAG, "ENTER ignored (too quick)");
                return true;
            }
            lastEnterTime = currentTime;
            
            if (action == KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "ENTER DOWN - consuming (typing: " + isTypingWindow + ")");
            } else if (action == KeyEvent.ACTION_UP) {
                Log.d(TAG, "ENTER UP - tapping send button");
                tapAt(SEND_X, SEND_Y, 50);
            }
            
            // CRITICAL: Return true for both DOWN and UP to prevent newline
            return true;
        }
        
        // UP arrow - Swipe DOWN for previous reel
        if (key == KeyEvent.KEYCODE_DPAD_UP) {
            if (action == KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "UP arrow - fast swipe DOWN");
                fastSwipeDown();
            }
            return true;
        }
        
        // DOWN arrow - Swipe UP for next reel
        if (key == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (action == KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "DOWN arrow - fast swipe UP");
                fastSwipeUp();
            }
            return true;
        }
        
        return false;
    }

    // Continuous hold on RIGHT side for fast forward
    private void startContinuousHold() {
        if (shiftHoldTask != null) {
            handler.removeCallbacks(shiftHoldTask);
        }
        
        shiftHoldTask = new Runnable() {
            public void run() {
                if (shiftHeld) {
                    Log.d(TAG, "Continuous hold on RIGHT side");
                    // Hold on RIGHT side to fast forward
                    longPress(RIGHT_SIDE_X, CENTER_Y, SHIFT_HOLD_DURATION);
                    
                    // Schedule next hold
                    handler.postDelayed(this, SHIFT_HOLD_INTERVAL);
                }
            }
        };
        
        // Start immediately
        handler.post(shiftHoldTask);
    }

    private void stopContinuousHold() {
        if (shiftHoldTask != null) {
            handler.removeCallbacks(shiftHoldTask);
            shiftHoldTask = null;
        }
    }

    // Fast swipe DOWN (for previous reel)
    private void fastSwipeDown() {
        if (Build.VERSION.SDK_INT < 24) return;
        
        // Swipe from center to bottom
        int startY = CENTER_Y - SWIPE_DISTANCE / 2;
        int endY = CENTER_Y + SWIPE_DISTANCE / 2;
        
        Path p = new Path();
        p.moveTo(CENTER_X, startY);
        p.lineTo(CENTER_X, endY);
        
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, SWIPE_DURATION))
            .build();
            
        boolean sent = dispatchGesture(gesture, null, null);
        Log.d(TAG, "Fast swipe DOWN result: " + sent);
    }

    // Fast swipe UP (for next reel)
    private void fastSwipeUp() {
        if (Build.VERSION.SDK_INT < 24) return;
        
        // Swipe from center to top
        int startY = CENTER_Y + SWIPE_DISTANCE / 2;
        int endY = CENTER_Y - SWIPE_DISTANCE / 2;
        
        Path p = new Path();
        p.moveTo(CENTER_X, startY);
        p.lineTo(CENTER_X, endY);
        
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, SWIPE_DURATION))
            .build();
            
        boolean sent = dispatchGesture(gesture, null, null);
        Log.d(TAG, "Fast swipe UP result: " + sent);
    }

    private void tapAt(int x, int y, int dur) {
        if (Build.VERSION.SDK_INT < 24) {
            Log.e(TAG, "Gesture API requires API 24+");
            return;
        }
        
        Path p = new Path();
        p.moveTo(x, y);
        
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, dur))
            .build();
            
        boolean sent = dispatchGesture(gesture, null, null);
        Log.d(TAG, "Tap at (" + x + "," + y + ") result: " + sent);
    }

    private void longPress(int x, int y, int dur) {
        if (Build.VERSION.SDK_INT < 24) return;
        
        Path p = new Path();
        p.moveTo(x, y);
        
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, dur))
            .build();
            
        boolean sent = dispatchGesture(gesture, null, null);
        Log.d(TAG, "Long press at (" + x + "," + y + ") for " + dur + "ms: " + sent);
    }

    @Override 
    public void onInputDeviceAdded(int id) { 
        Log.d(TAG, "Device added: " + id);
        checkKbd(); 
    }
    
    @Override 
    public void onInputDeviceRemoved(int id) { 
        Log.d(TAG, "Device removed: " + id);
        checkKbd(); 
    }
    
    @Override 
    public void onInputDeviceChanged(int id) { 
        checkKbd(); 
    }
    
    @Override 
    public void onInterrupt() {
        Log.d(TAG, "Service interrupted");
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "=== SERVICE DESTROYED ===");
        stopContinuousHold();
        if (im != null) im.unregisterInputDeviceListener(this);
        nm.cancel(1);
        super.onDestroy();
    }
}
