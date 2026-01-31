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
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class KeyboardTapService extends AccessibilityService
        implements InputManager.InputDeviceListener {

    private static final String TAG = "IGKbd";
    private static final String IG = "com.instagram.android";
    
    // Screen dimensions for Pixel 6 (1080x2340)
    private static final int SCREEN_WIDTH = 1080;
    private static final int SCREEN_HEIGHT = 2340;
    
    // Gesture coordinates (adjust for your device)
    private static final int CENTER_X = SCREEN_WIDTH / 2;
    private static final int CENTER_Y = SCREEN_HEIGHT / 2;
    
    // Send button fallback coordinates (bottom-right corner)
    private static final int SEND_X = (SCREEN_WIDTH * 9) / 10;  // 90% from left
    private static final int SEND_Y = (SCREEN_HEIGHT * 9) / 10; // 90% from top
    
    // Swipe parameters
    private static final int SWIPE_DISTANCE = 800;
    private static final int SWIPE_DURATION = 200;

    private InputManager im;
    private NotificationManager nm;
    private boolean kbd = false;
    private boolean ig = false;
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
        
        Log.d(TAG, "Service ready");
    }

    private void checkKbd() {
        boolean found = false;
        int[] ids = InputDevice.getDeviceIds();
        
        for (int id : ids) {
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
            
            // CRITICAL: Update notification only if keyboard is connected
            if (kbd) {
                updateNotification();
            } else {
                // Remove notification immediately when keyboard disconnected
                nm.cancel(1);
                Log.d(TAG, "Notification removed - keyboard disconnected");
            }
        }
    }

    private void updateNotification() {
        // CRITICAL: Only show notification if keyboard is connected
        if (!kbd) {
            nm.cancel(1);
            Log.d(TAG, "Notification not shown - no keyboard");
            return;
        }
        
        // If no keyboard, don't show notification even if Instagram is active
        if (!kbd) {
            nm.cancel(1);
            return;
        }
        
        String txt = ig ? "Instagram active - ENTER to send" : "Waiting for Instagram";
        
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
                Log.d(TAG, "*** INSTAGRAM: " + ig + " ***");
                
                // CRITICAL: Only update notification if keyboard is connected
                if (kbd) {
                    updateNotification();
                } else {
                    Log.d(TAG, "Instagram opened but no keyboard - no notification");
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
        
        Log.d(TAG, "Key: " + key + " action: " + action);
        
        // ENTER key - Send message in DM
        if (key == KeyEvent.KEYCODE_ENTER) {
            long currentTime = System.currentTimeMillis();
            
            // Prevent double-tap issues
            if (currentTime - lastEnterTime < 300) {
                Log.d(TAG, "ENTER ignored (too quick)");
                return true; // Still consume to prevent newline
            }
            lastEnterTime = currentTime;
            
            if (action == KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "ENTER DOWN - consuming");
                // Consume DOWN event to prevent newline
                return true;
            } else if (action == KeyEvent.ACTION_UP) {
                Log.d(TAG, "ENTER UP - tapping send button");
                tapAt(SEND_X, SEND_Y, 50);
                // Consume UP event
                return true;
            }
        }
        
        // UP arrow - Swipe DOWN for previous reel
        if (key == KeyEvent.KEYCODE_DPAD_UP) {
            if (action == KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "UP arrow - swipe DOWN");
                // Swipe from center to bottom (previous reel)
                swipe(CENTER_X, CENTER_Y - SWIPE_DISTANCE/2, 
                      CENTER_X, CENTER_Y + SWIPE_DISTANCE/2, 
                      SWIPE_DURATION);
            }
            return true;
        }
        
        // DOWN arrow - Swipe UP for next reel
        if (key == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (action == KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "DOWN arrow - swipe UP");
                // Swipe from center to top (next reel)
                swipe(CENTER_X, CENTER_Y + SWIPE_DISTANCE/2, 
                      CENTER_X, CENTER_Y - SWIPE_DISTANCE/2, 
                      SWIPE_DURATION);
            }
            return true;
        }
        
        // LEFT arrow - Long press for rewind (removed shift functionality)
        if (key == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (action == KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "LEFT arrow - long press (rewind)");
                longPress(CENTER_X / 3, CENTER_Y, 1000); // Left side for rewind
            }
            return true;
        }
        
        // RIGHT arrow - Long press for fast forward (removed shift functionality)
        if (key == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (action == KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "RIGHT arrow - long press (fast forward)");
                longPress((SCREEN_WIDTH * 2) / 3, CENTER_Y, 1000); // Right side for FF
            }
            return true;
        }
        
        return false;
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
        Log.d(TAG, "Tap at (" + x + "," + y + "): " + sent);
    }

    private void swipe(int x1, int y1, int x2, int y2, int dur) {
        if (Build.VERSION.SDK_INT < 24) return;
        
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, dur))
            .build();
            
        boolean sent = dispatchGesture(gesture, null, null);
        Log.d(TAG, "Swipe (" + x1 + "," + y1 + ") to (" + x2 + "," + y2 + "): " + sent);
    }

    private void longPress(int x, int y, int dur) {
        if (Build.VERSION.SDK_INT < 24) return;
        
        Path p = new Path();
        p.moveTo(x, y);
        
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, dur))
            .build();
            
        boolean sent = dispatchGesture(gesture, null, null);
        Log.d(TAG, "Long press (" + x + "," + y + "): " + sent);
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
        if (im != null) im.unregisterInputDeviceListener(this);
        nm.cancel(1);
        super.onDestroy();
    }
}
