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
