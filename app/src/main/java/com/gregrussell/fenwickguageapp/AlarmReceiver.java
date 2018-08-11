package com.gregrussell.fenwickguageapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

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

    Context mContext;
    PendingResult pendingResult;

    @Override
    public void onReceive(Context context, Intent intent) {

        mContext = context;

        Log.i("receivedBroadcast","broadcast received");
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent myIntent = new Intent(context, AlarmReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, myIntent, 0);
        myIntent.setAction("com.gregrussell.alarmtest.SEND_BROADCAST");

        if(Build.VERSION.SDK_INT >= 23) {
            alarmMgr.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + Constants.FIFTEEN_MINUTES_MILLIS, alarmIntent);
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            alarmMgr.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + Constants.FIFTEEN_MINUTES_MILLIS, alarmIntent);
        }else{
            Random random = new Random();
            int randomTimeMillis = random.nextInt(Constants.UPPER_BOUND_MILLIS - Constants.LOWER_BOUND_MILLIS) + Constants.LOWER_BOUND_MILLIS;
            alarmMgr.set(AlarmManager.ELAPSED_REALTIME,SystemClock.elapsedRealtime() + randomTimeMillis,alarmIntent);
        }

        pendingResult = goAsync();
        BackgroundTask task = new BackgroundTask();
        task.execute();



    }


    private class BackgroundTask extends AsyncTask<Void,Void,List<FloodedGauge>> {

        @Override
        protected List<FloodedGauge> doInBackground(Void... params){

            Log.i("receivedBroadcast","async task running");
            List<Gauge> faveGaugeList = getAllFavorites();
            List<GaugeReadParseObject> gaugeReadParseObjectList = new ArrayList<GaugeReadParseObject>();
            if(faveGaugeList != null && faveGaugeList.size() > 0){
                for(int i = 0; i < faveGaugeList.size();i++) {
                    gaugeReadParseObjectList.add(getGaugeReading(faveGaugeList.get(i).getGaugeID()));
                }
            }

            if(gaugeReadParseObjectList !=null && gaugeReadParseObjectList.size() > 0){
                pendingResult.finish();
                return floodedGauges(gaugeReadParseObjectList,faveGaugeList);
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

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext, HomeFragmentActivity.CHANNEL_ID)
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

        private List<FloodedGauge> floodedGauges(List<GaugeReadParseObject> list, List<Gauge> faveGaugeList){

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
                           list.get(i).getDatumList().get(0).getPrimary() + mContext.getResources().getString(R.string.feet_unit),convertToDate(list.get(i).getDatumList().get(0).getValid()),
                           floodWarning);
                   floodList.add(floodedGauge);

                }

            }
            return floodList;

        }

        private String convertToDate(String valid){

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


        }

        private int getFloodWarning(Sigstages sigstages, String waterHeight){


            double minorDouble = 0.0;
            double majorDouble = 0.0;
            double moderateDouble = 0.0;
            double waterDouble = 0.0;

            if(sigstages == null){
                return 0;
            }
            if(sigstages.getMajor() == null && sigstages.getModerate() == null && sigstages.getFlood() == null){
                return 0;
            }else {

                try {
                    waterDouble = Double.parseDouble(waterHeight);

                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }

                if(sigstages.getMajor() != null ){

                    try {
                        majorDouble = Double.parseDouble(sigstages.getMajor());
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }

                    if(waterDouble >= majorDouble){
                        return 3;
                    }
                }

                if(sigstages.getModerate() !=null){
                    try{
                        moderateDouble = Double.parseDouble(sigstages.getModerate());
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }
                    if(waterDouble >= moderateDouble){
                        return 2;
                    }
                }
                if(sigstages.getFlood() !=null){

                    try{
                        minorDouble = Double.parseDouble(sigstages.getFlood());
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }
                    if(waterDouble >= minorDouble){
                        return 1;
                    }
                }


            }
            return 0;

        }

        private List<Gauge> getAllFavorites(){
            DataBaseHelperGauges myDBHelper = new DataBaseHelperGauges(mContext);
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
            return myDBHelper.getNotifiableFavorites();

        }

        private GaugeReadParseObject getGaugeReading(String gaugeID){

            GaugeData gaugeData = new GaugeData(gaugeID);
            return gaugeData.getData();


        }



    }
    private class FloodedGauge{
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
