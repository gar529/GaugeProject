package com.gregrussell.fenwickguageapp;

import android.location.Location;
import android.util.Log;

import com.gregrussell.fenwickguageapp.WeatherXmlParser.Gauge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GetLocations {

    Location currentLocation;
    List<Gauge> gaugeList;
    final double MILE_CONVERTER = .000621371;

    public GetLocations(Location currentLocation, List<Gauge> gaugeList){

        this.currentLocation = currentLocation;
        this.gaugeList = gaugeList;

    }




    public List<Gauge> getClosestGauges(int distance){

        Log.d("getLocation1","start getting gauges");
        List<Gauge> closetGauges = new ArrayList<Gauge>();

        //Log.d("locations2", "list size " + gaugeList.size());
        if(gaugeList.size() > 0) {
            for (int i = 0; i < gaugeList.size(); i++) {

                Location gaugeLocation = new Location("");
                Gauge currentGauge = gaugeList.get(i);
                gaugeLocation.setLatitude(currentGauge.getGaugeLatitude());
                gaugeLocation.setLongitude(currentGauge.getGaugeLongitude());
                double distanceAway = currentLocation.distanceTo(gaugeLocation) * MILE_CONVERTER;
                //Log.d("locations2", "distance " + currentLocation.distanceTo(gaugeLocation));
                if (distanceAway  <= distance) {
                    currentGauge.setDistance(distanceAway);
                    closetGauges.add(currentGauge);
                }
            }
        }
        //Log.d("locations", "unsorted locations that are within 5 miles: ");



        Collections.sort(closetGauges, new Comparator<Gauge>() {
            @Override
            public int compare(Gauge o1, Gauge o2) {
                int i;
                if(o1.getDistance() < o2.getDistance()){
                    i = -1;
                } else if(o1.getDistance() > o2.getDistance()){
                    i = 1;
                }else {
                    i=0;
                }
                return i;
            }
        });

        //Log.d("locations", "sorted locations that are within 5 miles: ");



        Log.d("getlocation2","finished getting gauges");
        return closetGauges;
    }

    public List<Gauge> getClosestGauges(int startDistance, int endDistance){

        Log.d("getLocation1","start getting gauges");
        List<Gauge> closetGauges = new ArrayList<Gauge>();

        //Log.d("locations2", "list size " + gaugeList.size());
        if(gaugeList.size() > 0) {
            for (int i = 0; i < gaugeList.size(); i++) {

                Location gaugeLocation = new Location("");
                Gauge currentGauge = gaugeList.get(i);
                gaugeLocation.setLatitude(currentGauge.getGaugeLatitude());
                gaugeLocation.setLongitude(currentGauge.getGaugeLongitude());
                double distanceAway = currentLocation.distanceTo(gaugeLocation) * MILE_CONVERTER;
                //Log.d("locations2", "distance " + currentLocation.distanceTo(gaugeLocation));
                if (distanceAway  > startDistance && distanceAway <= endDistance) {
                    currentGauge.setDistance(distanceAway);
                    closetGauges.add(currentGauge);
                }
            }
        }
        //Log.d("locations", "unsorted locations that are within 5 miles: ");



        Collections.sort(closetGauges, new Comparator<Gauge>() {
            @Override
            public int compare(Gauge o1, Gauge o2) {
                int i;
                if(o1.getDistance() < o2.getDistance()){
                    i = -1;
                } else if(o1.getDistance() > o2.getDistance()){
                    i = 1;
                }else {
                    i=0;
                }
                return i;
            }
        });

        //Log.d("locations", "sorted locations that are within 5 miles: ");



        Log.d("getlocation2","finished getting gauges");
        return closetGauges;
    }




}
