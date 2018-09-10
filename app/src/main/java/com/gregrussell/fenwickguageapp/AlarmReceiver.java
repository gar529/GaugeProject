package com.gregrussell.fenwickguageapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

/**
 * The AlarmReceiver class is used to trigger notification updates. Updates are triggered by an
 * AlarmManager approximately every 15 minutes. During each update, the class checks for any
 * gauges that have notifications enabled and checks their stage from their NWS AHPS RSS feed.
 * If flood stage has been reached for one or more gauges, a notification is displayed on the device.
 * The AlarmReceiver class is also used as an opportunity update the device's location
 */
public class AlarmReceiver extends BroadcastReceiver {

    Context mContext;
    static PendingResult pendingResult;

    /**
     * Code that runs when the BroadcastReceiver is receiving an Intent broadcast
     * @param context The context in which the receiver is running
     * @param intent The intent being received
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        mContext = context;
        Log.i("receivedBroadcast","broadcast received");
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent myIntent = new Intent(context, AlarmReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, myIntent, 0);
        myIntent.setAction("com.gregrussell.alarmtest.SEND_BROADCAST");

        //different behavior for different SDKs. >= 23 AlarmManager.set() does not run while idle,
        //must use AlarmManager.setAndAllowWhileIdle
        //KitKat to 23, AlarmManager.set() default behavior is to run while idle
        //Before KitKat, AlarmManager.set() does not automatically randomize the intervals
        if(Build.VERSION.SDK_INT >= 23) {
            alarmMgr.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + GaugeApplication.FIFTEEN_MINUTES_MILLIS, alarmIntent);
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            alarmMgr.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + GaugeApplication.FIFTEEN_MINUTES_MILLIS, alarmIntent);
        }else{
            Random random = new Random();
            int randomTimeMillis = random.nextInt(GaugeApplication.UPPER_BOUND_MILLIS - GaugeApplication.LOWER_BOUND_MILLIS) + GaugeApplication.LOWER_BOUND_MILLIS;
            alarmMgr.set(AlarmManager.ELAPSED_REALTIME,SystemClock.elapsedRealtime() + randomTimeMillis,alarmIntent);
        }
        pendingResult = goAsync();
        LocationUpdate locationUpdate = new LocationUpdate(context, new LocCallback() {
            @Override
            public void callback(Location location) {
                //just need to update and store the lastKnownLocation, so don't need to handle it
                //when it is received
            }
        });
        locationUpdate.getCurrentLocation();
        BackgroundTask task = new BackgroundTask();
        task.execute(mContext);
    }

    /**
     * Class that holds objects needed for BackgroundTask
     */
    private static class BackgroundTaskParam{
        Context mContext;
        List<FloodedGauge> floodedGauges;

        /**
         * Class used for creating a BackgroundTaskParam
         * @param mContext Activity context
         * @param floodedGauges List of FloodedGauges
         */
        private BackgroundTaskParam(Context mContext, List<FloodedGauge>floodedGauges){
            this.mContext = mContext;
            this.floodedGauges = floodedGauges;
        }
    }


    /**
     * BackgroundTask runs on a background thread to check flood stages and display notifications if
     * necessary
     */
    private static class BackgroundTask extends AsyncTask<Context,Void,BackgroundTaskParam> {

        @Override
        protected BackgroundTaskParam doInBackground(Context... params){

            Log.i("receivedBroadcast","async task running");
            Context mContext = params[0];
            List<Gauge> faveGaugeList = getAllFavorites();
            List<RSSParsedObj> rssParsedObjList = new ArrayList<>();

            if(faveGaugeList != null && !faveGaugeList.isEmpty()) {
                for(Gauge gauge : faveGaugeList){
                    rssParsedObjList.add(getRssReading(gauge.getGaugeID()));
                }
            }

            if(!rssParsedObjList.isEmpty()){
                pendingResult.finish();
                return new BackgroundTaskParam(mContext,floodedGauges(mContext,rssParsedObjList,faveGaugeList));
            }


            pendingResult.finish();
            return null;
        }

        @Override
        protected void onPostExecute(BackgroundTaskParam result){

            List<FloodedGauge> list = result.floodedGauges;
            Context mContext = result.mContext;

            if(list != null && list.size() > 0) {

                if(list.size() == 1){

                    singleGaugeNotification(mContext,list.get(0));
                }else{
                    multiGaugeNotification(mContext);
                }
            }
            Log.i("receivedBroadcast", "async finished");



        }

        private void multiGaugeNotification(Context mContext){

            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            CharSequence title = mContext.getResources().getString(R.string.flood_warning);
            CharSequence text = mContext.getResources().getString(R.string.multi_flood_warning);
            Intent intent = new Intent(mContext, MainFragActivity.class);
            intent.putExtra("notification","notification");
            PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext, GaugeApplication.CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setAutoCancel(true)
                    .setSound(soundUri)
                    .setContentIntent(pendingIntent)
                    .setVibrate(new long[]{1000, 1000, 1000});
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);
            notificationManager.notify(0, mBuilder.build());
        }

