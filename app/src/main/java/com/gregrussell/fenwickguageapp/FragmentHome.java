package com.gregrussell.fenwickguageapp;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class FragmentHome extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){

        View view = inflater.inflate(R.layout.fragment_home_layout, container,false);
        final Location myLocation = this.getArguments().getParcelable("MY_LOCATION");


        /*Button searchButton = (Button)view.findViewById(R.id.home_fragment_search_button);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("click","click");
                Bundle bundle = new Bundle();
                bundle.putParcelable("MY_LOCATION",myLocation);
                Fragment searchFragment = new FragmentSearch();
                searchFragment.setArguments(bundle);
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.home_fragment_container, searchFragment,"search_fragment");
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        Button favoriteButton = (Button)view.findViewById(R.id.home_fragment_favorite_button);
        favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("click","click");
                Fragment favoritesFragment = new FragmentFavorites();
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.home_fragment_container, favoritesFragment,"search_fragment");
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });*/
        return view;
    }
}
