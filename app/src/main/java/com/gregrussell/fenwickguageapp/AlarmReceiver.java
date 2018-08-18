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

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

public class AlarmReceiver extends BroadcastReceiver {

    static Context mContext;
    static LocationCallback mLocationCallback;
    static PendingResult pendingResult;
    private static String units;

    @Override
    public void onReceive(Context context, Intent intent) {

        mContext = context;
        units = mContext.getResources().getString(R.string.feet_unit);
        Log.i("receivedBroadcast","broadcast received");





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

        pendingResult = goAsync();
        updateLocation();
        BackgroundTask task = new BackgroundTask();
        task.execute();



    }

    /**
     * Updates the device's current location
     * Stops requesting updates once a location is determined
     */
    private void updateLocation(){

        final FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5000);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.i("getLocationUpdate5","in onLocationResult");
                if (locationResult == null) {
                    Log.d("getLocationUpdate3","location result is null");
                    return;
                }else{
                    Log.i("getLocationUpdate4","location result is not null");
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    // ...
                    Log.i("getLocationUpdate",location.getLatitude() + ", " + location.getLongitude());
                    if(location!=null) {
                        Log.i("getLocationUpdate",location.getLatitude() + ", " + location.getLongitude());
                        stopLocationUpdates(mFusedLocationClient);
                    }
                }
            };
        };
        startLocationUpdates(mContext,mFusedLocationClient, mLocationRequest,mLocationCallback);


    }

    /**
     * Checks if location updates are permitted, then requests location updates in the interval
     * defined by mLocationRequest
     * @param context Activity context
     * @param mFusedLocationClient FusedLocationProviderClient used to initiate location requests
     */
    private static void startLocationUpdates(Context context,
                                             FusedLocationProviderClient mFusedLocationClient,
                                             LocationRequest mLocationRequest,
                                             LocationCallback mLocationCallback) {

        Log.i("getLocationUpdate","startLocation");
        if(ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback,
                    null /* Looper */);
            Log.i("getLocationUpdate2","getting location");
        }
    }

    /**
     * Stops the receiving of location updates
     * @param mFusedLocationClient FusedLocationProviderClient initiates stoppage of location updates
     */
    private static void stopLocationUpdates(FusedLocationProviderClient mFusedLocationClient) {
        Log.i("getLocationUpdate6","location updates stopped");
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }


    private static class BackgroundTask extends AsyncTask<Void,Void,List<FloodedGauge>> {

        @Override
        protected List<FloodedGauge> doInBackground(Void... params){

            Log.i("receivedBroadcast","async task running");
            List<Gauge> faveGaugeList = getAllFavorites();
            /*List<GaugeReadParseObject> gaugeReadParseObjectList = new ArrayList<GaugeReadParseObject>();
            if(faveGaugeList != null && faveGaugeList.size() > 0){
                for(int i = 0; i < faveGaugeList.size();i++) {
                    gaugeReadParseObjectList.add(getGaugeReading(faveGaugeList.get(i).getGaugeID()));
                }
            }

            if(gaugeReadParseObjectList !=null && gaugeReadParseObjectList.size() > 0){
                pendingResult.finish();
                return floodedGauges(gaugeReadParseObjectList,faveGaugeList);
            }*/

            List<RSSParsedObj> rssParsedObjList = new ArrayList<>();

            if(faveGaugeList != null && !faveGaugeList.isEmpty()) {
                for(Gauge gauge : faveGaugeList){
                    rssParsedObjList.add(getRssReading(gauge.getGaugeID()));
                }
            }

            if(!rssParsedObjList.isEmpty()){
                pendingResult.finish();
                return floodedGauges(rssParsedObjList,faveGaugeList);
            }


            pendingResult.finish();
            return null;
        }

        @Override
        protected void onPostExecute(List<FloodedGauge> result){

            if(result != null && result.size() > 0) {

                if(result.size() == 1){

                    singleGaugeNotification(result.get(0));
                }else{
                    multiGaugeNotification();
                }


            }
            stopLocationUpdates(LocationServices.getFusedLocationProviderClient(mContext));
            Log.i("receivedBroadcast", "async finished");



        }

        private void multiGaugeNotification(){

            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            CharSequence title = mContext.getResources().getString(R.string.flood_warning);
            CharSequence text = mContext.getResources().getString(R.string.multi_flood_warning);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext, GaugeApplication.CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setAutoCancel(true)
                    .setSound(soundUri)
                    .setVibrate(new long[]{1000, 1000, 1000});
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);
            notificationManager.notify(0, mBuilder.build());
        }

        private void singleGaugeNotification(FloodedGauge floodedGauge){



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

        /*private List<FloodedGauge> floodedGauges(List<GaugeReadParseObject> list, List<Gauge> faveGaugeList){

            List<FloodedGauge> floodList = new ArrayList<FloodedGauge>();

            Log.d("floodedGauge","grpo list size:" + list.size() + " |  fave list size" + faveGaugeList.size());
            for(int i =0; i< list.size();i++){

                String primary = "";
                if(list.get(i).getDatumList() != null) {
                    if (list.get(i).getDatumList().size() > 0) {
                        primary = list.get(i).getDatumList().get(0).getPrimary();
                    }
                }
                int floodWarning = getFloodWarning(list.get(i).getSigstages(),primary);
                if(floodWarning > 0){

                   FloodedGauge floodedGauge = new FloodedGauge(faveGaugeList.get(i).getGaugeName(),
                           list.get(i).getDatumList().get(0).getPrimary() + units,convertToDate(list.get(i).getDatumList().get(0).getValid()),
                           floodWarning);
                   floodList.add(floodedGauge);

                }

            }
            return floodList;

        }*/

        private List<FloodedGauge> floodedGauges(List<RSSParsedObj> list, List<Gauge> faveGaugeList){

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
                            addUnits(rssParsedObj.getStage()),convertToDate(rssParsedObj.getTime()),floodWarning);
                    floodList.add(floodedGauge);
                }

            }


            return floodList;

        }

        private String addUnits(String waterHeight){

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
            String unitsPref = sharedPref.getString(SettingsFragment.KEY_PREF_UNITS, "");

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
            return String.format("%.2f",feetDouble) + mContext.getResources().getString(R.string.feet_unit);
        }

        private String convertToMeters(Context mContext, String waterHeight){

            double meterConverter = .3048;
            double meterDouble = Double.parseDouble(waterHeight) * meterConverter;
            String meterString = String.valueOf(meterDouble);
            return String.format("%.2f",meterDouble) + mContext.getResources().getString(R.string.meter_unit);



        }

        /*private String convertToDate(String valid){

            if(valid.equals("")){
                return "";
            }
            String dateString = valid;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm");
            Date convertedDate = new Date();
            try {
                convertedDate = dateFormat.parse(dateString);


            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (NullPointerException e){
                e.printStackTrace();
            }
            Date date = Calendar.getInstance().getTime();
            DateFormat formatter = new SimpleDateFormat("h:mmaa");
            TimeZone tz = TimeZone.getDefault();
            Date now = new Date();
            int offsetFromUtc = tz.getOffset(now.getTime());
            Log.d("xmlData", "timezone offset is: " + offsetFromUtc);

            Log.d("xmlData", "date is: " + date.getTime());
            long offset = convertedDate.getTime() + offsetFromUtc;
            Log.d("xmlData", "date with offset is: " + offset);
            Date correctTZDate = new Date(offset);


            Log.d("xmlData", "correctTZDate is: " + correctTZDate.getTime());
            return formatter.format(correctTZDate);


        }*/


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

            long offset = convertedDate.getTime() + offsetFromUtc;

            Date correctTZDate = new Date(offset);

            return myFormat.format(convertedDate);


        }

        private int getFloodWarning(Sigstages sigstages, String waterHeight){

            double actionDouble = 0.0;
            double minorDouble = 0.0;
            double majorDouble = 0.0;
            double moderateDouble = 0.0;
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

        private GaugeReadParseObject getGaugeReading(String gaugeID){

            GaugeData gaugeData = new GaugeData(gaugeID);
            return gaugeData.getData();
        }

        private RSSParsedObj getRssReading(String gaugeID){

            GaugeData gaugeData = new GaugeData(gaugeID);
            return gaugeData.getRssData();
        }



    }
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
