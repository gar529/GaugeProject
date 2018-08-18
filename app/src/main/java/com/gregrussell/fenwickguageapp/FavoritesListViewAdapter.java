package com.gregrussell.fenwickguageapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class FavoritesListViewAdapter extends BaseAdapter {

    private Context mContext;
    private LayoutInflater inflater;
    private List<Gauge> gaugeList;
    private View currentView;



    public FavoritesListViewAdapter(Context mContext, List<Gauge> gaugeList){

        this.mContext = mContext;
        this.gaugeList = gaugeList;
        inflater = ((Activity)mContext).getLayoutInflater();


    }

    @Override
    public int getCount() {


        return gaugeList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View currentView = convertView;
        if(currentView == null){
            currentView = inflater.inflate(R.layout.favorite_list_layout,parent,false);
        }






        Log.d("FavoritesAdapter","gaugelist size " + gaugeList.size());
        Log.d("FavoritesAdapter2","current position " + position + ", gauge used " + gaugeList.get(position).getGaugeName());

        currentView.setTag(gaugeList.get(position));


        LoadFavoriteParams params = new LoadFavoriteParams(mContext,currentView,gaugeList.get(position));
        LoadFavorite task = new LoadFavorite();
        task.execute(params);




        Log.d("FavoritesAdapter3", "returning View");
        return currentView;
    }





    private class LoadFavoriteParams{
        Context context;
        View view;
        Gauge gauge;
        private LoadFavoriteParams(Context context, View view, Gauge gauge){
            this.context = context;
            this.view = view;
            this.gauge = gauge;
        }
    }


    private static class LoadFavorite extends AsyncTask<LoadFavoriteParams,Void,FavoriteParams>{



        @Override
        protected void onPreExecute(){

            Log.d("FavoritesAdapter4", "Async Start");



        }
        @Override
        protected FavoriteParams doInBackground(LoadFavoriteParams... position){


            Context context = position[0].context;
            View view = position[0].view;
            Log.d("FavoritesAdapter5", "Async DoInBackground");
            Gauge gauge = position[0].gauge;
            String gaugeName = gauge.getGaugeName();
            String gaugeAddress = gauge.getGaugeAddress();


            GaugeData gaugeData = new GaugeData(gauge.getGaugeID());
            //GaugeReadParseObject gaugeReadParseObject = gaugeData.getData();
            RSSParsedObj rssParsed = gaugeData.getRssData();

            //Datum datum = new Datum();
            String waterHeight = "";
            String primary =  "";
            //Log.d("favoriteS1",String.valueOf(gaugeReadParseObject));
            //Log.d("favoriteS2",String.valueOf(gaugeReadParseObject.getDatumList()));

            waterHeight = rssParsed.getStage();
            String date = convertRssDate(rssParsed.getTime());





            /*if(gaugeReadParseObject.getDatumList() != null) {
                if (gaugeReadParseObject.getDatumList().size() > 0) {
                    datum = gaugeReadParseObject.getDatumList().get(0);
                    primary = datum.getPrimary();
                    waterHeight = primary + context.getResources().getString(R.string.feet_unit);
                } else {
                    datum.setValid("");
                    datum.setPrimary("No Data");
                    waterHeight = datum.getPrimary();
                }
            }else{
                datum.setValid("");
                datum.setPrimary("No Data");
                waterHeight = datum.getPrimary();
            }
            String valid = datum.getValid();*/


            Sigstages sigstages = new Sigstages(rssParsed.getAction(),rssParsed.getMinor(),rssParsed.getModerate(),rssParsed.getMajor());
            //String floodWarning = getFloodWarning(context,gaugeReadParseObject.getSigstages(),primary);
            String floodWarning = getFloodWarning(context,sigstages,waterHeight);

            //String date = convertToDate(valid);

            Log.d("FavoritesAdapter14",waterHeight);

            int notification = GaugeApplication.myDBHelper.getFavoriteNotificationState(gauge);



            return new FavoriteParams(context,view,gaugeName,date,waterHeight,gaugeAddress,floodWarning, gauge, notification);
        }
        @Override
        protected void onPostExecute(final FavoriteParams params){

            View view = params.view;
            final Context context = params.context;
            RelativeLayout loadingPanel = (RelativeLayout)view.findViewById(R.id.favorite_list_loading_panel);
            TextView gaugeNameTextView = (TextView)view.findViewById(R.id.favorite_gauge_name);
            TextView gaugeAddressTextView = (TextView)view.findViewById(R.id.favorite_gauge_location);
            TextView floodWarningTextView = (TextView)view.findViewById(R.id.favorite_flood_warning);
            TextView dateTextView = (TextView)view.findViewById(R.id.favorite_time);
            TextView waterHeightTextView = (TextView)view.findViewById(R.id.favorite_water_height);
            Switch notificationSwitch = (Switch)view.findViewById(R.id.favorite_notification_switch);

            if(NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                if (params.notification == 0) {
                    notificationSwitch.setChecked(false);
                } else {
                    notificationSwitch.setChecked(true);
                }

                notificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                        Log.d("switchButton", checked + " " + params.gaugeName);
                        GaugeApplication.myDBHelper.changeFavoriteNotificationState(params.gauge, checked);
                        if (checked) {
                            CharSequence text = context.getResources().getString(R.string.notifications_enabled) + params.gauge.getGaugeName();
                            Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                            toast.show();
                        } else {
                            CharSequence text = context.getResources().getString(R.string.notifications_disabled) + params.gauge.getGaugeName();
                            Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
                });
            }else{
                notificationSwitch.setChecked(false);
                notificationSwitch.setEnabled(false);
            }



            Log.d("FavoritesAdapter15",String.valueOf(gaugeNameTextView));
            Log.d("FavoritesAdapter6", "Async onPostExecute");
            gaugeNameTextView.setText(params.gaugeName);
            gaugeAddressTextView.setText(params.gaugeAddress);
            if(params.floodWarning.equals("")){
                floodWarningTextView.setVisibility(View.GONE);
            }else {
                if(floodWarningTextView.getVisibility() != View.VISIBLE){
                    floodWarningTextView.setVisibility(View.VISIBLE);
                }
                floodWarningTextView.setText(params.floodWarning);
            }
            dateTextView.setText(params.date);
            if(!params.waterHeight.equals("")) {
                waterHeightTextView.setText(addUnits(context, params.waterHeight));
            }else{
                waterHeightTextView.setText(context.getResources().getString(R.string.no_data_short));
            }
            loadingPanel.setVisibility(View.GONE);


            Log.d("FavoritesAdapter7", "Async Done");

        }



        private String addUnits(Context context, String waterHeight){

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String unitsPref = sharedPref.getString(SettingsFragment.KEY_PREF_UNITS, "");

            int unit = Integer.parseInt(unitsPref);

            switch (unit){
                case GaugeApplication.FEET:
                    return convertToFeet(context, waterHeight);
                case GaugeApplication.METERS:
                    return convertToMeters(context, waterHeight);
                default:
                    return "";
            }



        }

        private String convertToFeet(Context context, String waterHeight){

            double feetDouble = Double.parseDouble(waterHeight);
            return String.format("%.2f",feetDouble) + context.getResources().getString(R.string.feet_unit);
        }

        private String convertToMeters(Context context, String waterHeight){

            double meterConverter = .3048;
            double meterDouble = Double.parseDouble(waterHeight) * meterConverter;
            String meterString = String.valueOf(meterDouble);
            return String.format("%.2f",meterDouble) + context.getResources().getString(R.string.meter_unit);



        }

        private String getFloodWarning(Context mContext, Sigstages sigstages, String waterHeight){

            double actionDouble = 0.0;
            double minorDouble = 0.0;
            double majorDouble = 0.0;
            double moderateDouble = 0.0;
            double waterDouble = 0.0;

            if(sigstages == null){
                return "";
            }
            if(sigstages.getAction() == null && sigstages.getMajor() == null && sigstages.getModerate() == null && sigstages.getFlood() == null){
                return "";
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
                            return mContext.getResources().getString(R.string.major_flooding);
                        }
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }


                }

                if(sigstages.getModerate() !=null){
                    try{
                        moderateDouble = Double.parseDouble(sigstages.getModerate());
                        if(waterDouble >= moderateDouble){
                            return mContext.getResources().getString(R.string.moderate_flooding);
                        }
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }

                }
                if(sigstages.getFlood() !=null){

                    try{
                        minorDouble = Double.parseDouble(sigstages.getFlood());
                        if(waterDouble >= minorDouble){
                            return mContext.getResources().getString(R.string.minor_flooding);
                        }
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }

                }
                if(sigstages.getAction() !=null){

                    try{
                        actionDouble = Double.parseDouble(sigstages.getAction());
                        if(waterDouble >= actionDouble){
                            return mContext.getResources().getString(R.string.minor_flooding);
                        }
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }

                }
            }
            return "";

        }

        private String convertRssDate(String dateString){

            if (dateString.equals("")) {
                return "";
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy h:mm a");
            Date convertedDate = new Date();
            try {
                convertedDate = dateFormat.parse(dateString);
            } catch (ParseException e) {

            } catch (NullPointerException e) {

            }
            SimpleDateFormat myFormat = new SimpleDateFormat("MMM dd h:mma");
            TimeZone tz = TimeZone.getDefault();
            Date now = new Date();
            int offsetFromUtc = tz.getOffset(now.getTime());

            long offset = convertedDate.getTime() + offsetFromUtc;

            Date correctTZDate = new Date(offset);

            return myFormat.format(convertedDate);
        }

        private String convertToDate(String valid){

            if(valid.equals("")){
                return "";
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            Date convertedDate = new Date();
            try {
                convertedDate = dateFormat.parse(valid);


            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (NullPointerException e){
                e.printStackTrace();
            }
            Date date = Calendar.getInstance().getTime();
            DateFormat formatter = new SimpleDateFormat("MMM dd h:mma");
            TimeZone tz = TimeZone.getDefault();
            Date now = new Date();
            int offsetFromUtc = tz.getOffset(now.getTime());
            Log.d("xmlData", "timezone offset is: " + offsetFromUtc);

            Log.d("xmlData", "date is: " + date.getTime());
            long offset = convertedDate.getTime() + offsetFromUtc;
            Log.d("xmlData", "date with offset is: " + offset);
            Date correctTZDate = new Date(offset);


            Log.d("xmlData", "correctTZDate is: " + correctTZDate.getTime());
            return formatter.format(convertedDate);


        }




    }

    private static class FavoriteParams{

        Context context;
        String gaugeName, date,waterHeight, gaugeAddress, floodWarning;
        Gauge gauge;
        int notification;
        View view;

        private FavoriteParams(Context context, View view,String gaugeName, String date, String waterHeight, String gaugeAddress, String floodWarning, Gauge gauge, int notification){

            this.context = context;
            this.view = view;
            this.gaugeName = gaugeName;
            this.date = date;
            this.gaugeAddress = gaugeAddress;
            this.floodWarning = floodWarning;
            this.waterHeight = waterHeight;
            this.gauge = gauge;
            this.notification = notification;
        }

    }


}