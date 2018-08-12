package com.gregrussell.fenwickguageapp;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class LoadGaugeFragment  extends Fragment {

    private static Gauge gauge;


    ImageView favoriteButton;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle onSavedInstanceState) {

        Log.d("loadGaugeFragment1","frag start");

        View view = inflater.inflate(R.layout.load_gauge_fragment_layout, container, false);
        final Context context = getContext();
        Bundle bundle = this.getArguments();




        favoriteButton = (ImageView)view.findViewById(R.id.load_gauge_fragment_favorite_button);
        RelativeLayout loadingPanel = (RelativeLayout) view.findViewById(R.id.load_gauge_fragment_list_loading_panel);
        loadingPanel.setVisibility(View.VISIBLE);
        LinearLayout gaugeText = (LinearLayout) view.findViewById(R.id.load_gauge_fragment_data_text);
        gaugeText.setVisibility(View.GONE);



        if(bundle != null) {
            gauge = (Gauge) bundle.get("gauge");
            favoriteButton.setClickable(true);
            favoriteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(favoriteButton.isSelected()){
                        favoriteButton.setSelected(false);
                        RemoveFavorite task = new RemoveFavorite();
                        task.execute(context);
                    }else{
                        favoriteButton.setSelected(true);


                        AddFavorite task = new AddFavorite();
                        task.execute(context);

                    }
                }
            });

            LoadGaugeParams params = new LoadGaugeParams(context,view,null,false,
                    loadingPanel,gaugeText,favoriteButton);
            LoadGauge task = new LoadGauge();
            task.execute(params);

        }






        return view;

    }

    private static class LoadGaugeParams{


        Context context;
        View view;
        GaugeReadParseObject gaugeReadParseObject;
        boolean isFavorite;
        RelativeLayout loadingPanel;
        LinearLayout gaugeText;
        ImageView favoriteButton;


        private LoadGaugeParams(Context context, View view, GaugeReadParseObject gaugeReadParseObject,
                                boolean isFavorite, RelativeLayout loadingPanel, LinearLayout gaugeText,
                                ImageView favoriteButton){

            this.context = context;
            this.view = view;
            this.gaugeReadParseObject = gaugeReadParseObject;
            this.isFavorite = isFavorite;
            this.loadingPanel = loadingPanel;
            this.gaugeText = gaugeText;
            this.favoriteButton = favoriteButton;
        }
    }

    private static class LoadGauge extends AsyncTask<LoadGaugeParams, Void, LoadGaugeParams> {



        @Override protected void onPreExecute(){



        }

        @Override
        protected LoadGaugeParams doInBackground(LoadGaugeParams... params){



            Context context = params[0].context;
            View view = params[0].view;
            RelativeLayout loadingPanel = params[0].loadingPanel;
            LinearLayout gaugeText = params[0].gaugeText;
            ImageView favoriteButton = params[0].favoriteButton;
            GaugeData gaugeData = new GaugeData(gauge.getGaugeID());
            GaugeReadParseObject gaugeReadParseObject = gaugeData.getData();
            boolean isFavorite = GaugeApplication.myDBHelper.isFavorite(gauge);
            return new LoadGaugeParams(context,view,gaugeReadParseObject,isFavorite,loadingPanel,
                    gaugeText,favoriteButton);
        }

        @Override protected void onPostExecute(LoadGaugeParams result){

            View view = result.view;
            Context context = result.context;
            RelativeLayout loadingPanel = result.loadingPanel;
            LinearLayout gaugeText = result.gaugeText;
            ImageView favoriteButton = result.favoriteButton;

            TextView gaugeNameText = (TextView)view.findViewById(R.id.load_gauge_fragment_gauge_name);
            TextView waterHeight = (TextView) view.findViewById(R.id.load_gauge_fragment_water_height);
            TextView time = (TextView) view.findViewById(R.id.load_gauge_fragment_reading_time);
            TextView floodWarning = (TextView)view.findViewById(R.id.load_gauge_fragment_flood_warning);


            if(result.isFavorite){
                favoriteButton.setSelected(true);
            }else{
                favoriteButton.setSelected(false);
            }

            waterHeight.setTextSize(TypedValue.COMPLEX_UNIT_SP,40);


            if(result.gaugeReadParseObject.getDatumList() != null && result.gaugeReadParseObject.getDatumList().size() > 0) {


                String gaugeName = gauge.getGaugeName();
                String water = result.gaugeReadParseObject.getDatumList().get(0).getPrimary() + context.getResources().getString(R.string.feet_unit);


                Log.d("onPostExecute result", "valid is: " + result.gaugeReadParseObject.getDatumList().get(0).getValid() + " primary is: " + result.gaugeReadParseObject.getDatumList().get(0).getPrimary());
                gaugeNameText.setText(gaugeName);
                waterHeight.setText(water);
                floodWarning.setText(getFloodWarning(context,result.gaugeReadParseObject.getSigstages(),result.gaugeReadParseObject.getDatumList().get(0).getPrimary()));
                String dateString = result.gaugeReadParseObject.getDatumList().get(0).getValid();
                time.setText(convertDate(dateString));
                loadingPanel.setVisibility(View.GONE);
                gaugeText.setVisibility(View.VISIBLE);
            }
            else{
                gaugeNameText.setText(gauge.getGaugeName());
                waterHeight.setText(context.getString(R.string.no_data));
                waterHeight.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
                time.setText("");
                loadingPanel.setVisibility(View.GONE);
                gaugeText.setVisibility(View.VISIBLE);
            }
        }

        private String convertDate(String dateString){
            //String dateString = "2018-08-10T12:05:00-00:00";
            Log.d("dateIssue1","value of the data retrieved from server: " + dateString);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
            Date convertedDate = new Date();
            try {
                convertedDate = dateFormat.parse(dateString);
                Log.d("dateIssue2","value of data retried from server converted to date in millis: " + convertedDate.getTime());


            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (NullPointerException e){
                e.printStackTrace();
            }
            Date date = Calendar.getInstance().getTime();
            DateFormat formatter = new SimpleDateFormat("MMM dd h:mma");
            return formatter.format(convertedDate);
        }

        private String getFloodWarning(Context context, Sigstages sigstages, String waterHeight){

            double minorDouble = 0.0;
            double majorDouble = 0.0;
            double moderateDouble = 0.0;
            double waterDouble = 0.0;

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
                        return context.getString(R.string.major_flooding);
                    }
                }

                if(sigstages.getModerate() !=null){
                    try{
                        moderateDouble = Double.parseDouble(sigstages.getModerate());
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }
                    if(waterDouble >= moderateDouble){
                        return context.getString(R.string.moderate_flooding);
                    }
                }
                if(sigstages.getFlood() !=null){

                    try{
                        minorDouble = Double.parseDouble(sigstages.getFlood());
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }
                    if(waterDouble >= minorDouble){
                        return context.getString(R.string.minor_flooding);
                    }
                }


            }
            return "";

        }
    }



    private static class AddFavorite extends AsyncTask<Context,Void,Context>{

        @Override
        protected Context doInBackground(Context... params){

            GaugeApplication.myDBHelper.addFavorite(gauge);
            Log.d("numFavorites",String.valueOf(GaugeApplication.myDBHelper.getFavoritesCount()));
            return params[0];
        }

        @Override
        protected void onPostExecute(Context context){
            String toastText = gauge.getGaugeName() + context.getResources().getString(R.string.add_favorite);
            Toast toast = Toast.makeText(context,toastText,Toast.LENGTH_SHORT);
            toast.show();
        }
    }


    private static class RemoveFavorite extends AsyncTask<Context,Void,Context>{

        @Override
        protected Context doInBackground(Context... params){

            GaugeApplication.myDBHelper.removeFavorite(gauge);
            Log.d("numFavorites",String.valueOf(GaugeApplication.myDBHelper.getFavoritesCount()));
            return params[0];
        }

        @Override
        protected void onPostExecute(Context context){
            String toastText = gauge.getGaugeName() + context.getResources().getString(R.string.remove_favorite);
            Toast toast = Toast.makeText(context,toastText,Toast.LENGTH_SHORT);
            toast.show();

        }
    }
}
