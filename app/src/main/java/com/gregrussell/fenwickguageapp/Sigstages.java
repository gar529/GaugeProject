package com.gregrussell.fenwickguageapp;

public class Sigstages {

    private String action;
    private String flood;
    private String moderate;
    private String major;

    public Sigstages(){

    }

    public Sigstages(String action, String flood, String moderate, String major){

        this.action = action;
        this.flood = flood;
        this.moderate = moderate;
        this.major = major;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public String getFlood(){
        return this.flood;
    }
    public void setFlood(String flood){
        this.flood = flood;
    }

    public String getModerate(){
        return this.moderate;
    }
    public void setModerate(String moderate){
        this.moderate = moderate;
    }

    public String getMajor(){
        return this.major;
    }

    public void setMajor(String major){
        this.major = major;
    }
}
