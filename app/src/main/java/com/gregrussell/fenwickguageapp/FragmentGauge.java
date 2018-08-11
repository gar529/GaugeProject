package com.gregrussell.fenwickguageapp;

import android.content.Context;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class FragmentGauge extends Fragment {

    Context mContext;
    DataBaseHelperGauges myDBHelper;
    Gauge gauge;
    boolean isFavorite;
    boolean isNotifiable;
    ImageView favoriteButton;
    Switch notificationSwitch;
    Toast notificationToast;
    Gauge selectedGauge;
    final int DAY_IN_MILLIS = 24 * 60 * 60 * 1000;
    final int ONE_DAY = 0;
    final int TWO_DAYS = 1;
    final int ALL = 2;
    ListView listView;
    RelativeLayout progressBarLayout;
    TextView gaugeNameTextView;
    TextView currentDateTextView;
    TextView currentStageTextView;
    TextView floodWarningTextView;
    TextView highStageTextView;
    TextView highDateTextView;
    TextView lowStageTextView;
    TextView lowDateTextView;
    final double BAD_DATA = -12654894165.41586;
    SwipeRefreshLayout swipeRefreshLayout;

    //0 not changed, -1 disabled, 1 enabled
    int switchChangedByFavorite;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle onSavedInstanceState){


        View view = inflater.inflate(R.layout.fragment_gauge_layout, container,false);
        mContext = getContext();
        switchChangedByFavorite = 0;
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
        Toolbar toolbar = (Toolbar)view.findViewById(R.id.gauge_fragment_toolbar);
        toolbar.inflateMenu(R.menu.gauge_fragment_toolbar_menu);

        toolbar.setNavigationIcon(android.support.v7.appcompat.R.drawable.abc_ic_ab_back_material);


        //RelativeLayout view2 = (RelativeLayout)view.findViewById(R.id.action_gauge_frag_favorite);
        notificationSwitch = ((RelativeLayout)view.findViewById(R.id.action_gauge_frag_switch)).findViewById(R.id.switch_layout_switch);
        favoriteButton = ((RelativeLayout)view.findViewById(R.id.action_gauge_frag_favorite)).findViewById(R.id.gauge_frag_favorite_button);
        listView = (ListView)view.findViewById(R.id.fragment_gauge_list);
        progressBarLayout = (RelativeLayout)view.findViewById(R.id.fragment_gauge_progress_bar_layout);
        gaugeNameTextView = (TextView)view.findViewById(R.id.fragment_gauge_name);
        currentDateTextView = (TextView)view.findViewById(R.id.fragment_gauge_current_date);
        currentStageTextView = (TextView)view.findViewById(R.id.fragment_gauge_current_stage);
        floodWarningTextView = (TextView)view.findViewById(R.id.fragment_gauge_flood_warning);
        highStageTextView = (TextView)view.findViewById(R.id.fragment_gauge_high_stage);
        highDateTextView = (TextView)view.findViewById(R.id.fragment_gauge_high_date);
        lowStageTextView = (TextView)view.findViewById(R.id.fragment_gauge_low_stage);
        lowDateTextView = (TextView)view.findViewById(R.id.fragment_gauge_low_date);
        swipeRefreshLayout = (SwipeRefreshLayout)view.findViewById(R.id.fragment_gauge_swipe_refresh);
        MenuItem item = toolbar.getMenu().findItem(R.id.action_gauge_frag_favorite);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int topRowVerticalPosition;
                if(listView == null || listView.getChildCount() == 0){
                    topRowVerticalPosition = 0;
                }else{
                    topRowVerticalPosition = listView.getChildAt(0).getTop();
                }
                if(firstVisibleItem == 0 && topRowVerticalPosition >=0) {
                    swipeRefreshLayout.setEnabled(true);
                }else{
                    swipeRefreshLayout.setEnabled(false);
                }

            }
        });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                GetGaugeData getGaugeData = new GetGaugeData();
                getGaugeData.execute();
            }
        });

        Bundle bundle = this.getArguments();



        if(bundle != null){
            selectedGauge =(Gauge)bundle.get("selected_gauge");
            gauge = (Gauge)bundle.get("gauge");
            isFavorite = bundle.getBoolean("isFavorite");
            isNotifiable = bundle.getBoolean("isNotifiable");
            String gaugeName = gauge.getGaugeName();
            toolbar.setTitle(gaugeName);

            if(isFavorite){
                favoriteButton.setSelected(true);
            }else {
                favoriteButton.setSelected(false);
            }

            if(NotificationManagerCompat.from(getContext()).areNotificationsEnabled()) {

                notificationSwitch.setEnabled(true);
                if(isNotifiable){
                    notificationSwitch.setChecked(true);
                }else{
                    notificationSwitch.setChecked(false);
                }

            }else{
                notificationSwitch.setEnabled(false);
                notificationSwitch.setChecked(false);
            }


        }

        notificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {

                if(!isFavorite){
                    notificationSwitch.setChecked(false);
                    if(switchChangedByFavorite == 0) {
                        CharSequence text = getContext().getResources().getString(R.string.favorite_for_notifications);
                        notificationToast = Toast.makeText(getContext(), text, Toast.LENGTH_SHORT);
                        notificationToast.show();
                    }
                    switchChangedByFavorite = 0;
                }else{
                    enableNotifications(checked);
                }

            }
        });



        favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(favoriteButton.isSelected()){
                    favoriteButton.setSelected(false);
                    Log.d("fragGauge2",String.valueOf(gauge));

                    RemoveFavorite task = new RemoveFavorite();
                    task.execute();

                }else{
                    favoriteButton.setSelected(true);
                    Log.d("fragGauge1",String.valueOf(gauge));
                    AddFavorite task = new AddFavorite();
                    task.execute();
                }
            }
        });







        //gaugeName


        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                if(selectedGauge !=null) {
                    Log.d("backpressed8,", String.valueOf(selectedGauge));
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("gauge", selectedGauge);
                    LoadGaugeFragment loadGaugeFragment = new LoadGaugeFragment();
                    loadGaugeFragment.setArguments(bundle);
                    fragmentTransaction.replace(R.id.gauge_data_layout, loadGaugeFragment, "load_gauge_fragment");
                }
                fragmentTransaction.remove(fragmentManager.findFragmentByTag("gauge_fragment"));
                fragmentTransaction.commit();
                if(fragmentManager.getBackStackEntryCount() > 0){
                    fragmentManager.popBackStack();
                }


            }
        });




        GetGaugeData getGaugeData = new GetGaugeData();
        getGaugeData.execute();




        return view;
    }

    private void enableNotifications(boolean enable){

        EnableNotifications task = new EnableNotifications();
        task.execute(enable);

    }

    private class EnableNotifications extends AsyncTask<Boolean,Void,Boolean> {

        @Override
        protected Boolean doInBackground(Boolean... params){

            boolean enable = params[0];
            myDBHelper.changeFavoriteNotificationState(gauge,enable);

            return enable;
        }

        @Override
        protected void onPostExecute(Boolean enable){
            String toastText = "";
            if(enable) {
                toastText = mContext.getResources().getString(R.string.notifications_enabled) + gauge.getGaugeName();
            }else{
                toastText = mContext.getResources().getString(R.string.notifications_disabled) + gauge.getGaugeName();
            }
            if(switchChangedByFavorite == 0) {
                notificationToast = Toast.makeText(mContext, toastText, Toast.LENGTH_SHORT);
                notificationToast.show();
            }
            switchChangedByFavorite = 0;
        }
    }

    private class AddFavorite extends AsyncTask<Void,Void,Boolean> {

        @Override
        protected Boolean doInBackground(Void... params){


            myDBHelper.addFavorite(gauge);
            Log.d("numFavorites",String.valueOf(myDBHelper.getFavoritesCount()));
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result){

            if(!notificationSwitch.isChecked()) {
                switchChangedByFavorite = 1;
            }
            else {
                switchChangedByFavorite = 0;
            }
            isFavorite = result;
            notificationSwitch.setChecked(true);

            String toastText = gauge.getGaugeName() + mContext.getResources().getString(R.string.add_favorite);
            Toast toast = Toast.makeText(mContext,toastText,Toast.LENGTH_SHORT);
            toast.show();




        }
    }


    private class RemoveFavorite extends AsyncTask<Void,Void,Boolean>{

        @Override
        protected Boolean doInBackground(Void... params){

            Log.d("fragGauge3",String.valueOf(gauge));
            myDBHelper.removeFavorite(gauge);
            Log.d("numFavorites",String.valueOf(myDBHelper.getFavoritesCount()));
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result){

            if(notificationSwitch.isChecked()) {
                switchChangedByFavorite = -1;
            }
            else {
                switchChangedByFavorite = 0;
            }
            isFavorite = result;
            notificationSwitch.setChecked(false);
            String toastText = gauge.getGaugeName() + mContext.getResources().getString(R.string.remove_favorite);
            Toast toast = Toast.makeText(mContext,toastText,Toast.LENGTH_SHORT);
            toast.show();




        }
    }

    private class GetGaugeData extends AsyncTask<Void,Void,GaugeReadParseObject>{

        @Override
        protected void onPreExecute(){

            progressBarLayout.setVisibility(View.VISIBLE);
        }

        @Override
        protected GaugeReadParseObject doInBackground(Void... params){

            GaugeData gaugeData = new GaugeData(gauge.getGaugeID());
            GaugeReadParseObject gaugeReadParseObject = gaugeData.getData();
            List<Datum> list = gaugeReadParseObject.getDatumList();
            gaugeReadParseObject.setDatumList(filterList(list,ONE_DAY));

            return gaugeReadParseObject;
        }

        @Override
        protected void onPostExecute(GaugeReadParseObject gaugeReadParseObject){

            if(gaugeReadParseObject!=null && gaugeReadParseObject.getDatumList() !=null && gaugeReadParseObject.getDatumList().size() > 0){
                List<Datum>datumList = gaugeReadParseObject.getDatumList();
                String currentStageText = addUnits(gaugeReadParseObject.getDatumList().get(0).getPrimary());
                String currentDateText = convertDate(gaugeReadParseObject.getDatumList().get(0).getValid());
                DataBound dataBound = new DataBound(datumList);
                String highStageText = addUnits(dataBound.getHighStage());
                String lowStageText = addUnits(dataBound.getLowStage());
                String highDateText = convertDate(dataBound.getHighDate());
                String lowDateText = convertDate(dataBound.getLowDate());
                String gaugeName = gauge.getGaugeName();
                String floodWarning = getFloodWarning(gaugeReadParseObject.getSigstages(),gaugeReadParseObject.getDatumList().get(0).getPrimary());

                gaugeNameTextView.setText(gaugeName);
                currentStageTextView.setText(currentStageText);
                currentDateTextView.setText(currentDateText);
                highDateTextView.setText(highDateText);
                highStageTextView.setText(highStageText);
                lowDateTextView.setText(lowDateText);
                lowStageTextView.setText(lowStageText);
                if(!floodWarning.equals("")) {
                    floodWarningTextView.setVisibility(View.VISIBLE);
                    floodWarningTextView.setText(floodWarning);
                }else {
                    floodWarningTextView.setVisibility(View.GONE);
                    floodWarningTextView.setText("");
                }


                GaugeDataListAdapter gaugeDataListAdapter = new GaugeDataListAdapter(mContext,datumList);
                if(listView !=null){
                    listView.setAdapter(null);
                    listView.setAdapter(gaugeDataListAdapter);
                    gaugeDataListAdapter.notifyDataSetChanged();
                }
                progressBarLayout.setVisibility(View.GONE);
                if(swipeRefreshLayout.isRefreshing()){
                    swipeRefreshLayout.setRefreshing(false);
                }





            }else{
                currentStageTextView.setText(mContext.getResources().getString(R.string.no_data_short));
                currentDateTextView.setText(mContext.getResources().getString(R.string.no_data));
                highStageTextView.setText(mContext.getResources().getString(R.string.no_data_short));
                lowStageTextView.setText(mContext.getResources().getString(R.string.no_data_short));

                if(listView !=null){
                    listView.setAdapter(null);
                }
                progressBarLayout.setVisibility(View.GONE);
                if(swipeRefreshLayout.isRefreshing()){
                    swipeRefreshLayout.setRefreshing(false);
                }
            }


        }


    }

    private List<Datum> filterList(List<Datum> datumList, int filter){

        Date currentDate = Calendar.getInstance().getTime();
        Long currentDateMillis = currentDate.getTime();

        switch (filter) {
            case 0:
                return filterOneDay(datumList, currentDateMillis);
            case 1:
                return filterTwoDays(datumList, currentDateMillis);
            case 2:
                return datumList;
            default:
                return datumList;
        }






    }

    private List<Datum> filterOneDay(List<Datum> datumList, Long currentDateMillis){


        List<Datum> filteredList = new ArrayList<Datum>();
        for(int i =0; i<datumList.size();i++){

            Long dateInMillis = convertStringToDate(datumList.get(i).getValid()).getTime();
            Long difference = currentDateMillis - dateInMillis;
            Log.d("dateInListIssue2", "difference is: " + difference / 1000 / 60 / 60 + " day in millis is: " + DAY_IN_MILLIS / 1000 / 60 / 60);
            if(difference <= DAY_IN_MILLIS){
                filteredList.add(datumList.get(i));
                DateFormat formatter = new SimpleDateFormat("MMM dd h:mma");
                Date current = new Date(currentDateMillis);
                Date data = new Date(dateInMillis);
                String dateString = formatter.format(current);
                String dateString1 = formatter.format(data);


                Log.d("dateInListIssue1",  "current time: " + dateString + " time added: " + dateString1);
            }
        }

        return filteredList;



    }

    private List<Datum> filterTwoDays(List<Datum> datumList, Long currentDateMillis){

        List<Datum> filteredList = new ArrayList<Datum>();
        for(int i =0; i<datumList.size();i++){

            Long dateInMillis = convertStringToDate(datumList.get(i).getValid()).getTime();
            Long difference = currentDateMillis - dateInMillis;
            if(difference <= DAY_IN_MILLIS * 2){
                filteredList.add(datumList.get(i));
            }
        }

        return filteredList;

    }

    private Date convertStringToDate(String dateString){

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

        return convertedDate;
    }

    private List<Datum> sortDatumList(List<Datum> datumList){

        Collections.sort(datumList, new Comparator<Datum>() {
            @Override
            public int compare(Datum o1, Datum o2) {
                int i;

                if(parseStage(o1.getPrimary()) < parseStage(o2.getPrimary())){
                    i = -1;
                } else if(parseStage(o1.getPrimary()) > parseStage(o2.getPrimary())){
                    i = 1;
                }else {
                    i=0;
                }
                return i;
            }
        });

        return datumList;

    }



    private double parseStage(String stage){

        double waterDouble = BAD_DATA;
        try {
            waterDouble = Double.parseDouble(stage);

        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return waterDouble;

    }


    private String addUnits(String stage){

        return stage +  getResources().getString(R.string.feet_unit);

    }


    private class DataBound{
        private List<Datum> datumList;
        private List<Datum> data;
        private List<Datum> list;

        public DataBound(List<Datum> datumList){
            this.datumList = datumList;
            list = new ArrayList<Datum>();
            list.addAll(this.datumList);
            data = sortDatumList(list);
        }

        public String getHighStage(){


            for(int i = data.size() - 1; i > -1;i--) {

                String highStage = data.get(i).getPrimary();
                if (parseStage(highStage) != BAD_DATA){
                    return highStage;
                }
            }
            return "No Data";
        }
        public String getLowStage(){

            for(int i = 0; i < data.size();i++) {

                String lowStage = data.get(i).getPrimary();
                if (parseStage(lowStage) != BAD_DATA){
                    return lowStage;
                }
            }
            return "No Data";

        }
        public String getHighDate(){

            for(int i = data.size() - 1; i > -1;i--) {

                String highStage = data.get(i).getPrimary();
                String highDate = data.get(i).getValid();
                if (parseStage(highStage) != BAD_DATA){
                    return highDate;
                }
            }
            return "No Data";
        }
        public String getLowDate(){

            for(int i = 0; i < data.size();i++) {

                String lowStage = data.get(i).getPrimary();
                String lowDate = data.get(i).getValid();
                if (parseStage(lowStage) != BAD_DATA){
                    return lowDate;
                }
            }
            return "No Data";
        }

    }

    private String convertDate(String dateString){

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
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
