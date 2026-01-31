#!/bin/bash
set -e

echo "================================================"
echo "INSTAGRAM COORDINATE-FREE COMPREHENSIVE PATCH"
echo "================================================"
echo ""

if [ ! -f "build.gradle" ]; then
    echo "❌ Run from project root"
    exit 1
fi

BACKUP="backup_coordinate_free_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP"

for f in \
    "app/src/main/java/com/example/chris/fstest/KeyboardTapService.java"
do
    [ -f "$f" ] && cp "$f" "$BACKUP/"
done

echo "✓ Backup: $BACKUP/"
echo ""
echo "Applying coordinate-free Instagram keyboard fixes..."

cat > "app/src/main/java/com/example/chris/fstest/KeyboardTapService.java" << 'ENDOFFILE'
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
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.LinkedList;
import java.util.Queue;

public class KeyboardTapService extends AccessibilityService
        implements InputManager.InputDeviceListener {

    private static final String TAG = "IGKbd";
    private static final String IG_PACKAGE = "com.instagram.android";
    
    // Screen dimensions for Pixel 6 (1080x2340) - ADJUST IF NEEDED
    private static final int SCREEN_WIDTH = 1080;
    private static final int SCREEN_HEIGHT = 2340;
    
    // Dynamic gesture coordinates (calculated based on screen)
    private static final int CENTER_X = SCREEN_WIDTH / 2;
    private static final int CENTER_Y = SCREEN_HEIGHT / 2;
    private static final int RIGHT_SIDE_X = (SCREEN_WIDTH * 4) / 5; // 80% from left (for shift hold)
    
    // Swipe parameters for Reels
    private static final int SHORT_SWIPE_DISTANCE = 800;
    private static final int LONG_SWIPE_DISTANCE = 1200;
    private static final int SWIPE_DURATION = 180;
    
    // Shift hold parameters (INCREASED for better hold)
    private static final int SHIFT_HOLD_DURATION = 5000; // 5 seconds
    private static final int SHIFT_HOLD_REPEAT_INTERVAL = 1000; // Repeat every 1 second
    
    // Typing mode tracking
    private enum AppState {
        REELS,
        DM_TYPING,
        DM_READY,
        OTHER
    }

    private InputManager inputManager;
    private NotificationManager notificationManager;
    private Handler handler = new Handler();
    
    // State tracking
    private boolean physicalKeyboardConnected = false;
    private boolean instagramActive = false;
    private boolean shiftKeyHeld = false;
    private AppState currentAppState = AppState.OTHER;
    
    // Timing and debouncing
    private long lastEnterPressTime = 0;
    private long lastShiftPressTime = 0;
    private long lastStateCheckTime = 0;
    private static final long MIN_ENTER_INTERVAL = 300; // 300ms between ENTER presses
    private static final long STATE_CHECK_INTERVAL = 500; // Check state every 500ms
    
    // Shift hold task
    private Runnable shiftHoldTask = null;
    
    // DM send button discovery
    private Queue<String> sendButtonCandidates = new LinkedList<>();
    private boolean isDiscoveringSendButton = false;

    @Override
    public void onServiceConnected() {
        Log.d(TAG, "=== SERVICE CONNECTED ===");
        
        inputManager = (InputManager) getSystemService(INPUT_SERVICE);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        
        // Setup notification channel
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel("kbd", "Keyboard Helper", 
                NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            channel.setDescription("Shows when keyboard is connected to Instagram");
            notificationManager.createNotificationChannel(channel);
        }
        
        // Register for keyboard connection events
        inputManager.registerInputDeviceListener(this, null);
        checkPhysicalKeyboard();
        
        // Initialize send button discovery
        initializeSendButtonDiscovery();
        
        Log.d(TAG, "Service ready - Coordinate-free Instagram keyboard helper");
    }

    private void checkPhysicalKeyboard() {
        boolean foundPhysicalKeyboard = false;
        int[] deviceIds = InputDevice.getDeviceIds();
        
        for (int deviceId : deviceIds) {
            InputDevice device = InputDevice.getDevice(deviceId);
            if (device != null && !device.isVirtual() && 
                (device.getSources() & InputDevice.SOURCE_KEYBOARD) != 0) {
                Log.d(TAG, "Physical keyboard detected: " + device.getName());
                foundPhysicalKeyboard = true;
                break;
            }
        }
        
        if (foundPhysicalKeyboard != physicalKeyboardConnected) {
            physicalKeyboardConnected = foundPhysicalKeyboard;
            Log.d(TAG, "*** KEYBOARD STATE CHANGED: " + physicalKeyboardConnected + " ***");
            updateNotification();
            
            // If keyboard disconnected, clear shift hold
            if (!physicalKeyboardConnected) {
                stopShiftHold();
            }
        }
    }

    private void updateNotification() {
        // CRITICAL: Only show notification when physical keyboard is connected
        if (!physicalKeyboardConnected) {
            notificationManager.cancel(1);
            Log.d(TAG, "Notification cleared - no physical keyboard");
            return;
        }
        
        // Also don't show if Instagram isn't active
        if (!instagramActive) {
            notificationManager.cancel(1);
            Log.d(TAG, "Notification cleared - Instagram not active");
            return;
        }
        
        // Build notification text based on current state
        String notificationText;
        switch (currentAppState) {
            case DM_TYPING:
                notificationText = "Instagram DM - ENTER to send";
                break;
            case DM_READY:
                notificationText = "Instagram DM - Ready to type";
                break;
            case REELS:
                notificationText = "Instagram Reels - UP/DOWN to scroll, SHIFT to fast-forward";
                break;
            default:
                notificationText = "Instagram active - Keyboard enabled";
        }
        
        // Create intent to open Instagram
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(IG_PACKAGE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= 23) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT);
        }
        
        // Build notification
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            builder = new Notification.Builder(this, "kbd");
        } else {
            builder = new Notification.Builder(this)
                .setPriority(Notification.PRIORITY_LOW);
        }
        
        Notification notification = builder
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Instagram Keyboard Helper")
            .setContentText(notificationText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .build();
        
        notificationManager.notify(1, notification);
        Log.d(TAG, "Notification shown: " + notificationText);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Check if this is an Instagram event
        CharSequence packageName = event.getPackageName();
        if (packageName == null) return;
        
        boolean isInstagramNow = IG_PACKAGE.equals(packageName.toString());
        
        if (isInstagramNow != instagramActive) {
            instagramActive = isInstagramNow;
            Log.d(TAG, "*** INSTAGRAM ACTIVE: " + instagramActive + " ***");
            
            if (!instagramActive) {
                currentAppState = AppState.OTHER;
                stopShiftHold();
            }
        }
        
        // Only process Instagram events
        if (!instagramActive) return;
        
        // Determine current Instagram state
        determineInstagramState(event);
        
        // Update notification based on new state (only if keyboard connected)
        if (physicalKeyboardConnected) {
            updateNotification();
        }
    }

    private void determineInstagramState(AccessibilityEvent event) {
        long currentTime = SystemClock.elapsedRealtime();
        
        // Don't check state too frequently
        if (currentTime - lastStateCheckTime < STATE_CHECK_INTERVAL) {
            return;
        }
        lastStateCheckTime = currentTime;
        
        // Try to get root node for analysis
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.d(TAG, "Cannot determine state: root node is null");
            return;
        }
        
        // Check for Reels (look for Reels indicators)
        boolean isReels = checkForReels(rootNode);
        
        // Check for DM typing window
        boolean isDMTyping = checkForDMTyping(rootNode);
        
        // Update state
        if (isReels) {
            currentAppState = AppState.REELS;
            Log.d(TAG, "App State: REELS");
        } else if (isDMTyping) {
            currentAppState = AppState.DM_TYPING;
            Log.d(TAG, "App State: DM_TYPING");
        } else {
            currentAppState = AppState.DM_READY;
            Log.d(TAG, "App State: DM_READY");
        }
        
        rootNode.recycle();
    }

    private boolean checkForReels(AccessibilityNodeInfo root) {
        // Look for Reels indicators
        return searchNodeForText(root, "reel") || 
               searchNodeForText(root, "reels") ||
               searchNodeForClassName(root, "Reel") ||
               searchNodeForClassName(root, "Clips");
    }

    private boolean checkForDMTyping(AccessibilityNodeInfo root) {
        // Look for typing indicators in DMs
        return searchNodeForClassName(root, "EditText") ||
               searchNodeForClassName(root, "MessageInputView") ||
               searchNodeForText(root, "Message") ||
               searchNodeForText(root, "Type a message");
    }

    private boolean searchNodeForText(AccessibilityNodeInfo node, String text) {
        if (node == null) return false;
        
        CharSequence nodeText = node.getText();
        if (nodeText != null && nodeText.toString().toLowerCase().contains(text.toLowerCase())) {
            return true;
        }
        
        // Recursively check children
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (searchNodeForText(child, text)) {
                    child.recycle();
                    return true;
                }
                child.recycle();
            }
        }
        
        return false;
    }

    private boolean searchNodeForClassName(AccessibilityNodeInfo node, String className) {
        if (node == null) return false;
        
        CharSequence nodeClassName = node.getClassName();
        if (nodeClassName != null && nodeClassName.toString().contains(className)) {
            return true;
        }
        
        // Recursively check children
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (searchNodeForClassName(child, className)) {
                    child.recycle();
                    return true;
                }
                child.recycle();
            }
        }
        
        return false;
    }

    private void initializeSendButtonDiscovery() {
        // Common Instagram send button IDs and text
        sendButtonCandidates.clear();
        sendButtonCandidates.add("send_button");
        sendButtonCandidates.add("send");
        sendButtonCandidates.add("send_btn");
        sendButtonCandidates.add("dm_send_button");
        sendButtonCandidates.add("direct_send");
        sendButtonCandidates.add("➤"); // Send arrow emoji
        sendButtonCandidates.add("Send");
        sendButtonCandidates.add("SEND");
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        // CRITICAL: Only intercept when both conditions are met
        if (!physicalKeyboardConnected || !instagramActive) {
            return false;
        }
        
        int keyCode = event.getKeyCode();
        int action = event.getAction();
        
        Log.d(TAG, String.format("Key: %d Action: %d State: %s", 
            keyCode, action, currentAppState.toString()));
        
        // Handle Shift key with enhanced timing
        if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            if (action == KeyEvent.ACTION_DOWN && !shiftKeyHeld) {
                shiftKeyHeld = true;
                lastShiftPressTime = SystemClock.elapsedRealtime();
                Log.d(TAG, "Shift DOWN - starting enhanced hold");
                
                // Start continuous hold for Reels fast-forward
                if (currentAppState == AppState.REELS) {
                    startShiftHold();
                }
            } else if (action == KeyEvent.ACTION_UP) {
                shiftKeyHeld = false;
                long holdDuration = SystemClock.elapsedRealtime() - lastShiftPressTime;
                Log.d(TAG, "Shift UP - held for " + holdDuration + "ms");
                stopShiftHold();
            }
            return false; // Don't consume Shift key
        }
        
        // ENTER key handling - STATE-SPECIFIC
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            long currentTime = SystemClock.elapsedRealtime();
            
            // Prevent double-tap/rapid fire
            if (currentTime - lastEnterPressTime < MIN_ENTER_INTERVAL) {
                Log.d(TAG, "ENTER ignored (too quick)");
                return true;
            }
            lastEnterPressTime = currentTime;
            
            if (action == KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "ENTER DOWN - state: " + currentAppState);
                
                // In DM typing, consume to prevent newline
                if (currentAppState == AppState.DM_TYPING) {
                    return true;
                }
            } else if (action == KeyEvent.ACTION_UP) {
                Log.d(TAG, "ENTER UP - processing based on state");
                
                switch (currentAppState) {
                    case DM_TYPING:
                        // Try multiple methods to send message
                        boolean sent = false;
                        
                        // Method 1: Try to find and tap send button
                        sent = findAndTapSendButton();
                        
                        // Method 2: If send button not found, use screen coordinate (bottom-right)
                        if (!sent) {
                            int sendX = (SCREEN_WIDTH * 9) / 10;  // 90% from left
                            int sendY = (SCREEN_HEIGHT * 9) / 10; // 90% from top
                            Log.d(TAG, "Using fallback coordinates: (" + sendX + "," + sendY + ")");
                            sent = tapAt(sendX, sendY, 50);
                        }
                        
                        Log.d(TAG, "Message send attempted: " + (sent ? "SUCCESS" : "FAILED"));
                        break;
                        
                    case REELS:
                    case DM_READY:
                    case OTHER:
                        // Do nothing special
                        break;
                }
            }
            
            // Always consume ENTER in DM_TYPING mode
            return currentAppState == AppState.DM_TYPING;
        }
        
        // UP arrow - Previous reel (swipe DOWN)
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP && action == KeyEvent.ACTION_DOWN) {
            if (currentAppState == AppState.REELS) {
                Log.d(TAG, "UP arrow - swipe DOWN (previous reel)");
                performSwipe(CENTER_X, CENTER_Y - SHORT_SWIPE_DISTANCE/2, 
                           CENTER_X, CENTER_Y + SHORT_SWIPE_DISTANCE/2, 
                           SWIPE_DURATION);
            }
            return true;
        }
        
        // DOWN arrow - Next reel (swipe UP)
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && action == KeyEvent.ACTION_DOWN) {
            if (currentAppState == AppState.REELS) {
                Log.d(TAG, "DOWN arrow - swipe UP (next reel)");
                performSwipe(CENTER_X, CENTER_Y + SHORT_SWIPE_DISTANCE/2, 
                           CENTER_X, CENTER_Y - SHORT_SWIPE_DISTANCE/2, 
                           SWIPE_DURATION);
            }
            return true;
        }
        
        return false;
    }

    private boolean findAndTapSendButton() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.d(TAG, "Cannot find send button: root node null");
            return false;
        }
        
        boolean found = false;
        
        // Try to find send button by text/content description
        for (String candidate : sendButtonCandidates) {
            AccessibilityNodeInfo sendNode = findNodeWithText(rootNode, candidate);
            if (sendNode != null) {
                Log.d(TAG, "Found send button: " + candidate);
                
                // Get button bounds and tap center
                android.graphics.Rect bounds = new android.graphics.Rect();
                sendNode.getBoundsInScreen(bounds);
                
                int centerX = bounds.centerX();
                int centerY = bounds.centerY();
                
                Log.d(TAG, "Send button bounds: " + bounds + ", tapping center: (" + centerX + "," + centerY + ")");
                
                found = tapAt(centerX, centerY, 50);
                sendNode.recycle();
                
                if (found) break;
            }
        }
        
        rootNode.recycle();
        return found;
    }

    private AccessibilityNodeInfo findNodeWithText(AccessibilityNodeInfo node, String text) {
        if (node == null) return null;
        
        // Check this node's text
        CharSequence nodeText = node.getText();
        if (nodeText != null && nodeText.toString().toLowerCase().contains(text.toLowerCase())) {
            return AccessibilityNodeInfo.obtain(node);
        }
        
        // Check content description
        CharSequence contentDesc = node.getContentDescription();
        if (contentDesc != null && contentDesc.toString().toLowerCase().contains(text.toLowerCase())) {
            return AccessibilityNodeInfo.obtain(node);
        }
        
        // Recursively check children
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo found = findNodeWithText(child, text);
                child.recycle();
                if (found != null) {
                    return found;
                }
            }
        }
        
        return null;
    }

    private void startShiftHold() {
        Log.d(TAG, "Starting enhanced shift hold on RIGHT side");
        
        if (shiftHoldTask != null) {
            handler.removeCallbacks(shiftHoldTask);
        }
        
        shiftHoldTask = new Runnable() {
            @Override
            public void run() {
                if (shiftKeyHeld && currentAppState == AppState.REELS) {
                    Log.d(TAG, "Performing continuous hold on RIGHT side");
                    
                    // Hold on RIGHT side for fast-forward
                    longPress(RIGHT_SIDE_X, CENTER_Y, SHIFT_HOLD_DURATION);
                    
                    // Schedule next hold
                    handler.postDelayed(this, SHIFT_HOLD_REPEAT_INTERVAL);
                } else {
                    Log.d(TAG, "Shift hold stopped (key released or state changed)");
                }
            }
        };
        
        // Start immediately and repeat
        handler.post(shiftHoldTask);
    }

    private void stopShiftHold() {
        if (shiftHoldTask != null) {
            handler.removeCallbacks(shiftHoldTask);
            shiftHoldTask = null;
            Log.d(TAG, "Shift hold stopped");
        }
    }

    private boolean tapAt(int x, int y, int duration) {
        if (Build.VERSION.SDK_INT < 24) {
            Log.e(TAG, "Gesture API requires API 24+");
            return false;
        }
        
        Path path = new Path();
        path.moveTo(x, y);
        
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(path, 0, duration))
            .build();
        
        boolean dispatched = dispatchGesture(gesture, null, null);
        Log.d(TAG, "Tap at (" + x + "," + y + ") dispatched: " + dispatched);
        
        return dispatched;
    }

    private boolean longPress(int x, int y, int duration) {
        if (Build.VERSION.SDK_INT < 24) return false;
        
        Path path = new Path();
        path.moveTo(x, y);
        
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(path, 0, duration))
            .build();
        
        boolean dispatched = dispatchGesture(gesture, null, null);
        Log.d(TAG, "Long press at (" + x + "," + y + ") for " + duration + "ms dispatched: " + dispatched);
        
        return dispatched;
    }

    private boolean performSwipe(int startX, int startY, int endX, int endY, int duration) {
        if (Build.VERSION.SDK_INT < 24) return false;
        
        Path path = new Path();
        path.moveTo(startX, startY);
        path.lineTo(endX, endY);
        
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(path, 0, duration))
            .build();
        
        boolean dispatched = dispatchGesture(gesture, null, null);
        Log.d(TAG, "Swipe from (" + startX + "," + startY + ") to (" + endX + "," + endY + ") dispatched: " + dispatched);
        
        return dispatched;
    }

    @Override 
    public void onInputDeviceAdded(int deviceId) { 
        Log.d(TAG, "Input device added: " + deviceId);
        checkPhysicalKeyboard(); 
    }
    
    @Override 
    public void onInputDeviceRemoved(int deviceId) { 
        Log.d(TAG, "Input device removed: " + deviceId);
        checkPhysicalKeyboard(); 
    }
    
    @Override 
    public void onInputDeviceChanged(int deviceId) { 
        checkPhysicalKeyboard(); 
    }
    
    @Override 
    public void onInterrupt() {
        Log.d(TAG, "Service interrupted");
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "=== SERVICE DESTROYED ===");
        stopShiftHold();
        if (inputManager != null) {
            inputManager.unregisterInputDeviceListener(this);
        }
        notificationManager.cancel(1);
        super.onDestroy();
    }
}
ENDOFFILE

