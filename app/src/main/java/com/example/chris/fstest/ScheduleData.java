package com.example.chris.fstest;

import java.util.Arrays;
import java.util.List;

public class ScheduleData {

    public static class Slot {
        public final String time;
        public final String area;
        public Slot(String t, String a) {
            time = t;
            area = a;
        }
    }

    public static class DaySchedule {
        public final String title;
        public final String source;
        public final List<Slot> slots;

        public DaySchedule(String t, String s, List<Slot> sl) {
            title = t;
            source = s;
            slots = sl;
        }
    }

    public static DaySchedule today(boolean tankFill, boolean mfSwap) {
        String source = "નર્મદા";

        String s1 = tankFill
                ? "ટાંકી ભરાવ"
                : (mfSwap
                    ? "યાદવ નગર + ચૌધરી ફેરીયો + મહેત નગર"
                    : "યાદવ નગર + ચૌધરી ફેરીયો + સોસાયટી");

        String s2 = mfSwap ? "સોસાયટી" : "મહેત નગર";

        List<Slot> slots = Arrays.asList(
                new Slot("06:00 - 07:30", s1),
                new Slot("07:30 - 09:00", s2),
                new Slot("09:00 - 12:00", "બાકીનો વિસ્તાર")
        );

        return new DaySchedule("આજે", source, slots);
    }

    public static DaySchedule tomorrow(boolean mfSwap) {
        String source = "નર્મદા";

        String s1 = mfSwap
                ? "યાદવ નગર + ચૌધરી ફેરીયો + મહેત નગર"
                : "યાદવ નગર + ચૌધરી ફેરીયો + સોસાયટી";

        String s2 = mfSwap ? "સોસાયટી" : "મહેત નગર";

        List<Slot> slots = Arrays.asList(
                new Slot("06:00 - 07:30", s1),
                new Slot("07:30 - 09:00", s2),
                new Slot("09:00 - 12:00", "બાકીનો વિસ્તાર")
        );

        return new DaySchedule("આવતીકાલે", source, slots);
    }
}
