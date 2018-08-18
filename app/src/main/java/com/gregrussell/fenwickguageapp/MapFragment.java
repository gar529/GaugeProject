package com.gregrussell.fenwickguageapp;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    Context myContext = this.getActivity();
    private GoogleMap mMap;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        //getChildFragmentManager().beginTransaction().add(R.id.fragment_activity,mapFragment);
        return (ViewGroup)inflater.inflate(R.layout.map_fragment, container, false);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng myLocation = new LatLng(38.796943,-77.071622);
        LatLng farthestLocation = new LatLng(38.830833,-77.134722);
        LatLngBounds myView = new LatLngBounds(myLocation,farthestLocation);
        LatLng sydney = new LatLng(-34, 151);

        Marker myLocationMarker = mMap.addMarker(new MarkerOptions().position(myLocation).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).title("My Location"));
        mMap.addMarker(new MarkerOptions().position(farthestLocation));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
        //zoom levels 5 miles - 13 20miles - 10, 50mils - 8
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation,10));
    }
}
