package com.gregrussell.fenwickguageapp;

public class Datum {

    private String valid;
    private String primary;

    public Datum(){

    }

    public Datum(String valid, String primary){
        this.valid = valid;
        this.primary = primary;
    }

    public String getValid(){
        return this.valid;
    }

    public void setValid(String valid){
        this.valid = valid;
    }

    public String getPrimary(){
        return this.primary;
    }

    public void setPrimary(String primary){
        this.primary = primary;
    }
}
