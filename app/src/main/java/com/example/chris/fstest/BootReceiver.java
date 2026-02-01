package com.example.chris.fstest;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    
    private static final String PREFS_NAME = "TankSchedulePrefs";
    private static final int REQ_CODE_MORNING = 1000;
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            
            // Reschedule morning alert if enabled
            if (prefs.getBoolean("morning_alert_enabled", false)) {
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                Intent alarmIntent = new Intent(context, NotificationReceiver.class);
                alarmIntent.setAction("MORNING_ALERT");
                
                PendingIntent pi = PendingIntent.getBroadcast(context, REQ_CODE_MORNING, alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(java.util.Calendar.HOUR_OF_DAY, 7);
                cal.set(java.util.Calendar.MINUTE, 45);
                cal.set(java.util.Calendar.SECOND, 0);
                
                if (cal.getTimeInMillis() < System.currentTimeMillis()) {
                    cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
                }
                
                if (am != null) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                    } else {
                        am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                    }
                }
            }
            
            // Note: Area alerts are not rescheduled on boot because they were for specific times today
            // They will be recalculated when user opens the app again (or you could store them and reschedule)
        }
    }
}
