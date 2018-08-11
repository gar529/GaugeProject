package com.gregrussell.fenwickguageapp;

import android.app.Activity;
import android.content.Context;
import android.database.SQLException;
import android.os.AsyncTask;
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
import java.text.DateFormat;
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
    private TextView gaugeNameTextView;
    private TextView waterHeightTextView;
    private TextView gaugeAddressTextView;
    private TextView floodWarningTextView;
    private TextView dateTextView;
    private RelativeLayout loadingPanel;
    private boolean background;
    private DataBaseHelperGauges myDBHelper;
    private Switch notificationSwitch;


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

        currentView = convertView;
        if(currentView == null){
            currentView = inflater.inflate(R.layout.favorite_list_layout,parent,false);
        }

        myDBHelper = new DataBaseHelperGauges(mContext);
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




        Log.d("FavoritesAdapter","gaugelist size " + gaugeList.size());
        Log.d("FavoritesAdapter2","current position " + position + ", gauge used " + gaugeList.get(position).getGaugeName());

        currentView.setTag(gaugeList.get(position));



        Log.d("FavoritesAdapter12",String.valueOf(gaugeNameTextView));

        LoadFavoriteParams params = new LoadFavoriteParams(currentView,gaugeList.get(position));
        LoadFavorite task = new LoadFavorite();
        task.execute(params);




        Log.d("FavoritesAdapter3", "returning View");
        return currentView;
    }

    private String convertToDate(String valid){

        if(valid.equals("")){
            return "";
        }
        String dateString = valid;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
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

    private String getFloodWarning(Sigstages sigstages, String waterHeight){

        double minorDouble = 0.0;
        double majorDouble = 0.0;
        double moderateDouble = 0.0;
        double waterDouble = 0.0;

        if(sigstages == null){
            return "";
        }
        if(sigstages.getMajor() == null && sigstages.getModerate() == null && sigstages.getFlood() == null){
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
                }catch (NumberFormatException e){
                    e.printStackTrace();
                }

                if(waterDouble >= majorDouble){
                    return mContext.getResources().getString(R.string.major_flooding);
                }
            }

            if(sigstages.getModerate() !=null){
                try{
                    moderateDouble = Double.parseDouble(sigstages.getModerate());
                }catch (NumberFormatException e){
                    e.printStackTrace();
                }
                if(waterDouble >= moderateDouble){
                    return mContext.getResources().getString(R.string.moderate_flooding);
                }
            }
            if(sigstages.getFlood() !=null){

                try{
                    minorDouble = Double.parseDouble(sigstages.getFlood());
                }catch (NumberFormatException e){
                    e.printStackTrace();
                }
                if(waterDouble >= minorDouble){
                    return mContext.getResources().getString(R.string.minor_flooding);
                }
            }


        }
        return "";

    }

    private class LoadFavoriteParams{
        View view;
        Gauge gauge;
        private LoadFavoriteParams(View view, Gauge gauge){
            this.view = view;
            this.gauge = gauge;
        }
    }


    private class LoadFavorite extends AsyncTask<LoadFavoriteParams,Void,FavoriteParams>{

        View view;

        @Override
        protected void onPreExecute(){

            Log.d("FavoritesAdapter4", "Async Start");
            Log.d("FavoritesAdapter13",String.valueOf(gaugeNameTextView));


        }
        @Override
        protected FavoriteParams doInBackground(LoadFavoriteParams... position){


            view = position[0].view;
            Log.d("FavoritesAdapter5", "Async DoInBackground");
            Gauge gauge = position[0].gauge;
            String gaugeName = gauge.getGaugeName();
            String gaugeAddress = gauge.getGaugeAddress();


            GaugeData gaugeData = new GaugeData(gauge.getGaugeID());
            GaugeReadParseObject gaugeReadParseObject = gaugeData.getData();
            Datum datum = new Datum();
            String waterHeight = "";
            String primary =  "";
            Log.d("favoriteS1",String.valueOf(gaugeReadParseObject));
            Log.d("favoriteS2",String.valueOf(gaugeReadParseObject.getDatumList()));
            if(gaugeReadParseObject.getDatumList() != null) {
                if (gaugeReadParseObject.getDatumList().size() > 0) {

                    datum = gaugeReadParseObject.getDatumList().get(0);
                    primary = datum.getPrimary();
                    waterHeight = primary + mContext.getResources().getString(R.string.feet_unit);
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

            String valid = datum.getValid();

            String floodWarning = getFloodWarning(gaugeReadParseObject.getSigstages(),primary);
            String date = convertToDate(valid);

            Log.d("FavoritesAdapter14",waterHeight);

            int notification = myDBHelper.getFavoriteNotificationState(gauge);



            return new FavoriteParams(gaugeName,date,waterHeight,gaugeAddress,floodWarning, gauge, notification);
        }
        @Override
        protected void onPostExecute(final FavoriteParams params){

            loadingPanel = (RelativeLayout)view.findViewById(R.id.favorite_list_loading_panel);
            gaugeNameTextView = (TextView)view.findViewById(R.id.favorite_gauge_name);
            gaugeAddressTextView = (TextView)view.findViewById(R.id.favorite_gauge_location);
            floodWarningTextView = (TextView)view.findViewById(R.id.favorite_flood_warning);
            dateTextView = (TextView)view.findViewById(R.id.favorite_time);
            waterHeightTextView = (TextView)view.findViewById(R.id.favorite_water_height);
            notificationSwitch = (Switch)view.findViewById(R.id.favorite_notification_switch);

            if(NotificationManagerCompat.from(mContext).areNotificationsEnabled()) {
                if (params.notification == 0) {
                    notificationSwitch.setChecked(false);
                } else {
                    notificationSwitch.setChecked(true);
                }

                notificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                        Log.d("switchButton", checked + " " + params.gaugeName);
                        myDBHelper.changeFavoriteNotificationState(params.gauge, checked);
                        if (checked) {
                            CharSequence text = mContext.getResources().getString(R.string.notifications_enabled) + params.gauge.getGaugeName();
                            Toast toast = Toast.makeText(mContext, text, Toast.LENGTH_SHORT);
                            toast.show();
                        } else {
                            CharSequence text = mContext.getResources().getString(R.string.notifications_disabled) + params.gauge.getGaugeName();
                            Toast toast = Toast.makeText(mContext, text, Toast.LENGTH_SHORT);
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
            waterHeightTextView.setText(params.waterHeight);
            loadingPanel.setVisibility(View.GONE);


            Log.d("FavoritesAdapter7", "Async Done");

        }




    }

    private class FavoriteParams{

        String gaugeName, date,waterHeight, gaugeAddress, floodWarning;
        Gauge gauge;
        int notification;

        private FavoriteParams(String gaugeName, String date, String waterHeight, String gaugeAddress, String floodWarning, Gauge gauge, int notification){

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
