package com.gregrussell.fenwickguageapp;

import java.util.List;

public class GaugeReadParseObject {

    private List<Datum> datumList;
    private Sigstages sigstages;

    public GaugeReadParseObject(){

    }
    public GaugeReadParseObject(List<Datum> datumList, Sigstages sigstages){
        this.datumList = datumList;
        this.sigstages  = sigstages;
    }

    public List<Datum> getDatumList() {
        return datumList;
    }

    public void setDatumList(List<Datum> datumList) {
        this.datumList = datumList;
    }

    public Sigstages getSigstages() {
        return sigstages;
    }

    public void setSigstages(Sigstages sigstages) {
        this.sigstages = sigstages;
    }
}
