package com.example.chris.fstest;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.content.Intent;
import android.provider.Settings;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        TextView t = new TextView(this);
        t.setText("Enable accessibility service.\nThen press ENTER on hardware keyboard.");
        t.setPadding(40, 40, 40, 40);
        setContentView(t);

        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }
}
