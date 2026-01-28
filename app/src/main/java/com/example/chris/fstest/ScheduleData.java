package com.example.chris.fstest;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public final class ScheduleData {

    private static final String YADAV = "યાદવ નગરી + ચૌધરી ફરીયો";
    private static final String SOCIETY = "સોસાયટી";
    private static final String MF = "મફત નગરી";
    private static final String REMAIN = "બાકીનો વિસ્તાર";
    private static final String TANK_FILL = "ટાંકો ભરાઈ રહયો છે";

    private static final Calendar SEED;
    private static final boolean SEED_SOURCE_BOREWELL;

    static {
        SEED = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        SEED.set(2025, Calendar.AUGUST, 29, 0, 0, 0);
        SEED.set(Calendar.MILLISECOND, 0);
        SEED_SOURCE_BOREWELL = true;
    }

    public static final class Slot {
        public final String time;
        public final String area;
        public final boolean active;

        public Slot(String t, String a, boolean ac) {
            time = t;
            area = a;
            active = ac;
        }
    }

    public static final class DaySchedule {
        public final String title;
        public final String source;
        public final List<Slot> slots;

        public DaySchedule(String t, String s, List<Slot> sl) {
            title = t;
            source = s;
            slots = sl;
        }
    }

    public static DaySchedule today(boolean tankFill, boolean forceSwap) {
        Calendar c = todayDate();
        return build("આજે", c, tankFill, forceSwap, true);
    }

    public static DaySchedule tomorrow(boolean forceSwap) {
        Calendar c = todayDate();
        c.add(Calendar.DAY_OF_MONTH, 1);
        return build("આવતીકાલે", c, false, forceSwap, false);
    }

    private static DaySchedule build(
            String title,
            Calendar day,
            boolean tankFill,
            boolean forceSwap,
            boolean markActive
    ) {
        long diff = daysBetween(SEED, day);

        boolean borewell =
                (diff % 2 == 0) ? SEED_SOURCE_BOREWELL : !SEED_SOURCE_BOREWELL;

        String source =
                borewell
                        ? "Borewell | બોરવેલ"
                        : "Narmada | નર્મદા";

        boolean firstHalf = day.get(Calendar.DAY_OF_MONTH) <= 15;

        boolean societyFirst =
                (diff % 2 == 0);

        if (forceSwap) {
            societyFirst = !societyFirst;
        }

        String firstPartner = societyFirst ? SOCIETY : MF;
        String secondPartner = societyFirst ? MF : SOCIETY;

        List<Slot> slots = new ArrayList<Slot>();

        Calendar now = todayDate();
        now.setTimeInMillis(System.currentTimeMillis());

        if (firstHalf) {
            if (tankFill) {
                slots.add(slot("06:00 – 09:00", REMAIN, now, markActive));
                slots.add(slot("09:00 – 11:00", TANK_FILL, now, markActive));
                slots.add(slot("11:00 – 12:30", YADAV + " + " + firstPartner, now, markActive));
                slots.add(slot("12:30 – 14:00", secondPartner, now, markActive));
            } else {
                slots.add(slot("06:00 – 09:00", REMAIN, now, markActive));
                slots.add(slot("09:00 – 10:30", firstPartner, now, markActive));
                slots.add(slot("10:30 – 12:00", secondPartner, now, markActive));
                slots.add(slot("12:00 – 13:30", YADAV, now, markActive));
            }
        } else {
            slots.add(slot("06:00 – 07:30", YADAV + " + " + firstPartner, now, markActive));
            slots.add(slot("07:30 – 09:00", secondPartner, now, markActive));
            slots.add(slot("09:00 – 12:00", REMAIN, now, markActive));
        }

        return new DaySchedule(title, source, slots);
    }

    private static Slot slot(
            String time,
            String area,
            Calendar now,
            boolean markActive
    ) {
        boolean active = false;
        if (markActive) {
            int h = now.get(Calendar.HOUR_OF_DAY);
            int m = now.get(Calendar.MINUTE);
            int t = h * 60 + m;

            String[] p = time.split("–");
            int s = parse(p[0]);
            int e = parse(p[1]);

            active = t >= s && t < e;
        }
        return new Slot(time, area, active);
    }

    private static int parse(String s) {
        s = s.trim();
        String[] p = s.split(":");
        return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
    }

    private static Calendar todayDate() {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    private static long daysBetween(Calendar a, Calendar b) {
        return (b.getTimeInMillis() - a.getTimeInMillis())
                / (24L * 60L * 60L * 1000L);
    }
}
