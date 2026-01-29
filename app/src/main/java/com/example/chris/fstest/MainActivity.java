package com.example.chris.fstest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        TextView t = new TextView(this);
        t.setPadding(40, 40, 40, 40);
        t.setText(
            "Instagram ENTER → SEND\n\n" +
            "1. Enable Accessibility Service\n" +
            "2. Connect physical keyboard\n" +
            "3. Open Instagram\n" +
            "4. Press ENTER to send"
        );

        setContentView(t);

        // Open accessibility settings automatically
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }
}
