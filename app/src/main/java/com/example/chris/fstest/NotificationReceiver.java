package com.example.chris.fstest;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import java.util.Calendar;

public class NotificationReceiver extends BroadcastReceiver {
    
    private static final String PREFS_NAME = "TankSchedulePrefs";
    private static final String KEY_SNOOZE_COUNT = "snooze_count_";
    private static final String CHANNEL_ID = "water_schedule_channel";
    private static final String CHANNEL_NAME = "Water Schedule Alerts";
    private static final int MAX_SNOOZE = 2;
    private static final int SNOOZE_MINUTES = 5;
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;
        
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannel(nm);
        
        if (action.equals("MORNING_ALERT")) {
            showMorningNotification(context, nm);
            scheduleNextMorning(context);
        } else if (action.equals("AREA_ALERT")) {
            String area = intent.getStringExtra("area");
            String time = intent.getStringExtra("time");
            int notifId = intent.getIntExtra("notif_id", 2000);
            showAreaNotification(context, nm, area, time, notifId);
        } else if (action.equals("SNOOZE")) {
            handleSnooze(context, intent);
        } else if (action.equals("DONE")) {
            int notifId = intent.getIntExtra("notif_id", 0);
            if (nm != null) nm.cancel(notifId);
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putInt(KEY_SNOOZE_COUNT + notifId, 0).apply();
        } else if (action.equals("TEST_NOTIFICATION")) {
            int testType = intent.getIntExtra("test_type", 0);
            showTestNotification(context, nm, testType);
        }
    }
    
    private void createChannel(NotificationManager nm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm != null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, 
                NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Water schedule alerts");
            nm.createNotificationChannel(channel);
        }
    }
    
    private void showMorningNotification(Context context, NotificationManager nm) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int sourcePref = prefs.getInt("morning_source_pref", 0);
        int snoozeCount = prefs.getInt(KEY_SNOOZE_COUNT + 1000, 0);
        
        Calendar cal = Calendar.getInstance();
        Calendar seed = Calendar.getInstance();
        seed.set(2025, Calendar.AUGUST, 29);
        long days = (cal.getTimeInMillis() - seed.getTimeInMillis()) / (1000*60*60*24);
        boolean isBorewell = (days % 2 == 0);
        
        boolean shouldNotify = false;
        String sourceName = isBorewell ? "બોરવેલ (Borewell)" : "નર્મદા (Narmada)";
        
        if (sourcePref == 0) shouldNotify = true;
        else if (sourcePref == 1 && !isBorewell) shouldNotify = true;
        else if (sourcePref == 2 && isBorewell) shouldNotify = true;
        
        if (!shouldNotify) return;
        
        // Add reminder count text
        String reminderText = "";
        if (snoozeCount == 1) {
            reminderText = " (બીજી વાર યાદ કરાવે છે)";
        } else if (snoozeCount == 2) {
            reminderText = " (છેલ્લી વાર યાદ કરાવે છે)";
        }
        
        Intent snoozeIntent = new Intent(context, NotificationReceiver.class);
        snoozeIntent.setAction("SNOOZE");
        snoozeIntent.putExtra("type", "morning");
        snoozeIntent.putExtra("notif_id", 1000);
        
        Intent doneIntent = new Intent(context, NotificationReceiver.class);
        doneIntent.setAction("DONE");
        doneIntent.putExtra("notif_id", 1000);
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        PendingIntent snoozePi = PendingIntent.getBroadcast(context, 1001, snoozeIntent, flags);
        PendingIntent donePi = PendingIntent.getBroadcast(context, 1002, doneIntent, flags);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("આજનો પાણીનો સ્રોત / Today's Water Source" + reminderText)
            .setContentText(sourceName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Done", donePi);
        
        // Only show snooze button if less than 2 snoozes used
        if (snoozeCount < MAX_SNOOZE) {
            builder.addAction(android.R.drawable.ic_lock_idle_alarm, 
                "Snooze " + SNOOZE_MINUTES + " min", snoozePi);
        }
        
        if (nm != null) nm.notify(1000, builder.build());
    }
    
    private void showAreaNotification(Context context, NotificationManager nm, String area, String time, int notifId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int snoozeCount = prefs.getInt(KEY_SNOOZE_COUNT + notifId, 0);
        
        // Add reminder count text
        String reminderText = "";
        if (snoozeCount == 1) {
            reminderText = " (બીજી વાર યાદ કરાવે છે)";
        } else if (snoozeCount == 2) {
            reminderText = " (છેલ્લી વાર યાદ કરાવે છે)";
        }
        
        Intent snoozeIntent = new Intent(context, NotificationReceiver.class);
        snoozeIntent.setAction("SNOOZE");
        snoozeIntent.putExtra("type", "area");
        snoozeIntent.putExtra("notif_id", notifId);
        snoozeIntent.putExtra("area", area);
        snoozeIntent.putExtra("time", time);
        
        Intent doneIntent = new Intent(context, NotificationReceiver.class);
        doneIntent.setAction("DONE");
        doneIntent.putExtra("notif_id", notifId);
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        PendingIntent snoozePi = PendingIntent.getBroadcast(context, notifId + 100, snoozeIntent, flags);
        PendingIntent donePi = PendingIntent.getBroadcast(context, notifId + 200, doneIntent, flags);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("પાણી સપ્લાય ટૂંક સમયમાં / Water Supply Soon" + reminderText)
            .setContentText(area + " - " + time)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Done", donePi);
        
        // Only show snooze button if less than 2 snoozes used
        if (snoozeCount < MAX_SNOOZE) {
            builder.addAction(android.R.drawable.ic_lock_idle_alarm, 
                "Snooze " + SNOOZE_MINUTES + " min", snoozePi);
        }
        
        if (nm != null) nm.notify(notifId, builder.build());
    }
    
    private void showTestNotification(Context context, NotificationManager nm, int testType) {
        // Show exact same notification as real alerts, just triggered via test
        if (testType == 0) {
            showMorningNotification(context, nm);
        } else {
            String[] areas = {
                "યાદવ નગરી + ચૌધરી ફરીયો",
                "મફત નગરી",
                "સોસાયટી",
                "બાકીનો વિસ્તાર"
            };
            String[] times = {"12:00-13:30", "10:30-12:00", "09:00-10:30", "06:00-09:00"};
            
            int idx = testType - 1;
            if (idx >= 0 && idx < areas.length) {
                showAreaNotification(context, nm, areas[idx], times[idx], 2000 + idx);
            }
        }
    }
    
    private void handleSnooze(Context context, Intent intent) {
        String type = intent.getStringExtra("type");
        int notifId = intent.getIntExtra("notif_id", 0);
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int count = prefs.getInt(KEY_SNOOZE_COUNT + notifId, 0);
        
        // If already max snoozes, just cancel and reset
        if (count >= MAX_SNOOZE) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(notifId);
            prefs.edit().putInt(KEY_SNOOZE_COUNT + notifId, 0).apply();
            return;
        }
        
        // Increment snooze count
        prefs.edit().putInt(KEY_SNOOZE_COUNT + notifId, count + 1).apply();
        
        // Schedule next alarm for 5 minutes later
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alertIntent = new Intent(context, NotificationReceiver.class);
        
        if (type.equals("morning")) {
            alertIntent.setAction("MORNING_ALERT");
        } else {
            alertIntent.setAction("AREA_ALERT");
            alertIntent.putExtra("area", intent.getStringExtra("area"));
            alertIntent.putExtra("time", intent.getStringExtra("time"));
            alertIntent.putExtra("notif_id", notifId);
        }
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getBroadcast(context, notifId + 500, alertIntent, flags);
        
        long triggerTime = System.currentTimeMillis() + (SNOOZE_MINUTES * 60 * 1000);
        
        if (am != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pi);
            }
        }
        
        // Cancel current notification
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(notifId);
    }
    
    private void scheduleNextMorning(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean("morning_alert_enabled", false)) return;
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 7);
        cal.set(Calendar.MINUTE, 45);
        cal.set(Calendar.SECOND, 0);
        
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction("MORNING_ALERT");
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getBroadcast(context, 1000, intent, flags);
        
        if (am != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            }
        }
    }
}
