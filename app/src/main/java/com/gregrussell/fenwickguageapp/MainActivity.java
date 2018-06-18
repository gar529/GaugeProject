package com.gregrussell.fenwickguageapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.widget.TextView;
import com.gregrussell.fenwickguageapp.WeatherXmlParser.Gauge;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class MainActivity extends Activity {

    public static final String WIFI = "Wi-Fi";
    public static final String ANY = "Any";
    //private static final String MY_URL = "https://water.weather.gov/ahps2/hydrograph_to_xml.php?gage=hpkv2&output=xml";

    private static final String MY_URL = "https://raw.githubusercontent.com/gar529/GaugeProject/master/GaugeProject/xmlData.xml";

    // Whether there is a Wi-Fi connection.
    private static boolean wifiConnected = true;
    // Whether there is a mobile connection.
    private static boolean mobileConnected = false;
    // Whether the display should be refreshed.
    public static boolean refreshDisplay = true;
    public static String sPref = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        loadPage();

    }



    public void loadPage() {

        Log.d("xmlData", "any is: " + ANY);
        if((ANY.equals(sPref)) && (wifiConnected || mobileConnected)) {
            new DownloadXmlTask().execute(MY_URL);
        }
        else if ((WIFI.equals(sPref)) && (wifiConnected)) {
            new DownloadXmlTask().execute(MY_URL);
        } else {
            Log.d("xmlData", "error");
            new DownloadXmlTask().execute(MY_URL);
        }
    }


    // Implementation of AsyncTask used to download XML feed from stackoverflow.com.
    private class DownloadXmlTask extends AsyncTask<String, Void, List<Gauge>> {
        @Override
        protected List<Gauge> doInBackground(String... urls) {
            Log.d("xmlData", "doinbackground");
            try {
                return loadXmlFromNetwork(urls[0]);
            } catch (IOException e) {
                //return getResources().getString(R.string.connection_error);
                return null;
            } catch (XmlPullParserException e) {
                //return getResources().getString(R.string.xml_error);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Gauge> result) {
            setContentView(R.layout.activity_main);
            //Displays the HTML string in the UI via a WebView
            //WebView myWebView = (WebView) findViewById(R.id.webview);
            //myWebView.loadData(result, "text/html", null);

            TextView waterHeight = (TextView)findViewById(R.id.current_water_height);
            TextView time = (TextView)findViewById(R.id.current_time);
            Log.d("xmlData", "size of result is: " + result.size() + "gaugeName is: " +
                    result.get(0).getGaugeName() + " gaugeID is: " + result.get(0).getGaugeID() +
                    "gaugeURL is: " + result.get(0).getGaugeURL() + "gaugeLat is: " +
                    result.get(0).getGaugeLatitude() + "gaugeLon is: " + result.get(0).getGaugeLongitude());
            waterHeight.setText((result.get(0).getGaugeID())+"ft");
            /*String dateString = result.get(0).gaugeName;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm");
            Date convertedDate = new Date();
            try {
                convertedDate = dateFormat.parse(dateString);


            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            Date date = Calendar.getInstance().getTime();
            DateFormat formatter = new SimpleDateFormat("MMM dd h:mmaa");
            TimeZone tz = TimeZone.getDefault();
            Date now = new Date();
            int offsetFromUtc = tz.getOffset(now.getTime());
            Log.d("xmlData", "timezone offset is: " + offsetFromUtc);

            Log.d("xmlData", "date is: " + date.getTime());
            long offset = convertedDate.getTime() + offsetFromUtc;
            Log.d("xmlData", "date with offset is: " + offset);
            Date correctTZDate = new Date(offset);



            Log.d("xmlData","correctTZDate is: " + correctTZDate.getTime());
            time.setText(formatter.format(correctTZDate));*/
            time.setText(result.get(0).getGaugeName());

            ClosestGauge closetTask = new ClosestGauge();
            closetTask.execute(result);

            }

        }



    // Uploads XML from stackoverflow.com, parses it, and combines it with
// HTML markup. Returns HTML string.
    private List loadXmlFromNetwork(String urlString) throws XmlPullParserException, IOException {
        InputStream stream = null;
        // Instantiate the parser
        WeatherXmlParser weatherXmlParser = new WeatherXmlParser();
        List<Gauge> gaugeList = null;
        String gaugeName = null;
        String gaugeID = null;
        String summary = null;
        Calendar rightNow = Calendar.getInstance();
        DateFormat formatter = new SimpleDateFormat("MMM dd h:mmaa");

        // Checks whether the user set the preference to include summary text
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean pref = sharedPrefs.getBoolean("summaryPref", false);

        /*StringBuilder htmlString = new StringBuilder();
        htmlString.append("<h3>" + getResources().getString(R.string.page_title) + "</h3>");
        htmlString.append("<em>" + getResources().getString(R.string.updated) + " " +
                formatter.format(rightNow.getTime()) + "</em>");*/

        try {
            stream = downloadUrl(urlString);
            gaugeList = weatherXmlParser.parse(stream);
            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (stream != null) {
                stream.close();
            }
        }

        // StackOverflowXmlParser returns a List (called "entries") of Entry objects.
        // Each Entry object represents a single post in the XML feed.
        // This section processes the entries list to combine each entry with HTML markup.
        // Each entry is displayed in the UI as a link that optionally includes
        // a text summary.
        /*for (WeatherXmlParser.Datum datum : datums) {
            htmlString.append("<p><a href='");
            htmlString.append(datum.gaugeName);

            htmlString.append("'>" + datum.gaugeName + "</a></p>");
            gaugeName = datum.gaugeName;
            summary = summary + gaugeName;
            // If the user set the preference to include summary text,
            // adds it to the display.
            if (pref) {
                htmlString.append(datum.gaugeID);
                gaugeID = datum.gaugeID;
                summary = summary + gaugeID;
            }
        }*/
        //return htmlString.toString();
        return gaugeList;
    }

    // Given a string representation of a URL, sets up a connection and gets
// an input stream.
    private InputStream downloadUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000 /* milliseconds */);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // Starts the query
        conn.connect();
        return conn.getInputStream();
    }

    private class ClosestGauge extends AsyncTask<List<Gauge>, Void, List<Gauge>>{


        @Override
        protected List<Gauge> doInBackground(List<Gauge>... gaugeList){




            return findClosestGauge(gaugeList[0]);
        }
        @Override
        protected void onPostExecute(List<Gauge> result){

            Log.d("locations", "locations that are within 5 miles: ");{
                for(int i = 0; i<result.size();i++){
                    Log.d("locations", result.get(i).getGaugeName() + " " + result.get(i).getGaugeID());
                }
            }
        }

        private List<Gauge> findClosestGauge(List<Gauge> gaugeList){

            Log.d("locations", "list size " + gaugeList.size());
            Location myLocation = new Location("");
            myLocation.setLatitude(38.796943);
            myLocation.setLongitude(-77.071622);
            GetLocations getLocations = new GetLocations(myLocation,gaugeList);

            return getLocations.getClosetGauges(5);
        }
    }

}


