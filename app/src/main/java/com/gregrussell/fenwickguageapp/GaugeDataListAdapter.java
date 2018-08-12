package com.gregrussell.fenwickguageapp;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class GaugeDataListAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private Context mContext;
    private List<Datum> datumList;
    private View currentView;

    public GaugeDataListAdapter(Context mContext, List<Datum> datumList){
        this.mContext = mContext;
        this.datumList = datumList;
        inflater = ((Activity)mContext).getLayoutInflater();
    }

    @Override
    public int getCount() {
        return datumList.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        currentView = view;
        if(currentView == null){
            currentView = inflater.inflate(R.layout.gauge_data_list_layout, parent,false);
        }

        TextView dateData = (TextView)currentView.findViewById(R.id.gauge_data_list_date);
        TextView dataStage = (TextView)currentView.findViewById(R.id.gauge_data_list_stage);
        LinearLayout dataContainer = (LinearLayout)currentView.findViewById(R.id.gauge_data_list_container);

        String dateText = convertDate(datumList.get(position).getValid());
        String stageText = addUnits(datumList.get(position).getPrimary());


        dateData.setText(dateText);
        dataStage.setText(stageText);
        if(position % 2 == 0){
            dataContainer.setBackgroundColor(mContext.getResources().getColor(R.color.color_light_list_item));
        }else{
            dataContainer.setBackgroundColor(mContext.getResources().getColor(R.color.color_dark_list_item));
        }



        return currentView;
    }

    private String convertDate(String dateString){

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
        return(formatter.format(convertedDate));

    }

    private String addUnits(String stage){


        return stage + mContext.getResources().getString(R.string.feet_unit);
    }
}