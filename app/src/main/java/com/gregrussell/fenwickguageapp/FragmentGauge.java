package com.gregrussell.fenwickguageapp;

import android.content.Context;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import java.io.IOException;

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

        RelativeLayout view1 = (RelativeLayout)view.findViewById(R.id.action_gauge_frag_switch);
        //RelativeLayout view2 = (RelativeLayout)view.findViewById(R.id.action_gauge_frag_favorite);
        notificationSwitch = ((RelativeLayout)view.findViewById(R.id.action_gauge_frag_switch)).findViewById(R.id.switch_layout_switch);
        favoriteButton = ((RelativeLayout)view.findViewById(R.id.action_gauge_frag_favorite)).findViewById(R.id.gauge_frag_favorite_button);

        MenuItem item = toolbar.getMenu().findItem(R.id.action_gauge_frag_favorite);

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


}
