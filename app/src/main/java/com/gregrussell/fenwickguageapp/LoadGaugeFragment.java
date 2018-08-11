package com.gregrussell.fenwickguageapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

    Gauge gauge;
    RelativeLayout loadingPanel;
    View gaugeText;
    ImageView favoriteButton;
    TextView gaugeNameText;
    TextView waterHeight;
    TextView time;
    TextView floodWarning;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle onSavedInstanceState) {

        Log.d("loadGaugeFragment1","frag start");

        View view = inflater.inflate(R.layout.load_gauge_fragment_layout, container, false);

        Bundle bundle = this.getArguments();



        loadingPanel = (RelativeLayout) view.findViewById(R.id.load_gauge_fragment_list_loading_panel);
        gaugeText = (View) view.findViewById(R.id.load_gauge_fragment_data_text);
        gaugeNameText = (TextView)view.findViewById(R.id.load_gauge_fragment_gauge_name);
        waterHeight = (TextView) view.findViewById(R.id.load_gauge_fragment_water_height);
        time = (TextView) view.findViewById(R.id.load_gauge_fragment_reading_time);
        floodWarning = (TextView)view.findViewById(R.id.load_gauge_fragment_flood_warning);
        favoriteButton = (ImageView)view.findViewById(R.id.load_gauge_fragment_favorite_button);


        if(bundle != null) {
            gauge = (Gauge) bundle.get("gauge");
            favoriteButton.setClickable(true);
            favoriteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(favoriteButton.isSelected()){
                        favoriteButton.setSelected(false);
                        RemoveFavorite task = new RemoveFavorite();
                        task.execute(gauge);
                    }else{
                        favoriteButton.setSelected(true);


                        AddFavorite task = new AddFavorite();
                        task.execute(gauge);

                    }
                }
            });

            LoadGauge task = new LoadGauge();
            task.execute(gauge);

        }






        return view;

    }

    public class LoadGauge extends AsyncTask<Gauge, Void, LoadGaugeParams> {



        @Override protected void onPreExecute(){


            gaugeText.setVisibility(View.GONE);
            loadingPanel.setVisibility(View.VISIBLE);
        }

        @Override
        protected LoadGaugeParams doInBackground(Gauge... gauge){



            GaugeData gaugeData = new GaugeData(gauge[0].getGaugeID());
            GaugeReadParseObject gaugeReadParseObject = gaugeData.getData();
            boolean isFavorite = GaugeApplication.myDBHelper.isFavorite(gauge[0]);

            LoadGaugeParams params = new LoadGaugeParams(gaugeReadParseObject,gauge[0],isFavorite);

            return params;
        }

        @Override protected void onPostExecute(LoadGaugeParams result){



            if(result.isFavorite){
                favoriteButton.setSelected(true);
            }else{
                favoriteButton.setSelected(false);
            }

            waterHeight.setTextSize(TypedValue.COMPLEX_UNIT_SP,40);


            if(result.gaugeReadParseObject.getDatumList() != null && result.gaugeReadParseObject.getDatumList().size() > 0) {


                String gaugeName = result.gauge.getGaugeName();
                String water = result.gaugeReadParseObject.getDatumList().get(0).getPrimary() + getActivity().getResources().getString(R.string.feet_unit);


                Log.d("onPostExecute result", "valid is: " + result.gaugeReadParseObject.getDatumList().get(0).getValid() + " primary is: " + result.gaugeReadParseObject.getDatumList().get(0).getPrimary());
                gaugeNameText.setText(gaugeName);
                waterHeight.setText(water);
                floodWarning.setText(getFloodWarning(result.gaugeReadParseObject.getSigstages(),result.gaugeReadParseObject.getDatumList().get(0).getPrimary()));
                String dateString = result.gaugeReadParseObject.getDatumList().get(0).getValid();
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
                TimeZone tz = TimeZone.getDefault();
                Date now = new Date();
                int offsetFromUtc = tz.getOffset(now.getTime());
                Log.d("xmlData", "timezone offset is: " + offsetFromUtc);

                Log.d("xmlData", "date is: " + date.getTime());
                long offset = convertedDate.getTime() + offsetFromUtc;
                Log.d("xmlData", "date with offset is: " + offset);
                Date correctTZDate = new Date(offset);


                Log.d("xmlData", "correctTZDate is: " + correctTZDate.getTime());
                time.setText(formatter.format(convertedDate));
                loadingPanel.setVisibility(View.GONE);
                gaugeText.setVisibility(View.VISIBLE);
            }
            else{
                gaugeNameText.setText(result.gauge.getGaugeName());
                waterHeight.setText(getResources().getString(R.string.no_data));
                waterHeight.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
                time.setText("");
                loadingPanel.setVisibility(View.GONE);
                gaugeText.setVisibility(View.VISIBLE);
            }
        }

        private String getFloodWarning(Sigstages sigstages, String waterHeight){

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
                        return getResources().getString(R.string.major_flooding);
                    }
                }

                if(sigstages.getModerate() !=null){
                    try{
                        moderateDouble = Double.parseDouble(sigstages.getModerate());
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }
                    if(waterDouble >= moderateDouble){
                        return getResources().getString(R.string.moderate_flooding);
                    }
                }
                if(sigstages.getFlood() !=null){

                    try{
                        minorDouble = Double.parseDouble(sigstages.getFlood());
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }
                    if(waterDouble >= minorDouble){
                        return getResources().getString(R.string.minor_flooding);
                    }
                }


            }
            return "";

        }
    }

    private class LoadGaugeParams{

        GaugeReadParseObject gaugeReadParseObject;
        Gauge gauge;
        boolean isFavorite;

        private LoadGaugeParams(GaugeReadParseObject gaugeReadParseObject,Gauge gauge, boolean isFavorite){

            this.gaugeReadParseObject = gaugeReadParseObject;
            this.gauge = gauge;
            this.isFavorite = isFavorite;
        }
    }

    private class AddFavorite extends AsyncTask<Gauge,Void,Gauge>{

        @Override
        protected Gauge doInBackground(Gauge... params){

            GaugeApplication.myDBHelper.addFavorite(params[0]);
            Log.d("numFavorites",String.valueOf(GaugeApplication.myDBHelper.getFavoritesCount()));
            return params[0];
        }

        @Override
        protected void onPostExecute(Gauge result){
            String toastText =result.getGaugeName() + getResources().getString(R.string.add_favorite);
            Toast toast = Toast.makeText(getContext(),toastText,Toast.LENGTH_SHORT);
            toast.show();
        }
    }


    private class RemoveFavorite extends AsyncTask<Gauge,Void,Gauge>{

        @Override
        protected Gauge doInBackground(Gauge... params){

            GaugeApplication.myDBHelper.removeFavorite(params[0]);
            Log.d("numFavorites",String.valueOf(GaugeApplication.myDBHelper.getFavoritesCount()));
            return params[0];
        }

        @Override
        protected void onPostExecute(Gauge result){
            String toastText = result.getGaugeName() + getResources().getString(R.string.remove_favorite);
            Toast toast = Toast.makeText(getContext(),toastText,Toast.LENGTH_SHORT);
            toast.show();

        }
    }
}
