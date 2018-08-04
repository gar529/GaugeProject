package com.gregrussell.fenwickguageapp;

import java.io.Serializable;

public class Gauge implements Serializable {
    private String gaugeURL;
    private String gaugeName;
    private String gaugeID;
    private double gaugeLatitude;
    private double gaugeLongitude;
    private String gaugeAddress;
    private double distance;

    public Gauge(String gaugeURL, String gaugeName, String gaugeID, double gaugeLatitude,
                 double gaugeLongitude, String gaugeAddress){
        this.gaugeURL = gaugeURL;
        this.gaugeName = gaugeName;
        this.gaugeID = gaugeID;
        this.gaugeLatitude = gaugeLatitude;
        this.gaugeLongitude = gaugeLongitude;
        this.gaugeAddress = gaugeAddress;
    }

    public Gauge(String gaugeURL, String gaugeName, String gaugeID, double gaugeLatitude,
                 double gaugeLongitude, String gaugeAddress, double distance){
        this.gaugeURL = gaugeURL;
        this.gaugeName = gaugeName;
        this.gaugeID = gaugeID;
        this.gaugeLatitude = gaugeLatitude;
        this.gaugeLongitude = gaugeLongitude;
        this.gaugeAddress = gaugeAddress;
        this.distance = distance;
    }

    public String getGaugeURL(){
        return this.gaugeURL;
    }
    public void setGaugeURL(String url){
        this.gaugeURL = url;
    }
    public String getGaugeName(){
        return this.gaugeName;
    }
    public void setGaugeName(String name){
        this.gaugeName = name;
    }
    public String getGaugeID(){
        return this.gaugeID;
    }
    public void setGaugeID(String id){
        this.gaugeID = id;
    }
    public double getGaugeLatitude(){
        return this.gaugeLatitude;
    }
    public void setGaugeLatitude(double lat){
        this.gaugeLatitude = lat;
    }
    public double getGaugeLongitude(){
        return this.gaugeLongitude;
    }
    public void setGaugeLongitude(double lon){
        this.gaugeLongitude = lon;
    }
    public String getGaugeAddress(){
        return this.gaugeAddress;
    }
    public void setGaugeAddress(String address){
        this.gaugeAddress = address;
    }
    public double getDistance(){
        return this.distance;
    }
    public void setDistance(double distance){
        this.distance = distance;
    }

}