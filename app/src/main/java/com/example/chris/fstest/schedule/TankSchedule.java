package com.example.chris.fstest.schedule;

import java.util.Calendar;

public class TankSchedule {

    private static final Calendar SEED;
    static {
        SEED = Calendar.getInstance();
        SEED.set(2025, Calendar.AUGUST, 29, 0, 0, 0);
        SEED.set(Calendar.MILLISECOND, 0);
    }

    public static String build(Calendar day, boolean tankFill, boolean forceSwap) {
        long diff = (day.getTimeInMillis() - SEED.getTimeInMillis()) / (1000L*60*60*24);
        boolean borewell = diff % 2 == 0;

        String source = borewell ? "બોરવેલ" : "નર્મદા";

        String a = "યાદવ નગરી + ચૌધરી ફરીયો";
        String b = "મફત નગરી";
        String c = "સોસાયટી";

        if (forceSwap) {
            String t = b; b = c; c = t;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Source: ").append(source).append("\\n");

        if (tankFill && day.get(Calendar.DAY_OF_MONTH) <= 15) {
            sb.append("ટાંકો ભરાઈ રહયો છે\\n");
        }

        sb.append("• ").append(a).append("\\n");
        sb.append("• ").append(b).append("\\n");
        sb.append("• ").append(c).append("\\n");

        sb.append("\\nSlots:\\n");
        sb.append("Morning / Afternoon");

        return sb.toString();
    }
}
