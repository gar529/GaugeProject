package com.gregrussell.fenwickguageapp;

import android.app.Application;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class GaugeApplication extends Application {

    public static DataBaseHelperGauges myDBHelper;
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
                process = Runtime.getRuntime().exec("logcat -f " + logFile);
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



}
