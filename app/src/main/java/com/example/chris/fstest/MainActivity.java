package com.example.chris.fstest;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.provider.Settings;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        TextView t = new TextView(this);
        t.setText("Enable KeyboardTapService in Accessibility Settings\nThen use ENTER to send, UP/DOWN for reels");
        t.setPadding(40, 40, 40, 40);
        setContentView(t);
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }
}
