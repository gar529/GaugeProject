package com.gregrussell.fenwickguageapp;

import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Random;

public class GaugeApplication extends Application {

    public static DataBaseHelperGauges myDBHelper;
    public static final String CHANNEL_ID = "Flood Warning";
    public static final int GAUGES_ID_POSITION = 0;
    public static final int GAUGES_IDENTIFIER_POSITION = 1;
    public static final int GAUGES_NAME_POSITION = 2;
    public static final int GAUGES_URL_POSITION = 3;
    public static final int GAUGES_LATITUDE_POSITION = 4;
    public static final int GAUGES_LONGITUDE_POSITION = 5;
    public static final int GAUGES_ADDRESS_POSITION = 6;
    public static final int GAUGES_ACTIVE_POSITION = 7;
    public static final int GAUGES_VERSION_POSITION = 8;
    public static final int GAUGES_TIMESTAMP_POSITION = 9;

    public static final int FAVORITES_ID_POSITION = 0;
    public static final int FAVORITES_IDENTIFIER_POSITION = 1;
    public static final int FAVORITES_NOTIFICATION_POSITION = 2;
    public static final int FAVORITES_ACTIVE_POSITION = 3;
    public static final int FAVORITES_TIMESTAMP_POSITION = 4;

    public static final int MARKERS_ID_POSITION = 0;
    public static final int MARKERS_NAME_POSITION = 1;

    public static final int FIFTEEN_MINUTES_MILLIS = 1000 * 60 * 15;
    public static final int LOWER_BOUND_MILLIS = 1000 * 60 * 12;
    public static final int UPPER_BOUND_MILLIS = 1000 * 60 * 17;

    public static final int FEET = 0;
    public static final int METERS = 1;




    @Override
    public void onCreate() {
        super.onCreate();

        myDBHelper = new DataBaseHelperGauges(this);
        try{
            myDBHelper.createDataBase();

        }catch (IOException e){
            throw new Error("unable to create db");

        }
        try{
            myDBHelper.openDataBase();
        }catch (SQLException sqle){
            throw sqle;

        }

        logger();

        ComponentName receiver = new ComponentName(this, StartAlarmAtBoot.class);
        PackageManager pm = this.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        createNotificationChannel();

        AlarmManager alarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this,AlarmReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this,0,intent,0);
        intent.setAction("com.gregrussell.alarmtest.SEND_BROADCAST");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            alarmMgr.setWindow(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + FIFTEEN_MINUTES_MILLIS, UPPER_BOUND_MILLIS - FIFTEEN_MINUTES_MILLIS, alarmIntent);
        }else{
            Random random = new Random();
            int randomTimeMillis = random.nextInt(UPPER_BOUND_MILLIS - LOWER_BOUND_MILLIS) + LOWER_BOUND_MILLIS;
            alarmMgr.set(AlarmManager.ELAPSED_REALTIME,SystemClock.elapsedRealtime() + randomTimeMillis,alarmIntent);
        }


    }

    public void logger(){

        if ( isExternalStorageWritable() ) {

            Log.d("logger","logging");

            File appDirectory = new File( Environment.getExternalStorageDirectory() + "/GaugeProjectLogs" );
            File logDirectory = new File( appDirectory + "/log" );
            File logFile = new File( logDirectory, "logcat" + System.currentTimeMillis() + ".txt" );

            // create app folder
            if ( !appDirectory.exists() ) {
                appDirectory.mkdir();
            }

            // create log folder
            if ( !logDirectory.exists() ) {
                logDirectory.mkdir();
            }

            // clear the previous logcat and then write the new one to the file
            try {
                Process process = Runtime.getRuntime().exec("logcat -c");
                process = Runtime.getRuntime().exec("logcat -f " + logFile + " *:I");
            } catch ( IOException e ) {
                e.printStackTrace();
            }

        } else if ( isExternalStorageReadable() ) {
            // only readable
        } else {
            // not accessible
        }

    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals( state ) ) {
            return true;
        }
        return false;
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setDescription(description);
            channel.setVibrationPattern(new long[]{1000, 1000, 1000});
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }





}
