package com.example.chris.fstest;

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
        
        Intent snoozeIntent = new Intent(context, NotificationReceiver.class);
        snoozeIntent.setAction("SNOOZE");
        snoozeIntent.putExtra("type", "morning");
        PendingIntent snoozePi = PendingIntent.getBroadcast(context, 1001, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        Intent doneIntent = new Intent(context, NotificationReceiver.class);
        doneIntent.setAction("DONE");
        doneIntent.putExtra("notif_id", 1000);
        PendingIntent donePi = PendingIntent.getBroadcast(context, 1002, doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("💧 આજે પાણીનો સ્ત્રોત")
            .setContentText(sourceName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_media_pause, "⏰ Snooze (5m)", snoozePi)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "✓ Done", donePi);
        
        if (nm != null) nm.notify(1000, builder.build());
    }
    
    private void showAreaNotification(Context context, NotificationManager nm, String area, String time, int notifId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int snoozeCount = prefs.getInt(KEY_SNOOZE_COUNT + notifId, 0);
        
        Intent snoozeIntent = new Intent(context, NotificationReceiver.class);
        snoozeIntent.setAction("SNOOZE");
        snoozeIntent.putExtra("notif_id", notifId);
        snoozeIntent.putExtra("area", area);
        snoozeIntent.putExtra("time", time);
        PendingIntent snoozePi = PendingIntent.getBroadcast(context, notifId + 3000, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        Intent doneIntent = new Intent(context, NotificationReceiver.class);
        doneIntent.setAction("DONE");
        doneIntent.putExtra("notif_id", notifId);
        PendingIntent donePi = PendingIntent.getBroadcast(context, notifId + 4000, doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        String content = area + " માટે પાણી " + time + " વાગે શરૂ થશે";
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("💧 પાણી સમય સૂચના")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true);
        
        if (snoozeCount < 3) {
            builder.addAction(android.R.drawable.ic_media_pause, "⏰ Snooze (5m)", snoozePi);
        }
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "✓ Done", donePi);
        
        if (nm != null) nm.notify(notifId, builder.build());
    }
    
    private void handleSnooze(Context context, Intent intent) {
        int notifId = intent.getIntExtra("notif_id", 1000);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int snoozeCount = prefs.getInt(KEY_SNOOZE_COUNT + notifId, 0);
        
        if (snoozeCount >= 3) return;
        
        prefs.edit().putInt(KEY_SNOOZE_COUNT + notifId, snoozeCount + 1).apply();
        
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(notifId);
        
        android.app.AlarmManager am = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent newIntent = new Intent(context, NotificationReceiver.class);
        
        if (notifId == 1000) {
            newIntent.setAction("MORNING_ALERT");
        } else {
            newIntent.setAction("AREA_ALERT");
            newIntent.putExtra("area", intent.getStringExtra("area"));
            newIntent.putExtra("time", intent.getStringExtra("time"));
            newIntent.putExtra("notif_id", notifId);
        }
        
        PendingIntent pi = PendingIntent.getBroadcast(context, notifId, newIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        long triggerAt = System.currentTimeMillis() + (5 * 60 * 1000);
        
        if (Build.VERSION.SDK_INT >= 23) {
            am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else {
            am.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }
    
    private void scheduleNextMorning(Context context) {
        android.app.AlarmManager am = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction("MORNING_ALERT");
        
        PendingIntent pi = PendingIntent.getBroadcast(context, 1000, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 7);
        cal.set(Calendar.MINUTE, 45);
        cal.set(Calendar.SECOND, 0);
        
        if (Build.VERSION.SDK_INT >= 23) {
            am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        } else {
            am.setExact(android.app.AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        }
    }
                }
            