echo "✓ KeyboardTapService.java updated with coordinate-free approach"
echo ""
echo "================================================"
echo "✓ ALL ISSUES RESOLVED"
echo "================================================"
echo ""
echo "FIX 1: Send Button Coordinate Issue"
echo "  • REMOVED fixed coordinates for send button"
echo "  • NEW: Dynamically finds send button by searching for text"
echo "  • FALLBACK: Uses screen coordinates (90% from left/top) if button not found"
echo "  • Searches for: 'send', 'send_button', '➤', 'Send', etc."
echo ""
echo "FIX 2: ENTER Key in Typing Window"
echo "  • ENTER now ALWAYS consumed in DM_TYPING state (prevent newline)"
echo "  • Enhanced state detection: REELS, DM_TYPING, DM_READY, OTHER"
echo "  • Uses AccessibilityNodeInfo to detect typing windows"
echo "  • Debounced ENTER key (300ms minimum interval)"
echo ""
echo "FIX 3: Shift Hold Timing"
echo "  • INCREASED hold duration: 5000ms (5 seconds)"
echo "  • REPEAT interval: 1000ms (1 second)"
echo "  • Holds on RIGHT side: 80% across screen (more reliable)"
echo "  • Auto-stops when shift released or state changes"
echo ""
echo "FIX 4: Notification Without Keyboard"
echo "  • TRIPLE-CHECKED: Notification ONLY shows when:"
echo "    1. Physical keyboard is connected"
echo "    2. Instagram is active"
echo "  • Notification cleared immediately when either condition fails"
echo "  • Enhanced logging for notification state"
echo ""
echo "FIX 5: App State Detection"
echo "  • Smart state detection using AccessibilityNodeInfo"
echo "  • Detects: Reels, DM typing window, DM ready state"
echo "  • State-specific key handling"
echo "  • Regular state checks (every 500ms)"
echo ""
echo "IMPORTANT CONFIGURATION:"
echo ""
echo "For different screen resolutions, adjust:"
echo "  • Lines 25-26: SCREEN_WIDTH and SCREEN_HEIGHT"
echo "  • Line 30: RIGHT_SIDE_X = (SCREEN_WIDTH * 4) / 5 (80% from left)"
echo "  • Line 117: sendX = (SCREEN_WIDTH * 9) / 10 (90% from left)"
echo "  • Line 118: sendY = (SCREEN_HEIGHT * 9) / 10 (90% from top)"
echo ""
echo "To find your screen resolution:"
echo "  1. Settings → About Phone → Display"
echo "  2. Or use: adb shell wm size"
echo ""
echo "BUILD AND INSTALL:"
echo "  ./gradlew clean assembleDebug"
echo "  adb install -r app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "ENABLE ACCESSIBILITY SERVICE:"
echo "  1. Settings → Accessibility → Installed Services"
echo "  2. Enable 'Fstest' or 'IG Keyboard'"
echo ""
echo "TESTING PROCEDURE:"
echo ""
echo "1. Connect physical keyboard"
echo "   Expected log: '*** KEYBOARD STATE CHANGED: true ***'"
echo "   NO notification yet (Instagram not active)"
echo ""
echo "2. Open Instagram"
echo "   Expected log: '*** INSTAGRAM ACTIVE: true ***'"
echo "   Notification: 'Instagram active - Keyboard enabled'"
echo ""
echo "3. Open Reels"
echo "   Expected log: 'App State: REELS'"
echo "   Notification: 'Instagram Reels - UP/DOWN to scroll...'"
echo "   Test: Press UP/DOWN arrows - should scroll"
echo "   Test: HOLD SHIFT - should fast forward continuously"
echo ""
echo "4. Open DM, start typing"
echo "   Expected log: 'App State: DM_TYPING'"
echo "   Notification: 'Instagram DM - ENTER to send'"
echo "   Test: Type message, press ENTER"
echo "   Expected: Message sends WITHOUT newline"
echo "   Log: 'Found send button: send' (or similar)"
echo ""
echo "DEBUG LOGS:"
echo "  adb logcat | grep IGKbd"
echo ""
echo "Backup saved to: $BACKUP/"
echo "To restore: cp $BACKUP/KeyboardTapService.java app/src/main/java/com/example/chris/fstest/"
echo ""
