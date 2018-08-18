package com.gregrussell.fenwickguageapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
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
    static Gauge gauge;
    static boolean isFavorite;
    static boolean isNotifiable;
    ImageView favoriteButton;
    Switch notificationSwitch;
    Toast notificationToast;
    Gauge selectedGauge;
    final static int DAY_IN_MILLIS = 24 * 60 * 60 * 1000;
    final static int ONE_DAY = 0;
    final static int TWO_DAYS = 1;
    final static int ALL = 2;
    ListView listView;
    RelativeLayout progressBarLayout;
    final static double BAD_DATA = -12654894165.41586;
    SwipeRefreshLayout swipeRefreshLayout;
    Toolbar toolbar;
    View view;

    //0 not changed, -1 disabled, 1 enabled
    static int switchChangedByFavorite;


    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, Bundle onSavedInstanceState){


        Log.d("FragmentGaugeOnCreate", "onCreate");
        view = inflater.inflate(R.layout.fragment_gauge_layout, container,false);
        mContext = getContext();
        switchChangedByFavorite = 0;
        toolbar = (Toolbar)view.findViewById(R.id.gauge_fragment_toolbar);
        toolbar.inflateMenu(R.menu.gauge_fragment_toolbar_menu);

        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.baseline_arrow_back_black));



        //RelativeLayout view2 = (RelativeLayout)view.findViewById(R.id.action_gauge_frag_favorite);
        notificationSwitch = ((RelativeLayout)view.findViewById(R.id.action_gauge_frag_switch)).findViewById(R.id.switch_layout_switch);
        favoriteButton = ((RelativeLayout)view.findViewById(R.id.action_gauge_frag_favorite)).findViewById(R.id.gauge_frag_favorite_button);
        listView = (ListView)view.findViewById(R.id.fragment_gauge_list);
        progressBarLayout = (RelativeLayout)view.findViewById(R.id.fragment_gauge_progress_bar_layout);
        swipeRefreshLayout = (SwipeRefreshLayout)view.findViewById(R.id.fragment_gauge_swipe_refresh);





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
                GetGaugeDataParams params = new GetGaugeDataParams(mContext,view,listView,
                        progressBarLayout,swipeRefreshLayout,null);
                GetGaugeData getGaugeData = new GetGaugeData();
                getGaugeData.execute(params);
            }
        });

        final Bundle bundle = this.getArguments();

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
                    FavoriteParams params = new FavoriteParams(mContext,notificationSwitch);
                    RemoveFavorite task = new RemoveFavorite();
                    task.execute(params);

                }else{
                    if(GaugeApplication.myDBHelper.getFavoritesCount() < 10) {
                        favoriteButton.setSelected(true);
                        Log.d("fragGauge1", String.valueOf(gauge));
                        FavoriteParams params = new FavoriteParams(mContext, notificationSwitch);
                        AddFavorite task = new AddFavorite();
                        task.execute(params);
                    }else{
                        CharSequence text = getContext().getResources().getString(R.string.favorite_limit);
                        Toast toast = Toast.makeText(getContext(),text,Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
            }
        });







        //gaugeName

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()){
                    case R.id.action_settings:

                        /*Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);*/

                        /*if(bundle != null){
                            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                            SettingsContainerFragment settingsFragment = new SettingsContainerFragment();
                            settingsFragment.setArguments(bundle);
                            fragmentTransaction.replace(R.id.main_layout, settingsFragment, "settings_container_fragment").commit();
                        }else{
                            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                            fragmentTransaction.replace(R.id.main_layout, new SettingsContainerFragment(), "settings_container_fragment").commit();
                        }*/

                        Intent intent = new Intent(getActivity(),SettingsActivity.class);
                        startActivity(intent);





                        return true;

                    default:

                        return false;
                }

            }
        });
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
                Log.d("backpressed9,", String.valueOf(selectedGauge));
                fragmentTransaction.remove(fragmentManager.findFragmentByTag("gauge_fragment"));
                fragmentTransaction.commit();
                if(fragmentManager.getBackStackEntryCount() > 0){
                    fragmentManager.popBackStack();
                }


            }
        });



        progressBarLayout.setVisibility(View.VISIBLE);





        return view;
    }

    @Override
    public void onStart(){
        super.onStart();

        Log.d("FragmentGauge","onStart");
        GetGaugeDataParams params = new GetGaugeDataParams(mContext,view,listView,progressBarLayout,
                swipeRefreshLayout,null);
        GetGaugeData getGaugeData = new GetGaugeData();
        getGaugeData.execute(params);
    }

    private void enableNotifications(boolean enable){

        NotificationParams params = new NotificationParams(mContext,enable);
        EnableNotifications task = new EnableNotifications();
        task.execute(params);

    }

    private static class NotificationParams{
        Context context;
        boolean enable;
        private NotificationParams(Context context, boolean enable){
            this.context = context;
            this.enable = enable;
        }
    }

    private static class EnableNotifications extends AsyncTask<NotificationParams,Void,NotificationParams> {

        @Override
        protected NotificationParams doInBackground(NotificationParams... params){

            Context context = params[0].context;
            boolean enable = params[0].enable;
            GaugeApplication.myDBHelper.changeFavoriteNotificationState(gauge,enable);

            return new NotificationParams(context,enable);
        }

        @Override
        protected void onPostExecute(NotificationParams result){
            boolean enable = result.enable;
            Context context = result.context;
            String toastText = "";
            if(enable) {
                toastText = context.getResources().getString(R.string.notifications_enabled) + gauge.getGaugeName();
            }else{
                toastText = context.getResources().getString(R.string.notifications_disabled) + gauge.getGaugeName();
            }
            if(switchChangedByFavorite == 0) {
                Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_SHORT);
                toast.show();
            }
            switchChangedByFavorite = 0;
        }
    }

    private static class FavoriteParams{
        Context context;
        Switch notificationSwitch;

        private FavoriteParams(Context context, Switch notificationSwitch){
            this.context = context;
            this.notificationSwitch = notificationSwitch;
        }
    }

    private static class AddFavorite extends AsyncTask<FavoriteParams,Void,FavoriteParams> {

        @Override
        protected FavoriteParams doInBackground(FavoriteParams... params){

            Context context = params[0].context;
            Switch notificationSwitch = params[0].notificationSwitch;
            GaugeApplication.myDBHelper.addFavorite(gauge);
            Log.d("numFavorites",String.valueOf(GaugeApplication.myDBHelper.getFavoritesCount()));
            return new FavoriteParams(context,notificationSwitch);
        }

        @Override
        protected void onPostExecute(FavoriteParams result){

            Switch notificationSwitch = result.notificationSwitch;
            Context context = result.context;
            if(!notificationSwitch.isChecked()) {
                switchChangedByFavorite = 1;
            }
            else {
                switchChangedByFavorite = 0;
            }
            isFavorite = true;
            notificationSwitch.setChecked(true);

            String toastText = gauge.getGaugeName() + context.getResources().getString(R.string.add_favorite);
            Toast toast = Toast.makeText(context,toastText,Toast.LENGTH_SHORT);
            toast.show();




        }
    }


    private static class RemoveFavorite extends AsyncTask<FavoriteParams,Void,FavoriteParams>{

        @Override
        protected FavoriteParams doInBackground(FavoriteParams... params){

            Context context = params[0].context;
            Switch notificationSwitch = params[0].notificationSwitch;
            Log.d("fragGauge3",String.valueOf(gauge));
            GaugeApplication.myDBHelper.removeFavorite(gauge);
            Log.d("numFavorites",String.valueOf(GaugeApplication.myDBHelper.getFavoritesCount()));
            return new FavoriteParams(context,notificationSwitch);
        }

        @Override
        protected void onPostExecute(FavoriteParams result){

            Switch notificationSwitch = result.notificationSwitch;
            Context context = result.context;
            if(notificationSwitch.isChecked()) {
                switchChangedByFavorite = -1;
            }
            else {
                switchChangedByFavorite = 0;
            }
            isFavorite = false;
            notificationSwitch.setChecked(false);
            String toastText = gauge.getGaugeName() + context.getResources().getString(R.string.remove_favorite);
            Toast toast = Toast.makeText(context,toastText,Toast.LENGTH_SHORT);
            toast.show();




        }
    }

    private static class GetGaugeDataParams{
        Context context;
        View view;
        ListView listView;
        RelativeLayout progressLayout;
        SwipeRefreshLayout swipeRefreshLayout;
        GaugeReadParseObject gaugeReadParseObject;
        private GetGaugeDataParams(Context context, View view, ListView listView,
                                   RelativeLayout progressLayout, SwipeRefreshLayout swipeRefreshLayout,
                                   GaugeReadParseObject gaugeReadParseObject){
            this.context = context;
            this.view = view;
            this.listView = listView;
            this.progressLayout = progressLayout;
            this.swipeRefreshLayout = swipeRefreshLayout;
            this.gaugeReadParseObject = gaugeReadParseObject;

        }
    }

    private static class GetGaugeData extends AsyncTask<GetGaugeDataParams,Void,GetGaugeDataParams>{

        @Override
        protected void onPreExecute(){


        }

        @Override
        protected GetGaugeDataParams doInBackground(GetGaugeDataParams... params){

            GetGaugeDataParams getGaugeDataParams = params[0];
            Context context =getGaugeDataParams.context;
            View view = getGaugeDataParams.view;
            ListView listView = getGaugeDataParams.listView;
            RelativeLayout progress = getGaugeDataParams.progressLayout;
            SwipeRefreshLayout swipe = getGaugeDataParams.swipeRefreshLayout;
            GaugeData gaugeData = new GaugeData(gauge.getGaugeID());
            GaugeReadParseObject gaugeReadParseObject = gaugeData.getData();
            List<Datum> list = gaugeReadParseObject.getDatumList();
            gaugeReadParseObject.setDatumList(filterList(list,ONE_DAY));

            return new GetGaugeDataParams(context,view,listView,progress,swipe,gaugeReadParseObject);
        }

        @Override
        protected void onPostExecute(GetGaugeDataParams result){

            Context context = result.context;
            View view = result.view;
            GaugeReadParseObject gaugeReadParseObject = result.gaugeReadParseObject;
            ListView listView = result.listView;
            RelativeLayout progressBarLayout = result.progressLayout;
            SwipeRefreshLayout swipeRefreshLayout = result.swipeRefreshLayout;

            TextView gaugeNameTextView = (TextView)view.findViewById(R.id.fragment_gauge_name);
            TextView currentDateTextView = (TextView)view.findViewById(R.id.fragment_gauge_current_date);
            TextView currentStageTextView = (TextView)view.findViewById(R.id.fragment_gauge_current_stage);
            TextView floodWarningTextView = (TextView)view.findViewById(R.id.fragment_gauge_flood_warning);
            TextView highStageTextView = (TextView)view.findViewById(R.id.fragment_gauge_high_stage);
            TextView highDateTextView = (TextView)view.findViewById(R.id.fragment_gauge_high_date);
            TextView lowStageTextView = (TextView)view.findViewById(R.id.fragment_gauge_low_stage);
            TextView lowDateTextView = (TextView)view.findViewById(R.id.fragment_gauge_low_date);

            if(gaugeReadParseObject!=null && gaugeReadParseObject.getDatumList() !=null && gaugeReadParseObject.getDatumList().size() > 0){
                GaugeApplication gaugeApplication = new GaugeApplication();
                List<Datum>datumList = gaugeReadParseObject.getDatumList();
                String currentStageText = addUnits(context,gaugeReadParseObject.getDatumList().get(0).getPrimary());
                String currentDateText = convertDate(gaugeReadParseObject.getDatumList().get(0).getValid());
                DataBound dataBound = new DataBound(datumList);
                String highStageText = addUnits(context,dataBound.getHighStage());
                String lowStageText = addUnits(context,dataBound.getLowStage());
                String highDateText = convertDate(dataBound.getHighDate());
                String lowDateText = convertDate(dataBound.getLowDate());
                String gaugeName = gauge.getGaugeName();
                String floodWarning = getFloodWarning(context,gaugeReadParseObject.getSigstages(),gaugeReadParseObject.getDatumList().get(0).getPrimary());

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


                GaugeDataListAdapter gaugeDataListAdapter = new GaugeDataListAdapter(context,datumList);
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
                currentStageTextView.setText(context.getResources().getString(R.string.no_data_short));
                currentDateTextView.setText(context.getResources().getString(R.string.no_data));
                highStageTextView.setText(context.getResources().getString(R.string.no_data_short));
                lowStageTextView.setText(context.getResources().getString(R.string.no_data_short));

                if(listView !=null){
                    listView.setAdapter(null);
                }
                progressBarLayout.setVisibility(View.GONE);
                if(swipeRefreshLayout.isRefreshing()){
                    swipeRefreshLayout.setRefreshing(false);
                }
            }


        }

        private String addUnits(Context context, String waterHeight){

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String unitsPref = sharedPref.getString(SettingsFragment.KEY_PREF_UNITS, "0");

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
            if(datumList!=null) {
                for (int i = 0; i < datumList.size(); i++) {

                    Long dateInMillis = convertStringToDate(datumList.get(i).getValid()).getTime();
                    Long difference = currentDateMillis - dateInMillis;
                    Log.d("dateInListIssue2", "difference is: " + difference / 1000 / 60 / 60 + " day in millis is: " + DAY_IN_MILLIS / 1000 / 60 / 60);
                    if (difference <= DAY_IN_MILLIS) {
                        filteredList.add(datumList.get(i));
                        DateFormat formatter = new SimpleDateFormat("MMM dd h:mma");
                        Date current = new Date(currentDateMillis);
                        Date data = new Date(dateInMillis);
                        String dateString = formatter.format(current);
                        String dateString1 = formatter.format(data);


                        Log.d("dateInListIssue1", "current time: " + dateString + " time added: " + dateString1);
                    }
                }
            }

            return filteredList;



        }

        private List<Datum> filterTwoDays(List<Datum> datumList, Long currentDateMillis){

            List<Datum> filteredList = new ArrayList<Datum>();
            if(datumList!=null) {
                for (int i = 0; i < datumList.size(); i++) {

                    Long dateInMillis = convertStringToDate(datumList.get(i).getValid()).getTime();
                    Long difference = currentDateMillis - dateInMillis;
                    if (difference <= DAY_IN_MILLIS * 2) {
                        filteredList.add(datumList.get(i));
                    }
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




        private class DataBound{
            private List<Datum> datumList;
            private List<Datum> data;
            private List<Datum> list;

            private DataBound(List<Datum> datumList){
                this.datumList = datumList;
                list = new ArrayList<Datum>();
                list.addAll(this.datumList);
                data = sortDatumList(list);
            }

            private String getHighStage(){


                for(int i = data.size() - 1; i > -1;i--) {

                    String highStage = data.get(i).getPrimary();
                    if (parseStage(highStage) != BAD_DATA){
                        return highStage;
                    }
                }
                return "No Data";
            }
            private String getLowStage(){

                for(int i = 0; i < data.size();i++) {

                    String lowStage = data.get(i).getPrimary();
                    if (parseStage(lowStage) != BAD_DATA){
                        return lowStage;
                    }
                }
                return "No Data";

            }
            private String getHighDate(){

                for(int i = data.size() - 1; i > -1;i--) {

                    String highStage = data.get(i).getPrimary();
                    String highDate = data.get(i).getValid();
                    if (parseStage(highStage) != BAD_DATA){
                        return highDate;
                    }
                }
                return "No Data";
            }
            private String getLowDate(){

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

        private String getFloodWarning(Context context, Sigstages sigstages, String waterHeight){

            double actionDouble = 0.0;
            double minorDouble = 0.0;
            double majorDouble = 0.0;
            double moderateDouble = 0.0;
            double waterDouble = 0.0;

            if(sigstages.getMajor() == null && sigstages.getModerate() == null &&
                    sigstages.getFlood() == null && sigstages.getAction() == null){
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
                            return context.getResources().getString(R.string.major_flooding);
                        }
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }


                }

                if(sigstages.getModerate() !=null){
                    try{
                        moderateDouble = Double.parseDouble(sigstages.getModerate());
                        if(waterDouble >= moderateDouble){
                            return context.getResources().getString(R.string.moderate_flooding);
                        }
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }

                }
                if(sigstages.getFlood() !=null){

                    try{
                        minorDouble = Double.parseDouble(sigstages.getFlood());
                        if(waterDouble >= minorDouble){
                            return context.getResources().getString(R.string.minor_flooding);
                        }
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }

                }
                if(sigstages.getAction() !=null){

                    try{
                        actionDouble = Double.parseDouble(sigstages.getAction());
                        if(waterDouble >= actionDouble){
                            return context.getResources().getString(R.string.action_flooding);
                        }
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }

                }


            }
            return "";

        }

    }










}