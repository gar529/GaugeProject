package com.gregrussell.fenwickguageapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import java.util.Random;

public class StartAlarmAtBoot extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent){

        Log.d("broadcast",intent.getAction());

        if(intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {

            AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent myIntent = new Intent(context, AlarmReceiver.class);
            PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, myIntent, 0);
            myIntent.setAction("com.gregrussell.alarmtest.SEND_BROADCAST");



            if(Build.VERSION.SDK_INT >= 23) {
                alarmMgr.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + GaugeApplication.FIFTEEN_MINUTES_MILLIS, alarmIntent);
            }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                alarmMgr.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + GaugeApplication.FIFTEEN_MINUTES_MILLIS, alarmIntent);
            }else{
                Random random = new Random();
                int randomTimeMillis = random.nextInt(GaugeApplication.UPPER_BOUND_MILLIS - GaugeApplication.LOWER_BOUND_MILLIS) + GaugeApplication.LOWER_BOUND_MILLIS;
                alarmMgr.set(AlarmManager.ELAPSED_REALTIME,SystemClock.elapsedRealtime() + randomTimeMillis,alarmIntent);
            }
        }
    }

}