        private void singleGaugeNotification(Context mContext,FloodedGauge floodedGauge){

            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            CharSequence title;
            String floodWarning = "";
            int status = floodedGauge.status;
            switch (status){
                case 0:
                    title = mContext.getResources().getString(R.string.action_warning);
                    floodWarning = mContext.getResources().getString(R.string.action_flooding);
                    break;
                case 1:
                    title = mContext.getResources().getString(R.string.minor_warning);
                    floodWarning = mContext.getResources().getString(R.string.minor_flooding);
                    break;
                case 2:
                    title = mContext.getResources().getString(R.string.moderate_warning);
                    floodWarning = mContext.getResources().getString(R.string.moderate_flooding);
                    break;
                case 3:
                    title = mContext.getResources().getString(R.string.major_warning);
                    floodWarning = mContext.getResources().getString(R.string.major_flooding);
                    break;
                default:
                    title = mContext.getResources().getString(R.string.flood_warning);
                    floodWarning = "";
                    break;
            }

            CharSequence text = floodedGauge.name + " reached a stage of " + floodedGauge.stage + " at " + floodedGauge.time + "." + floodWarning;

            Intent intent = new Intent(mContext, MainFragActivity.class);
            intent.putExtra("notification","notification");
            PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext, GaugeApplication.CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setAutoCancel(true)
                    .setSound(soundUri)
                    .setContentIntent(pendingIntent)
                    .setVibrate(new long[]{1000, 1000, 1000});
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);
            notificationManager.notify(0, mBuilder.build());


        }

        private List<FloodedGauge> floodedGauges(Context mContext,List<RSSParsedObj> list, List<Gauge> faveGaugeList){

            List<FloodedGauge> floodList = new ArrayList<FloodedGauge>();

            Log.d("floodedGauge","grpo list size:" + list.size() + " |  fave list size" + faveGaugeList.size());

            for(int i =0; i< list.size();i++){

                RSSParsedObj rssParsedObj = list.get(i);
                String stage = "";
                if(rssParsedObj.getStage() != null && !rssParsedObj.getStage().equals("")){
                    stage = rssParsedObj.getStage();
                }
                Sigstages sigstages = new Sigstages(rssParsedObj.getAction(),rssParsedObj.getMinor(),
                        rssParsedObj.getModerate(),rssParsedObj.getMajor());
                int floodWarning = getFloodWarning(sigstages,stage);
                if(floodWarning > -1){
                    FloodedGauge floodedGauge = new FloodedGauge(faveGaugeList.get(i).getGaugeName(),
                            addUnits(mContext,rssParsedObj.getStage()),convertToDate(rssParsedObj.getTime()),floodWarning);
                    floodList.add(floodedGauge);
                }

            }

            return floodList;

        }

        private String addUnits(Context mContext,String waterHeight){

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
            String unitsPref = sharedPref.getString(SettingsFragment.KEY_PREF_UNITS, "0");

            int unit = Integer.parseInt(unitsPref);

            switch (unit){
                case GaugeApplication.FEET:
                    return convertToFeet(mContext, waterHeight);
                case GaugeApplication.METERS:
                    return convertToMeters(mContext, waterHeight);
                default:
                    return "";
            }



        }

        private String convertToFeet(Context mContext, String waterHeight){

            double feetDouble = Double.parseDouble(waterHeight);
            return String.format(Locale.getDefault(),"%.2f",feetDouble) + mContext.getResources().getString(R.string.feet_unit);
        }

        private String convertToMeters(Context mContext, String waterHeight){

            double meterConverter = .3048;
            double meterDouble = Double.parseDouble(waterHeight) * meterConverter;
            return String.format(Locale.getDefault(),"%.2f",meterDouble) +
                    mContext.getResources().getString(R.string.meter_unit);



        }




        private String convertToDate(String dateString){

            if(dateString.equals("")){
                return "";
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy h:mm a");
            Date convertedDate = new Date();
            try{
                convertedDate = dateFormat.parse(dateString);
            }catch (ParseException e){

            }catch (NullPointerException e){

            }
            SimpleDateFormat myFormat = new SimpleDateFormat("MMM dd h:mma");
            TimeZone tz = TimeZone.getDefault();
            Date now = new Date();
            int offsetFromUtc = tz.getOffset(now.getTime());



            return myFormat.format(convertedDate);


        }

        private int getFloodWarning(Sigstages sigstages, String waterHeight){

            double actionDouble;
            double minorDouble;
            double majorDouble;
            double moderateDouble;
            double waterDouble = 0.0;

            if(sigstages == null){
                return -1;
            }
            if(sigstages.getMajor() == null && sigstages.getModerate() == null && sigstages.getFlood() == null && sigstages.getAction() == null){
                return -1;
            }else {

                try {
                    waterDouble = Double.parseDouble(waterHeight);

                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }

                if(sigstages.getMajor() != null ){

                    try {
                        majorDouble = Double.parseDouble(sigstages.getMajor());
                        if(waterDouble >= majorDouble){
                            return 3;
                        }
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }


                }

                if(sigstages.getModerate() !=null){
                    try{
                        moderateDouble = Double.parseDouble(sigstages.getModerate());
                        if(waterDouble >= moderateDouble){
                            return 2;
                        }
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }

                }
                if(sigstages.getFlood() !=null){

                    try{
                        minorDouble = Double.parseDouble(sigstages.getFlood());
                        if(waterDouble >= minorDouble){
                            return 1;
                        }
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }

                }
                if(sigstages.getAction() !=null){
                    try{
                        actionDouble = Double.parseDouble(sigstages.getAction());
                        if(waterDouble >= actionDouble){
                            Log.d("alarmReceiver", "stage is " + waterDouble + " action is " + actionDouble);
                            return 0;
                        }
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }

                }
            }
            return -1;

        }

        private List<Gauge> getAllFavorites(){

            return GaugeApplication.myDBHelper.getNotifiableFavorites();

        }

        private RSSParsedObj getRssReading(String gaugeID){

            GaugeData gaugeData = new GaugeData(gaugeID);
            return gaugeData.getRssData();
        }

    }

    /**
     * FloodedGauge object class. Holds the data needed to d
     */
    private static class FloodedGauge{
        String name,stage,time;
        int status;
        private FloodedGauge(String name, String stage, String time, int status){
            this.name = name;
            this.stage = stage;
            this.time = time;
            this.status = status;
        }
    }

}
