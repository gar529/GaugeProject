package com.gregrussell.fenwickguageapp;

import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.ResourceBundle;

public class GaugeData {

    private String gaugeID;

    public GaugeData(String gaugeID){
        this.gaugeID = gaugeID;
    }


    public GaugeReadParseObject getData(){

        GaugeReadParseObject gaugeReadParseObject = new GaugeReadParseObject();

        for(int i = 0; i < 3;i++) {
            try {
                gaugeReadParseObject = readGauge(gaugeID);


            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
        }

        return gaugeReadParseObject;


    }

    private GaugeReadParseObject readGauge(String gaugeID) throws IOException, XmlPullParserException {
        String urlString = "https://water.weather.gov/ahps2/hydrograph_to_xml.php?gage=" + gaugeID;
        InputStream stream = null;
        // Instantiate the parser
        GaugeReadingXMLParser gaugeXmlParser = new GaugeReadingXMLParser();

        GaugeReadParseObject gaugeReadParseObject;
        try {
            stream = downloadUrl(urlString);
            gaugeReadParseObject = gaugeXmlParser.parse(stream);
            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        }
        finally {
            if (stream != null) {
                stream.close();
            }
        }
        //Log.d("readGauge","first datum is " + datums.get(0).getPrimary());
        return gaugeReadParseObject;
    }


    private InputStream downloadUrl(String urlString) throws IOException {
        Log.d("urlString",urlString);
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        Log.d("gaugeDataCache"," " + conn.getDefaultUseCaches() + " " + conn.getUseCaches());
        conn.setDefaultUseCaches(false);
        conn.setUseCaches(false);
        Log.d("gaugeDataCache2"," " + conn.getDefaultUseCaches() + " " + conn.getUseCaches());
        conn.setReadTimeout(10000 /* milliseconds */);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // Starts the query
        conn.connect();
        return conn.getInputStream();

    }



}
