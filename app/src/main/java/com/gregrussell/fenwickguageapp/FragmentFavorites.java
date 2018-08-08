package com.gregrussell.fenwickguageapp;

import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;


import java.io.IOException;
import java.util.List;

public class FragmentFavorites extends Fragment {

    private ListView listView;
    LoadList task;
    SwipeRefreshLayout swipeRefresh;





    @Override
    public void onResume(){
        super.onResume();

        if(task.getStatus() != AsyncTask.Status.PENDING && task.getStatus() != AsyncTask.Status.RUNNING){
            task = new LoadList();
            task.execute();
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle onSavedInstanceState){


        View view = inflater.inflate(R.layout.fragment_favortes_layout, container,false);




        Toolbar toolbar = (Toolbar)view.findViewById(R.id.favorites_toolbar);
        toolbar.inflateMenu(R.menu.favorite_fragment_toolbar_menu);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return false;
            }
        });

        toolbar.setNavigationIcon(android.support.v7.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                getActivity().getSupportFragmentManager().beginTransaction().remove(getActivity().getSupportFragmentManager().findFragmentByTag("favorite_fragment")).commit();
                if(getActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            }
        });
        toolbar.setTitle(R.string.favorites);

        listView = (ListView)view.findViewById(R.id.favorite_list_view);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {


                Log.d("favoritesList","itemClick");
                Gauge gauge = (Gauge)view.getTag();
                Log.d("favoritesList",gauge.getGaugeName());



                task.cancel(true);
                LoadGaugeFragment loadGaugeFragment = new LoadGaugeFragment();
                loadGaugeFragment.execute(gauge);

            }
        });
        swipeRefresh = (SwipeRefreshLayout)view.findViewById(R.id.swipe_refresh);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                task = new LoadList();
                task.execute();

            }
        });

        task = new LoadList();
        task.execute();

        return view;
    }

    private class LoadList extends AsyncTask <Void,Void,List<Gauge>> {

        @Override
        protected List<Gauge> doInBackground(Void... params){

            DataBaseHelperGauges myDBHelper = new DataBaseHelperGauges(getContext());
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




            return myDBHelper.getAllFavorites();
        }
        @Override
        protected void onPostExecute(List<Gauge> result){

            FavoritesListViewAdapter adapter = new FavoritesListViewAdapter(getContext(),result);
            listView.setAdapter(null);
            listView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
            if(swipeRefresh.isRefreshing()){
                swipeRefresh.setRefreshing(false);
            }

        }

    }

    private class LoadGaugeFragment extends AsyncTask<Gauge,Void,GaugeFragParams>{

        @Override
        protected GaugeFragParams doInBackground(Gauge... params){


            DataBaseHelperGauges myDBHelper = new DataBaseHelperGauges(getContext());
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
            Gauge gauge = params[0];
            boolean isFavorite = myDBHelper.isFavorite(gauge);
            Log.d("fragFave",String.valueOf(isFavorite));
            int notificationState = myDBHelper.getFavoriteNotificationState(gauge);
            boolean isNotifiable = true;
            if(notificationState == 0){
                isNotifiable = false;
            }



            GaugeFragParams gaugeFragParams = new GaugeFragParams(gauge,isFavorite,isNotifiable);
            return gaugeFragParams;
        }

        @Override
        protected void onPostExecute(GaugeFragParams params){

            Bundle bundle = new Bundle();
            bundle.putSerializable("gauge",params.getGauge());
            bundle.putBoolean("isFavorite",params.isFavorite());
            bundle.putBoolean("isNotifiable",params.isNotifiable());
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            FragmentGauge fragmentGauge = new FragmentGauge();
            fragmentGauge.setArguments(bundle);
            fragmentTransaction.replace(R.id.main_layout,fragmentGauge,"gauge_fragment").addToBackStack("Tag");
            fragmentTransaction.commit();

        }
    }
}
