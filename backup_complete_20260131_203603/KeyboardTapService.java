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
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class KeyboardTapService extends AccessibilityService
        implements InputManager.InputDeviceListener {

    private static final String TAG = "IGKbd";
    private static final String IG_PACKAGE = "com.instagram.android";
    
    // EXACT coordinates provided
    private static final int SEND_X = 990;
    private static final int SEND_Y = 2313;
    
    // Screen center for reels
    private static final int CENTER_X = 540;
    private static final int CENTER_Y = 1170;
    
    // FAST reel swipes (100ms for quick response)
    private static final int SWIPE_DURATION = 100;
    private static final int SWIPE_DISTANCE = 900;

    private InputManager im;
    private NotificationManager nm;
    private boolean keyboardConnected = false;
    private boolean instagramActive = false;
    
    // Key state tracking to prevent repeats/spam
    private boolean enterKeyDown = false;
    private long lastEnterDownTime = 0;
    private static final long ENTER_COOLDOWN = 500; // ms between allowed enters
    
    // Track if we're in a text input field
    private boolean isTextInputFocused = false;

    @Override
    public void onServiceConnected() {
        Log.d(TAG, "Service connected");
        im = (InputManager) getSystemService(INPUT_SERVICE);
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("kbd", "Keyboard Helper", 
                NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
        
        im.registerInputDeviceListener(this, null);
        checkKeyboard();
    }

    private void checkKeyboard() {
        boolean found = false;
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice d = InputDevice.getDevice(id);
            if (d != null && !d.isVirtual() && 
                (d.getSources() & InputDevice.SOURCE_KEYBOARD) != 0) {
                found = true;
                break;
            }
        }
        if (found != keyboardConnected) {
            keyboardConnected = found;
            updateNotification();
        }
    }

    private void updateNotification() {
        if (!keyboardConnected || !instagramActive) {
            nm.cancel(1);
            return;
        }
        
        String text = isTextInputFocused ? 
            "IG DM - ENTER to send" : 
            "IG Active - ENTER: Send | UP/DOWN: Reels";
        
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(IG_PACKAGE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        
        Notification.Builder builder = (Build.VERSION.SDK_INT >= 26) 
            ? new Notification.Builder(this, "kbd") 
            : new Notification.Builder(this);
            
        Notification n = builder
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Instagram Keyboard")
            .setContentText(text)
            .setContentIntent(pi)
            .setOngoing(true)
            .build();
            
        nm.notify(1, n);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        // Check for Instagram package
        if (e.getPackageName() != null) {
            boolean nowIG = IG_PACKAGE.equals(e.getPackageName().toString());
            if (nowIG != instagramActive) {
                instagramActive = nowIG;
                Log.d(TAG, "Instagram active: " + instagramActive);
                updateNotification();
            }
        }
        
        // Detect if we're in a text input field
        if (instagramActive) {
            detectTextInputField(e);
        }
    }
    
    private void detectTextInputField(AccessibilityEvent e) {
        // Check if an EditText got focus
        if (e.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            AccessibilityNodeInfo node = e.getSource();
            if (node != null) {
                CharSequence className = node.getClassName();
                if (className != null) {
                    boolean wasTextInput = isTextInputFocused;
                    isTextInputFocused = className.toString().contains("EditText");
                    if (wasTextInput != isTextInputFocused) {
                        Log.d(TAG, "Text input focus changed: " + isTextInputFocused);
                        updateNotification();
                    }
                }
                node.recycle();
            }
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        // ONLY activate when BOTH keyboard connected AND Instagram is active
        if (!keyboardConnected || !instagramActive) {
            return false;
        }
        
        int key = e.getKeyCode();
        int action = e.getAction();
        
        // ENTER KEY HANDLING - CRITICAL SECTION
        if (key == KeyEvent.KEYCODE_ENTER) {
            long now = SystemClock.elapsedRealtime();
            
            if (action == KeyEvent.ACTION_DOWN) {
                // Prevent multiple DOWN events (key repeat)
                if (enterKeyDown) {
                    Log.d(TAG, "ENTER DOWN blocked - already down");
                    return true; // Block repeat
                }
                
                // Check cooldown
                if (now - lastEnterDownTime < ENTER_COOLDOWN) {
                    Log.d(TAG, "ENTER DOWN blocked - cooldown");
                    return true; // Block during cooldown
                }
                
                enterKeyDown = true;
                lastEnterDownTime = now;
                Log.d(TAG, "ENTER DOWN - consumed");
                
                // If in text input, we need to be extra aggressive
                if (isTextInputFocused) {
                    // Schedule the tap for slightly later to ensure we catch the UP
                    performTapOnUp = true;
                }
                
                return true; // Consume DOWN
                
            } else if (action == KeyEvent.ACTION_UP) {
                enterKeyDown = false;
                Log.d(TAG, "ENTER UP - tapping at (" + SEND_X + "," + SEND_Y + ")");
                
                // Perform the actual tap
                tap(SEND_X, SEND_Y, 50);
                performTapOnUp = false;
                
                return true; // Consume UP
            }
        }
        
        // UP ARROW - Previous reel (swipe DOWN) - FAST
        if (key == KeyEvent.KEYCODE_DPAD_UP && action == KeyEvent.ACTION_DOWN) {
            Log.d(TAG, "UP arrow - fast swipe DOWN");
            swipe(CENTER_X, CENTER_Y - SWIPE_DISTANCE/2, 
                  CENTER_X, CENTER_Y + SWIPE_DISTANCE/2);
            return true;
        }
        
        // DOWN ARROW - Next reel (swipe UP) - FAST
        if (key == KeyEvent.KEYCODE_DPAD_DOWN && action == KeyEvent.ACTION_DOWN) {
            Log.d(TAG, "DOWN arrow - fast swipe UP");
            swipe(CENTER_X, CENTER_Y + SWIPE_DISTANCE/2, 
                  CENTER_X, CENTER_Y - SWIPE_DISTANCE/2);
            return true;
        }
        
        return false;
    }
    
    private boolean performTapOnUp = false;

    private void tap(int x, int y, int duration) {
        if (Build.VERSION.SDK_INT < 24) {
            Log.e(TAG, "Gesture API requires API 24+");
            return;
        }
        
        Path p = new Path();
        p.moveTo(x, y);
        
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, duration))
            .build();
            
        boolean dispatched = dispatchGesture(gesture, null, null);
        Log.d(TAG, "Tap dispatched: " + dispatched + " at (" + x + "," + y + ")");
    }

    private void swipe(int x1, int y1, int x2, int y2) {
        if (Build.VERSION.SDK_INT < 24) {
            Log.e(TAG, "Gesture API requires API 24+");
            return;
        }
        
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, SWIPE_DURATION))
            .build();
            
        boolean dispatched = dispatchGesture(gesture, null, null);
        Log.d(TAG, "Swipe dispatched: " + dispatched + " duration: " + SWIPE_DURATION + "ms");
    }

    @Override public void onInputDeviceAdded(int id) { checkKeyboard(); }
    @Override public void onInputDeviceRemoved(int id) { checkKeyboard(); }
    @Override public void onInputDeviceChanged(int id) { checkKeyboard(); }
    @Override public void onInterrupt() {}
    
    @Override
    public void onDestroy() {
        if (im != null) im.unregisterInputDeviceListener(this);
        nm.cancel(1);
        super.onDestroy();
    }
    }
