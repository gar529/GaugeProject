package com.gregrussell.fenwickguageapp;

import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;


import java.io.IOException;
import java.util.List;

public class FragmentFavorites extends Fragment {

    private ListView listView;
    LoadList task;
    SwipeRefreshLayout swipeRefresh;






    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle onSavedInstanceState){


        View view = inflater.inflate(R.layout.fragment_favortes_layout, container,false);

        ImageView backButton = (ImageView)view.findViewById(R.id.back_button_favorites);
        backButton.setClickable(true);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                getActivity().getSupportFragmentManager().beginTransaction().remove(getActivity().getSupportFragmentManager().findFragmentByTag("favorite_fragment")).commit();
                if(getActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            }
        });

        listView = (ListView)view.findViewById(R.id.favorite_list_view);
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
}
