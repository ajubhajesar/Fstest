#!/bin/bash

# Instagram Keyboard Helper - Complete Patch Script
# This script updates the Android project to implement ENTER-to-SEND functionality

set -e  # Exit on error

echo "========================================"
echo "Instagram Keyboard Helper - Patch"
echo "========================================"
echo ""

# Check if we're in the right directory
if [ ! -f "build.gradle" ] || [ ! -d "app/src/main" ]; then
    echo "ERROR: Please run this script from the project root directory"
    echo "Expected structure: build.gradle and app/src/main/"
    exit 1
fi

echo "✓ Project structure verified"
echo ""

# Backup original files
echo "Creating backups..."
BACKUP_DIR="backup_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"

if [ -f "app/src/main/java/com/example/chris/fstest/KeyboardHelperService.java" ]; then
    cp "app/src/main/java/com/example/chris/fstest/KeyboardHelperService.java" "$BACKUP_DIR/"
fi
if [ -f "app/src/main/AndroidManifest.xml" ]; then
    cp "app/src/main/AndroidManifest.xml" "$BACKUP_DIR/"
fi
if [ -f "app/src/main/java/com/example/chris/fstest/MainActivity.java" ]; then
    cp "app/src/main/java/com/example/chris/fstest/MainActivity.java" "$BACKUP_DIR/"
fi
if [ -f "app/src/main/res/layout/activity_main.xml" ]; then
    cp "app/src/main/res/layout/activity_main.xml" "$BACKUP_DIR/"
fi

echo "✓ Backups created in $BACKUP_DIR/"
echo ""

# Update KeyboardHelperService.java
echo "Updating KeyboardHelperService.java..."
cat > "app/src/main/java/com/example/chris/fstest/KeyboardHelperService.java" << 'JAVA_EOF'
package com.example.chris.fstest;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Path;
import android.hardware.input.InputManager;
import android.os.Build;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class KeyboardHelperService extends AccessibilityService
        implements InputManager.InputDeviceListener {

    private static final String INSTAGRAM_PACKAGE = "com.instagram.android";
    private static final String CHANNEL_ID = "kbd_helper";
    private static final int NOTIFICATION_ID = 1;
    
    // Coordinates for Send button (portrait mode)
    private static final int SEND_X = 990;
    private static final int SEND_Y = 2313;

    private boolean physicalKeyboardConnected = false;
    private boolean instagramActive = false;
    private InputManager inputManager;
    private NotificationManager notificationManager;

    @Override
    public void onServiceConnected() {
        inputManager = (InputManager) getSystemService(INPUT_SERVICE);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        
        createNotificationChannel();
        inputManager.registerInputDeviceListener(this, null);
        checkPhysicalKeyboard();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Keyboard Helper",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Physical keyboard status");
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void checkPhysicalKeyboard() {
        boolean keyboardFound = false;
        int[] deviceIds = InputDevice.getDeviceIds();
        
        for (int i = 0; i < deviceIds.length; i++) {
            InputDevice device = InputDevice.getDevice(deviceIds[i]);
            if (device != null && !device.isVirtual()) {
                int sources = device.getSources();
                if ((sources & InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD) {
                    keyboardFound = true;
                    break;
                }
            }
        }
        
        if (keyboardFound != physicalKeyboardConnected) {
            physicalKeyboardConnected = keyboardFound;
            updateNotification();
        }
    }

    private void updateNotification() {
        if (!physicalKeyboardConnected) {
            notificationManager.cancel(NOTIFICATION_ID);
            return;
        }

        String title = "Physical keyboard connected";
        String text = instagramActive ? "Instagram active - Enter sends" : "Waiting for Instagram";

        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_notify_more)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setOngoing(true)
                    .build();
        } else {
            notification = new Notification.Builder(this)
                    .setSmallIcon(android.R.drawable.stat_notify_more)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_LOW)
                    .build();
        }

        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence packageName = event.getPackageName();
            boolean newState = packageName != null && INSTAGRAM_PACKAGE.contentEquals(packageName);
            
            if (newState != instagramActive) {
                instagramActive = newState;
                if (physicalKeyboardConnected) {
                    updateNotification();
                }
            }
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        // Only intercept ENTER key on physical keyboard when Instagram is active
        if (physicalKeyboardConnected && instagramActive) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    performSendTap();
                }
                // Consume both DOWN and UP to prevent newline insertion
                return true;
            }
        }
        return false;
    }

    private void performSendTap() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }

        Path path = new Path();
        path.moveTo(SEND_X, SEND_Y);
        path.lineTo(SEND_X, SEND_Y);

        GestureDescription.StrokeDescription stroke = 
                new GestureDescription.StrokeDescription(path, 0, 50);
        
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke)
                .build();

        dispatchGesture(gesture, null, null);
    }

    @Override
    public void onInterrupt() {
        // Required override - nothing to do
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        checkPhysicalKeyboard();
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        checkPhysicalKeyboard();
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        checkPhysicalKeyboard();
    }

    @Override
    public void onDestroy() {
        if (inputManager != null) {
            inputManager.unregisterInputDeviceListener(this);
        }
        notificationManager.cancel(NOTIFICATION_ID);
        super.onDestroy();
    }
}
JAVA_EOF

