package com.gregrussell.fenwickguageapp;

import java.io.Serializable;

public class GaugeFragParams{

    private Gauge gauge;
    private boolean isFavorite, isNotifiable;

    public GaugeFragParams(Gauge gauge, boolean isFavorite, boolean isNotifiable){

        this.gauge = gauge;
        this.isFavorite = isFavorite;
        this.isNotifiable = isNotifiable;
    }

    public Gauge getGauge() {
        return gauge;
    }

    public void setGauge(Gauge gauge) {
        this.gauge = gauge;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public boolean isNotifiable() {
        return isNotifiable;
    }

    public void setNotifiable(boolean notifiable) {
        isNotifiable = notifiable;
    }
}
