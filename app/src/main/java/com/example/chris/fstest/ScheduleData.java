package com.example.chris.fstest;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public final class ScheduleData {

    /* ========= CONSTANTS (FROM PYTHON) ========= */

    private static final String YADAV = "યાદવ નગરી + ચૌધરી ફરીયો";
    private static final String SOCIETY = "સોસાયટી";
    private static final String MF = "મફત નગરી";
    private static final String REMAIN = "બાકીનો વિસ્તાર";
    private static final String TANK_FILL = "ટાંકી ભરાઈ રહી છે";

    /* Seed date from Python */
    private static final Calendar SEED;
    static {
        SEED = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        SEED.set(2024, Calendar.JANUARY, 1, 0, 0, 0);
        SEED.set(Calendar.MILLISECOND, 0);
    }

    /* ========= MODELS ========= */

    public static final class Slot {
        public final String time;
        public final String area;

        public Slot(String t, String a) {
            time = t;
            area = a;
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

    /* ========= PUBLIC API ========= */

    public static DaySchedule today(boolean tankFillToday, boolean forceSwap) {
        Calendar c = todayDate();
        return buildForDay("આજે", c, tankFillToday, forceSwap);
    }

    public static DaySchedule tomorrow(boolean forceSwap) {
        Calendar c = todayDate();
        c.add(Calendar.DAY_OF_MONTH, 1);
        return buildForDay("આવતીકાલે", c, false, forceSwap);
    }

    /* ========= CORE LOGIC (PORTED FROM PYTHON) ========= */

    private static DaySchedule buildForDay(
            String title,
            Calendar day,
            boolean tankFill,
            boolean forceSwap
    ) {

        String source = dailySource(day);

        boolean firstHalf = day.get(Calendar.DAY_OF_MONTH) <= 15;

        String firstPartner = partnerFirst(day, forceSwap);
        String secondPartner = firstPartner.equals(SOCIETY) ? MF : SOCIETY;

        List<Slot> slots = new ArrayList<Slot>();

        if (firstHalf) {
            if (tankFill) {
                slots.add(new Slot("06:00 – 09:00", REMAIN));
                slots.add(new Slot("09:00 – 11:00", TANK_FILL));
                slots.add(new Slot("11:00 – 12:30", YADAV + " + " + firstPartner));
                slots.add(new Slot("12:30 – 14:00", secondPartner));
            } else {
                slots.add(new Slot("06:00 – 09:00", REMAIN));
                slots.add(new Slot("09:00 – 10:30", firstPartner));
                slots.add(new Slot("10:30 – 12:00", secondPartner));
                slots.add(new Slot("12:00 – 13:30", YADAV));
            }
        } else {
            slots.add(new Slot("06:00 – 07:30", YADAV + " + " + firstPartner));
            slots.add(new Slot("07:30 – 09:00", secondPartner));
            slots.add(new Slot("09:00 – 12:00", REMAIN));
        }

        return new DaySchedule(title, source, slots);
    }

    /* ========= HELPERS ========= */

    private static Calendar todayDate() {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    private static String dailySource(Calendar day) {
        long diff =
            (day.getTimeInMillis() - SEED.getTimeInMillis())
            / (24L * 60L * 60L * 1000L);

        return (diff % 2 == 0) ? "બોરવેલ" : "નર્મદા";
    }

    private static String partnerFirst(Calendar day, boolean forceSwap) {
        long diff =
            (day.getTimeInMillis() - SEED.getTimeInMillis())
            / (24L * 60L * 60L * 1000L);

        String base = (diff % 2 == 0) ? SOCIETY : MF;
        if (forceSwap) {
            return base.equals(SOCIETY) ? MF : SOCIETY;
        }
        return base;
    }
}
