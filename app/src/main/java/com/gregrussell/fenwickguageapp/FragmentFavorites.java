package com.gregrussell.fenwickguageapp;

import android.content.Context;
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


    private LoadList task;
    private Gauge selectedGauge;




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

        Bundle bundle = this.getArguments();
        if(bundle != null){
            selectedGauge = (Gauge)bundle.get("selected_gauge");
        }



        Toolbar toolbar = (Toolbar)view.findViewById(R.id.favorites_toolbar);
        toolbar.inflateMenu(R.menu.favorite_fragment_toolbar_menu);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return false;
            }
        });

        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.baseline_arrow_back_black));
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
                fragmentTransaction.remove(fragmentManager.findFragmentByTag("favorite_fragment"));
                fragmentTransaction.commit();
                if(fragmentManager.getBackStackEntryCount() > 0){
                    fragmentManager.popBackStack();
                }

            }
        });
        toolbar.setTitle(R.string.favorites);

        ListView listView = (ListView)view.findViewById(R.id.favorite_list_view);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {


                Log.d("favoritesList","itemClick");
                Gauge gauge = (Gauge)view.getTag();
                Log.d("favoritesList",gauge.getGaugeName());



                task.cancel(true);
                LoadGaugeParams params = new LoadGaugeParams(getActivity().getSupportFragmentManager(),gauge,null);
                LoadGaugeFragmentAsync loadGaugeFragment = new LoadGaugeFragmentAsync();
                loadGaugeFragment.execute(params);

            }
        });
        SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout)view.findViewById(R.id.swipe_refresh);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                task = new LoadList();
                task.execute();

            }
        });

        LoadListParams params = new LoadListParams(getContext(),listView,swipeRefresh,null);
        task = new LoadList();
        task.execute(params);

        return view;
    }

    private static class LoadListParams{
        Context context;
        ListView listView;
        SwipeRefreshLayout swipeRefresh;
        List<Gauge> gauges;
        private LoadListParams(Context context, ListView listView, SwipeRefreshLayout swipeRefresh, List<Gauge> gauges){
            this.context = context;
            this.listView = listView;
            this.swipeRefresh = swipeRefresh;
            this.gauges = gauges;
        }
    }

    private static class LoadList extends AsyncTask <LoadListParams,Void,LoadListParams> {

        @Override
        protected LoadListParams doInBackground(LoadListParams... params){

            Context context = params[0].context;
            ListView listView = params[0].listView;
            SwipeRefreshLayout swipe = params[0].swipeRefresh;
            List<Gauge>gauges = GaugeApplication.myDBHelper.getAllFavorites();
            return new LoadListParams(context,listView,swipe,gauges);
        }
        @Override
        protected void onPostExecute(LoadListParams result){

            Context context = result.context;
            ListView listView = result.listView;
            SwipeRefreshLayout swipeRefresh = result.swipeRefresh;
            FavoritesListViewAdapter adapter = new FavoritesListViewAdapter(context,result.gauges);
            listView.setAdapter(null);
            listView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
            if(swipeRefresh.isRefreshing()){
                swipeRefresh.setRefreshing(false);
            }

        }

    }

    private static class LoadGaugeParams{

        FragmentManager fragmentManager;
        Gauge gauge;
        GaugeFragParams params;
        private LoadGaugeParams(FragmentManager fragmentManager, Gauge gauge, GaugeFragParams params){
            this. fragmentManager = fragmentManager;
            this.gauge = gauge;
            this.params = params;
        }
    }

    private static class LoadGaugeFragmentAsync extends AsyncTask<LoadGaugeParams,Void,LoadGaugeParams>{

        @Override
        protected LoadGaugeParams doInBackground(LoadGaugeParams... params){

            FragmentManager fragmentManager = params[0].fragmentManager;

            Gauge gauge = params[0].gauge;
            boolean isFavorite = GaugeApplication.myDBHelper.isFavorite(gauge);
            Log.d("fragFave",String.valueOf(isFavorite));
            int notificationState = GaugeApplication.myDBHelper.getFavoriteNotificationState(gauge);
            boolean isNotifiable = true;
            if(notificationState == 0){
                isNotifiable = false;
            }



            GaugeFragParams gaugeFragParams = new GaugeFragParams(gauge,isFavorite,isNotifiable);
            return new LoadGaugeParams(fragmentManager,gauge,gaugeFragParams);
        }

        @Override
        protected void onPostExecute(LoadGaugeParams result){

            GaugeFragParams params =result.params;
            Bundle bundle = new Bundle();
            bundle.putSerializable("gauge",params.getGauge());
            bundle.putBoolean("isFavorite",params.isFavorite());
            bundle.putBoolean("isNotifiable",params.isNotifiable());
            FragmentManager fragmentManager = result.fragmentManager;
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            FragmentGauge fragmentGauge = new FragmentGauge();
            fragmentGauge.setArguments(bundle);
            fragmentTransaction.replace(R.id.main_layout,fragmentGauge,"gauge_fragment").addToBackStack("Tag");
            fragmentTransaction.commit();

        }
    }
}
