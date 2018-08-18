package com.gregrussell.fenwickguageapp;

public class RSSParsedObj {

    private String action,minor,moderate,major,stage,time;

    public RSSParsedObj(){

    }

    public RSSParsedObj(String action, String minor, String moderate, String major, String stage,
                        String time){
        this.action = action;
        this.minor = minor;
        this.moderate = moderate;
        this.major = major;
        this.stage = stage;
        this.time = time;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMinor() {
        return minor;
    }

    public void setMinor(String minor) {
        this.minor = minor;
    }

    public String getModerate() {
        return moderate;
    }

    public void setModerate(String moderate) {
        this.moderate = moderate;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