echo "✓ KeyboardHelperService.java updated"

# Update AndroidManifest.xml
echo "Updating AndroidManifest.xml..."
cat > "app/src/main/AndroidManifest.xml" << 'XML_EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.chris.fstest">

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">
        <activity android:name=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <service
            android:name=".KeyboardHelperService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="false">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service" />
        </service>
    </application>

</manifest>
XML_EOF

echo "✓ AndroidManifest.xml updated"

# Update MainActivity.java
echo "Updating MainActivity.java..."
cat > "app/src/main/java/com/example/chris/fstest/MainActivity.java" << 'JAVA_EOF'
package com.example.chris.fstest;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView infoText = (TextView) findViewById(R.id.info_text);
        Button settingsButton = (Button) findViewById(R.id.settings_button);

        infoText.setText("Instagram Keyboard Helper\n\n" +
                "This app makes ENTER send messages in Instagram when using a physical keyboard.\n\n" +
                "To enable:\n" +
                "1. Tap 'Open Settings' below\n" +
                "2. Find 'Keyboard Helper Service'\n" +
                "3. Turn it ON\n" +
                "4. Connect your physical keyboard\n" +
                "5. Open Instagram and start typing!");

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
        });
    }
}
JAVA_EOF

echo "✓ MainActivity.java updated"

# Update activity_main.xml
echo "Updating activity_main.xml..."
cat > "app/src/main/res/layout/activity_main.xml" << 'XML_EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="24dp"
    android:gravity="center">

    <TextView
        android:id="@+id/info_text"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="16sp"
        android:lineSpacingExtra="4dp" />

    <Button
        android:id="@+id/settings_button"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="32dp"
        android:text="Open Settings"
        android:textSize="18sp"
        android:paddingLeft="32dp"
        android:paddingRight="32dp" />

</LinearLayout>
XML_EOF

echo "✓ activity_main.xml updated"

# Remove unnecessary files
echo ""
echo "Cleaning up unnecessary files..."
rm -f app/src/main/java/com/example/chris/fstest/ScheduleData.java
rm -rf app/src/main/java/com/example/chris/fstest/schedule/
echo "✓ Cleanup complete"

echo ""
echo "========================================"
echo "✓ PATCH APPLIED SUCCESSFULLY!"
echo "========================================"
echo ""
echo "What was changed:"
echo "  • KeyboardHelperService: Complete rewrite with proper notification handling"
echo "  • AndroidManifest: Fixed service name reference"
echo "  • MainActivity: Simplified to guide users"
echo "  • activity_main.xml: Clean layout"
echo "  • Removed: Schedule-related files"
echo ""
echo "Next steps:"
echo "  1. Build the project: ./gradlew assembleDebug"
echo "  2. Install on device: adb install app/build/outputs/apk/debug/app-debug.apk"
echo "  3. Enable accessibility service in Settings"
echo "  4. Connect physical keyboard and test in Instagram"
echo ""
echo "Backups saved in: $BACKUP_DIR/"
echo ""
