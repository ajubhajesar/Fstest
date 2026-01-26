package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.content.res.Configuration;

public class SmartEnterAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) return false;
        if (event.getKeyCode() != KeyEvent.KEYCODE_ENTER) return false;
        if (event.isShiftPressed()) return false;

        if (getResources().getConfiguration().keyboard != Configuration.KEYBOARD_QWERTY)
            return false;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;

        CharSequence pkg = root.getPackageName();
        if (pkg == null) return false;

        String p = pkg.toString();
        if (!p.equals("com.instagram.android") && !p.equals("com.openai.chatgpt"))
            return false;

        for (AccessibilityNodeInfo n : root.findAccessibilityNodeInfosByText("Send")) {
            n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return true;
        }
        return false;
    }

    @Override
    public void onInterrupt() {}
}
