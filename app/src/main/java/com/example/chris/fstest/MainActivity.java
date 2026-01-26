package com.example.chris.fstest;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.chris.fstest.schedule.ScheduleConfig;
import com.example.chris.fstest.schedule.TankSchedule;

import java.util.Calendar;

public class MainActivity extends Activity {

    private TextView output;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40,40,40,40);

        CheckBox tankFill = new CheckBox(this);
        tankFill.setText("Tank Fill Today");
        tankFill.setChecked(ScheduleConfig.tankFillToday(this));

        CheckBox swap = new CheckBox(this);
        swap.setText("Force MF / Society Swap");
        swap.setChecked(ScheduleConfig.forceSwapMF(this));

        output = new TextView(this);
        output.setTextSize(15f);

        tankFill.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean c) {
                ScheduleConfig.setTankFillToday(MainActivity.this, c);
                refresh();
            }
        });

        swap.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean c) {
                ScheduleConfig.setForceSwapMF(MainActivity.this, c);
                refresh();
            }
        });

        root.addView(tankFill);
        root.addView(swap);
        root.addView(output);
        sv.addView(root);
        setContentView(sv);

        refresh();
    }

    private void refresh() {
        Calendar today = Calendar.getInstance();
        Calendar tomorrow = (Calendar) today.clone();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);

        boolean tf = ScheduleConfig.tankFillToday(this);
        boolean fs = ScheduleConfig.forceSwapMF(this);

        String text =
            "📅 TODAY\\n" +
            TankSchedule.build(today, tf, fs) +
            "\\n\\n📅 TOMORROW\\n" +
            TankSchedule.build(tomorrow, tf, fs);

        output.setText(text);
    }
}
