package com.example.chris.fstest;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        TextView t = new TextView(this);
        t.setPadding(40,40,40,40);
        t.setText(
            "Instagram ENTER → SEND\n\n" +
            "• Enable accessibility\n" +
            "• Connect physical keyboard\n" +
            "• Open Instagram\n" +
            "• Press ENTER"
        );
        setContentView(t);
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }
}
