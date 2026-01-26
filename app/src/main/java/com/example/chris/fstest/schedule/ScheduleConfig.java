package com.example.chris.fstest.schedule;

import android.content.Context;
import android.content.SharedPreferences;

public class ScheduleConfig {
    private static final String PREFS = "schedule_env";

    public static boolean tankFillToday(Context c) {
        return prefs(c).getBoolean("TANK_FILL_TODAY", false);
    }

    public static boolean forceSwapMF(Context c) {
        return prefs(c).getBoolean("MF_SOCIETY_FORCE_SWAP", false);
    }

    public static void setTankFillToday(Context c, boolean v) {
        prefs(c).edit().putBoolean("TANK_FILL_TODAY", v).apply();
    }

    public static void setForceSwapMF(Context c, boolean v) {
        prefs(c).edit().putBoolean("MF_SOCIETY_FORCE_SWAP", v).apply();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
