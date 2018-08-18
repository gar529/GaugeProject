package com.gregrussell.fenwickguageapp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SettingsContainerFragment extends Fragment {


    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, Bundle onSavedInstanceState) {


        Log.d("FragmentGaugeOnCreate", "onCreate");
        View view = inflater.inflate(R.layout.settings_layout, container, false);

        final Bundle bundle = this.getArguments();
        Toolbar toolbar = (Toolbar)view.findViewById(R.id.settings_toolbar);
        toolbar.setNavigationIcon(getContext().getResources().getDrawable(R.drawable.baseline_arrow_back_black));
        toolbar.setTitle(getContext().getResources().getString(R.string.settings_title));

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                if(fragmentManager.findFragmentByTag("fragment_gauge") !=null){
                    Log.d("settings","not null");
                    fragmentTransaction.replace(R.id.main_layout,fragmentManager.findFragmentByTag("fragment_gauge"),"fragment_gauge");
                }else{
                    fragmentManager.popBackStack();
                    FragmentGauge fragmentGauge = new FragmentGauge();
                    fragmentGauge.setArguments(bundle);
                    fragmentTransaction.replace(R.id.main_layout,fragmentGauge,"fragment_gauge");
                }

                fragmentTransaction.commit();


            }
        });

        //FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        //FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        //fragmentTransaction.add(R.id.settings_fragment_container, new SettingsFragment(), "settings_fragment").commit();

        return view;
    }

}
