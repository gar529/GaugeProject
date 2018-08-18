package com.gregrussell.fenwickguageapp;


import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FragmentSearch extends Fragment {

    private static Context mContext;
    private static int spinnerPosition;
    private static int[] radiusMiles = {5,10,20,50};
    private static Location myLocation;
    private static int distanceAway;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){

        View view = inflater.inflate(R.layout.fragment_search_layout, container,false);
        mContext = getContext();
        myLocation = this.getArguments().getParcelable("MY_LOCATION");
        LinearLayout layoutContainer = (LinearLayout)view.findViewById(R.id.fragment_search_container);
        Spinner radiusSpinner = (Spinner)view.findViewById(R.id.search_radius_spinner);

        Log.d("searchFragment1", String.valueOf(myLocation));

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(mContext, R.layout.radius_spinner_layout);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        for(int i : radiusMiles){
            spinnerAdapter.add(i+"mi");
        }
        final Button searchButton = (Button)view.findViewById(R.id.fragment_search_button);
        final EditText searchLocation = (EditText) view.findViewById(R.id.search_location_edit_text);
        if(myLocation !=null){
            Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
            try {
                List<Address> address = geocoder.getFromLocation(myLocation.getLatitude(), myLocation.getLongitude(), 1);
                String locationText = address.get(0).getLocality() + ", " + address.get(0).getAdminArea() + ", " + address.get(0).getPostalCode();
                searchLocation.setText(locationText);
            }catch (IOException e){
                e.printStackTrace();
            }

        }else{
            searchButton.setVisibility(View.GONE);
        }



        layoutContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchLocation.clearFocus();
            }
        });
        radiusSpinner.setAdapter(spinnerAdapter);

        radiusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                Log.d("spinner1", "int i: " + i);
                Log.d("spinner2", "Selected: " + radiusMiles[i]+"mi");
                distanceAway =radiusMiles[i];

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        searchLocation.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(verifyLocation(searchLocation.getText().toString())!=null) {
                    Address address = verifyLocation(searchLocation.getText().toString());
                    String result = address.getLocality() + ", " +
                            address.getAdminArea() + " " +
                            address.getPostalCode();
                    searchLocation.setText(result);
                    searchButton.setVisibility(View.VISIBLE);
                }else{
                    if(searchButton.getVisibility() != View.GONE){
                        searchButton.setVisibility(View.GONE);
                    }
                }
            }
        });


        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(verifyLocation(searchLocation.getText().toString())!=null) {
                    Address address = verifyLocation(searchLocation.getText().toString());
                    String result = address.getLocality() + ", " +
                            address.getAdminArea() + " " +
                            address.getPostalCode();
                    searchLocation.setText(result);
                    searchButton.setVisibility(View.VISIBLE);
                    Log.d("Searchfragment4", String.valueOf(address));
                    myLocation = null;
                    ClosestGaugeInputParams params = new ClosestGaugeInputParams(distanceAway,address);
                    ClosestGauge task = new ClosestGauge();
                    task.execute(params);



                }else{
                    if(searchButton.getVisibility() != View.GONE){
                        searchButton.setVisibility(View.GONE);
                    }
                }
            }
        });




        return view;

    }

    Address verifyLocation(String location){

        Address address = null;
        Address testedAddress = null;
        Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
        String result = null;

        try{

            List<Address> addressList = geocoder.getFromLocationName(location,1);
            Log.d("searchAddress2",addressList.size() + " " + String.valueOf(addressList));
            if(addressList.size() > 0) {
                address = addressList.get(0);
                if(address.getCountryCode().equals("US")) {
                    List<Address> mAddressList = new ArrayList<Address>();
                    mAddressList =geocoder.getFromLocation(address.getLatitude(),address.getLongitude(),1);
                    testedAddress = mAddressList.get(0);

                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }

        Log.d("searchAddress3", "result is: " + String.valueOf(address));
        return testedAddress;

    }


    private class ClosestGauge extends AsyncTask<ClosestGaugeInputParams, Void, ClosestGaugeOutputParams> {


        @Override
        protected ClosestGaugeOutputParams doInBackground(ClosestGaugeInputParams... params) {


            ClosestGaugeInputParams inputParams = params[0];
            Address myAddress = inputParams.address;
            Location location = new Location("");
            if (myLocation == null) {

                location.setLatitude(myAddress.getLatitude());
                location.setLongitude(myAddress.getLongitude());
            } else {
                location = myLocation;
            }

            ClosestGaugeOutputParams outputParams = new ClosestGaugeOutputParams(getAllGauges(), location);

            return findClosestGaugeByGPS(outputParams);
        }

        @Override
        protected void onPostExecute(ClosestGaugeOutputParams result) {

            Log.d("locations", "number of gauges within 5 miles: " + result.gaugeList.size());
            Log.d("locations", "locations that are within 5 miles: ");
            {
                for (int i = 0; i < result.gaugeList.size(); i++) {
                    Log.d("locations", result.gaugeList.get(i).getGaugeName() + " " + result.gaugeList.get(i).getGaugeID());
                }

                ArrayList<Gauge> myList = new ArrayList<Gauge>();
                myList.addAll(result.gaugeList);
                Log.d("locations10", "myList size: " + myList.size());
                Intent intent = new Intent(mContext, MainFragActivity.class);
                //intent.putExtra("LIST_OF_RESULTS", myList);
                intent.putExtra("MY_LOCATION", result.location);
                intent.putExtra("DISTANCE", distanceAway);
                startActivity(intent);

            }
        }


    }

    private static class ClosestGaugeInputParams{
        int distanceAway;
        Address address;

        private ClosestGaugeInputParams(int distanceAway, Address address){
            this.distanceAway = distanceAway;
            this.address = address;
        }
    }


    private static class ClosestGaugeOutputParams{
        List<Gauge> gaugeList;
        Location location;

        private ClosestGaugeOutputParams(List<Gauge> gaugeList, Location location){
            this.gaugeList = gaugeList;
            this.location = location;
        }
    }

    private List<Gauge> getAllGauges(){
        DataBaseHelperGauges myDBHelper = new DataBaseHelperGauges(mContext);
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
        return myDBHelper.getAllGauges();
    }

    private ClosestGaugeOutputParams findClosestGaugeByGPS(ClosestGaugeOutputParams params){

        Log.d("locations", "list size " + params.gaugeList.size());
        GetLocations getLocations = new GetLocations(params.location,params.gaugeList);
        List<Gauge> locationList = getLocations.getClosestGauges(100);
        ClosestGaugeOutputParams newParams = new ClosestGaugeOutputParams(locationList,params.location);
        return newParams;
    }


}
