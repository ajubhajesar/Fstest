package com.example.chris.fstest;

import java.util.*;

public class ScheduleData {

    public static class Slot {
        public final String time;
        public final String area;
        public Slot(String time, String area) {
            this.time = time;
            this.area = area;
        }
    }

    public static class DaySchedule {
        public final String title;
        public final String source;
        public final List<Slot> slots;
        public DaySchedule(String title, String source, List<Slot> slots) {
            this.title = title;
            this.source = source;
            this.slots = slots;
        }
    }

    public static DaySchedule today(boolean forceSwap) {
        String source = forceSwap ? "નર્મદા" : "બોરવેલ";
        List<Slot> slots = Arrays.asList(
            new Slot("06:00 – 07:30", "યાદવ નગરી + ચૌધરી ફેરીયો + સોસાયટી"),
            new Slot("07:30 – 09:00", "મહત નગરી"),
            new Slot("09:00 – 12:00", "બાકીનો વિસ્તાર")
        );
        return new DaySchedule("આજે", source, slots);
    }

    public static DaySchedule tomorrow(boolean forceSwap) {
        String source = forceSwap ? "બોરવેલ" : "નર્મદા";
        List<Slot> slots = Arrays.asList(
            new Slot("06:00 – 07:30", "યાદવ નગરી + ચૌધરી ફેરીયો + મહત નગરી"),
            new Slot("07:30 – 09:00", "સોસાયટી"),
            new Slot("09:00 – 12:00", "બાકીનો વિસ્તાર")
        );
        return new DaySchedule("આવતીકાલે", source, slots);
    }
}
