package com.example.chris.fstest;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class MainActivity extends Activity {

    private boolean tankFillToday = false;
    private boolean forceSwap = false;
    private WebView web;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(root);

        CheckBox tank = new CheckBox(this);
        tank.setText("Tank Fill Today");
        root.addView(tank);

        CheckBox swap = new CheckBox(this);
        swap.setText("Force MF / Society Swap");
        root.addView(swap);

        web = new WebView(this);
        root.addView(web);

        tank.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean v) {
                tankFillToday = v;
                render();
            }
        });

        swap.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean v) {
                forceSwap = v;
                render();
            }
        });

        setContentView(scroll);
        render();
    }

    private void render() {
        ScheduleData.DaySchedule today =
            ScheduleData.today(forceSwap);
        ScheduleData.DaySchedule tomorrow =
            ScheduleData.tomorrow(forceSwap);

        StringBuilder h = new StringBuilder();
        h.append("<html><body style=font-family:sans-serif>");

        h.append(day(today));
        h.append(day(tomorrow));

        h.append("</body></html>");
        web.loadDataWithBaseURL(null, h.toString(), "text/html", "utf-8", null);
    }

    private String day(ScheduleData.DaySchedule d) {
        StringBuilder s = new StringBuilder();
        s.append("<div style=margin:16px;padding:12px;border-radius:12px;background:#f7f7f7>");
        s.append("<h2>").append(d.title).append("</h2>");
        s.append("<div><b>પાણીનો સ્ત્રોત:</b> ").append(d.source).append("</div>");

        if (tankFillToday && "આજે".equals(d.title)) {
            s.append("<div style=color:#b35c00;font-weight:bold;margin-top:6px>🚰 ટાંકી ભરવાનું કાર્ય</div>");
        }

        for (int i = 0; i < d.slots.size(); i++) {
            ScheduleData.Slot sl = d.slots.get(i);
            s.append("<div style=margin-top:10px>");
            s.append("<b>").append(sl.time).append("</b><br/>");
            s.append(sl.area);
            s.append("</div>");
        }

        s.append("</div>");
        return s.toString();
    }
}
