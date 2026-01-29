package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.content.res.Configuration;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class SmartEnterAccessibilityService extends AccessibilityService {

    @Override
    protected void onServiceConnected() {
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags =
                AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS |
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public boolean onKeyEvent(KeyEvent event) {

        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
        if (event.getKeyCode() != KeyEvent.KEYCODE_ENTER) return false;

        // Only when hardware keyboard is present
        if (getResources().getConfiguration().keyboard
                != Configuration.KEYBOARD_QWERTY) {
            return false;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;

        AccessibilityNodeInfo sendNode = findSendNode(root);
        if (sendNode == null) return false;

        // Walk up to clickable parent
        AccessibilityNodeInfo clickable = sendNode;
        while (clickable != null && !clickable.isClickable()) {
            clickable = clickable.getParent();
        }

        if (clickable == null) return false;

        Rect r = new Rect();
        clickable.getBoundsInScreen(r);

        if (r.isEmpty()) return false;

        Path p = new Path();
        p.moveTo(r.centerX(), r.centerY());

        GestureDescription g =
                new GestureDescription.Builder()
                        .addStroke(
                                new GestureDescription.StrokeDescription(
                                        p, 0, 40
                                )
                        )
                        .build();

        dispatchGesture(g, null, null);

        // Consume Enter ONLY if gesture executed
        return true;
    }

    private AccessibilityNodeInfo findSendNode(AccessibilityNodeInfo node) {
        if (node == null) return null;

        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();

        if (text != null) {
            String t = text.toString().toLowerCase();
            if (t.contains("send") || t.contains("મોકલ")) {
                return node;
            }
        }

        if (desc != null) {
            String d = desc.toString().toLowerCase();
            if (d.contains("send")) {
                return node;
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo r = findSendNode(node.getChild(i));
            if (r != null) return r;
        }

        return null;
    }

    @Override
    public void onInterrupt() {}
}
