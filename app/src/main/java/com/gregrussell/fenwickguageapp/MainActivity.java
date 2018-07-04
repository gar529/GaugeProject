package com.gregrussell.fenwickguageapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.gregrussell.fenwickguageapp.WeatherXmlParser.Gauge;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import android.database.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends Activity {

    public static final String WIFI = "Wi-fi";
    public static final String ANY = "Any";
    //private static final String MY_URL = "https://water.weather.gov/ahps2/hydrograph_to_xml.php?gage=hpkv2&output=xml";
    private static Context myContext;
    private static final String MY_URL = "https://raw.githubusercontent.com/gar529/GaugeProject/master/GaugeProject/xmlData.xml";
    private int distanceAway = 5;
    // Whether there is a Wi-Fi connection.
    private static boolean wifiConnected = true;
    // Whether there is a mobile connection.
    private static boolean mobileConnected = false;
    // Whether the display should be refreshed.
    public static boolean refreshDisplay = true;
    public static String sPref = null;
    private static Address myAddress;
    private static Location myLocation;
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 0;

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("Timer","Start");
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        sPref = sharedPrefs.getString("listPref", "Wi-fi");
        Log.d("shared",sPref);
        updateConnectedFlags();

        myContext = this;
        final EditText zipEditText = (EditText)findViewById(R.id.zip_code_edit_text);
        final TextView invalidZip = (TextView)findViewById(R.id.invalid_zip_code);
        invalidZip.setVisibility(View.GONE);
        Button searchByZip = (Button)findViewById(R.id.search_by_zip_button);
        searchByZip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                myAddress = verifyZipCode(zipEditText.getText().toString());
                if(myAddress != null){
                    if(invalidZip.getVisibility() != View.GONE){
                        invalidZip.setVisibility(View.GONE);
                    };
                    loadPage();
                    Log.d("address4",myAddress.getPostalCode());
                }else{
                    if(invalidZip.getVisibility() != View.VISIBLE){
                        invalidZip.setVisibility(View.VISIBLE);
                    }
                    zipEditText.setHint("Invalid Zip Code");
                    Log.d("address3","invalid zip");
                }
            }
        });
        //Location loc = new Location("");
        //loc.setLatitude(38.830833);
        //loc.setLongitude(-77.134722);

        final FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        //mFusedLocationClient.setMockLocation(loc);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("locations8", "permission not granted");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }else {
            Log.d("locations9", "permission granted");
            mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    Log.d("location5", "logging");
                    if (location != null) {
                        //do location stuff

                        Calendar calendar = Calendar.getInstance();
                        Date date = new Date();
                        date.setTime(location.getTime());


                        Log.d("location1", String.valueOf(date));
                        long myTime = calendar.getTimeInMillis();
                        long currentTime = 0;

                        Log.d("location10", String.valueOf(date));
                        myLocation = location;

                        //loadPage();
                        //runGeo(location);


                    } else {
                        Log.d("location2", String.valueOf(location));
                    }
                }
            });
        }

        OpenDataBaseTask task = new OpenDataBaseTask();
        task.execute();


        //Log.d("location6","last location lat: " +mFusedLocationClient.getLastLocation().getResult().getLatitude());
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        }
        Log.d("location4","logging");
    }


    public void updateConnectedFlags() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo != null && activeInfo.isConnected()) {
            wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
            mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
        } else {
            wifiConnected = false;
            mobileConnected = false;
        }
    }


    private void runGeo(Location location){

        Geocoder geocoder = new Geocoder(myContext, Locale.getDefault());

        try{


            Address address;
            List<Address> addressList = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);
            Log.d("address2",addressList.size() + " " + String.valueOf(addressList));
            if(addressList.size() > 0) {
                address = addressList.get(0);
                Log.d("location12",address.getFeatureName() + " " + address.getThoroughfare() + " " + address.getLocality() + " " + address.getAdminArea() + " " +address.getPostalCode());

            }
        }catch (IOException e){
            e.printStackTrace();
        }

    }



    public void loadPage() {

        Log.d("xmlData", "any is: " + ANY);
        if((ANY.equals(sPref)) && (wifiConnected || mobileConnected)) {
            Log.d("location23", "executing");
            new DownloadXmlTask().execute(MY_URL);

        }
        else if ((WIFI.equals(sPref)) && (wifiConnected)) {
            Log.d("location24", "executing");
            new DownloadXmlTask().execute(MY_URL);

        } else {
            Log.d("location22", "error, no data available");


        }
    }


    // Implementation of AsyncTask used to download XML feed from stackoverflow.com.
    private class DownloadXmlTask extends AsyncTask<String, Void, List<Gauge>> {
        @Override
        protected List<Gauge> doInBackground(String... urls) {
            Log.d("xmlData", "doinbackground");


            try {
               // Log.d("location14",String.valueOf(checkVersion(urls[0])));
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

            EditText zipCodeEditText = (EditText)findViewById(R.id.zip_code_edit_text);
            String zipCode = zipCodeEditText.getText().toString();

            ClosestGaugeParams params = new ClosestGaugeParams(result,myAddress);
            ClosestGauge closestGauge = new ClosestGauge();
            closestGauge.execute();



            }

        }


    // Implementation of AsyncTask used to download XML feed from stackoverflow.com.
    private class CheckVersionTask extends AsyncTask<String, Void, Integer> {
        @Override
        protected Integer doInBackground(String... urls) {
            Log.d("xmlData", "doinbackground");


            try {
                // Log.d("location14",String.valueOf(checkVersion(urls[0])));
                return checkVersion(urls[0]);
            } catch (IOException e) {
                //return getResources().getString(R.string.connection_error);
                return null;
            } catch (XmlPullParserException e) {
                //return getResources().getString(R.string.xml_error);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            setContentView(R.layout.activity_main);

            Log.d("location16",String.valueOf(result));



        }

    }




    private Address verifyZipCode(String zipCode){


        Address address = null;
        Geocoder geocoder = new Geocoder(myContext, Locale.getDefault());

        try{

            List<Address> addressList = geocoder.getFromLocationName(zipCode,1);
            Log.d("address2",addressList.size() + " " + String.valueOf(addressList));
            if(addressList.size() > 0) {
                address = addressList.get(0);

            }
        }catch (IOException e){
            e.printStackTrace();
        }

        return address;
    }



    private int checkVersion(String urlString)throws XmlPullParserException, IOException{

        Log.d("location15","checking version");
        InputStream stream = null;
        CheckXMLVersion checkVersion = new CheckXMLVersion();
        int version = 0;
        try {
            stream = downloadUrl(urlString);
            version = checkVersion.parse(stream);
            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
        Log.d("location17",String.valueOf(version));
        return version;

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

    private class ClosestGauge extends AsyncTask<Void, Void, ClosestGaugeParamsGPS>{


        @Override
        protected ClosestGaugeParamsGPS doInBackground(Void... empty){


            //Log.d("address5",myAddress.getPostalCode());
            Location location = new Location("");
            if(myLocation == null) {

                location.setLatitude(myAddress.getLatitude());
                location.setLongitude(myAddress.getLongitude());
            }else{
                location = myLocation;
            }

            ClosestGaugeParamsGPS params = new ClosestGaugeParamsGPS(getAllGauges(),location);

            return findClosestGaugeByGPS(params);
        }
        @Override
        protected void onPostExecute(ClosestGaugeParamsGPS result){

            Log.d("locations", "number of gauges within 5 miles: " + result.gaugeList.size());
            Log.d("locations", "locations that are within 5 miles: ");{
                for(int i = 0; i<result.gaugeList.size();i++){
                    Log.d("locations", result.gaugeList.get(i).getGaugeName() + " " + result.gaugeList.get(i).getGaugeID());
                }

                ArrayList<Gauge> myList = new ArrayList<Gauge>();
                myList.addAll(result.gaugeList);
                Intent intent = new Intent(MainActivity.this,MainFragActivity.class);
                intent.putExtra("LIST_OF_RESULTS", myList);
                intent.putExtra("MY_LOCATION", result.location);
                intent.putExtra("DISTANCE", distanceAway);
                startActivity(intent);

            }
        }

        private List<Gauge> findClosestGauge(ClosestGaugeParams params){

            Log.d("locations", "list size " + params.gaugeList.size());
            Location myLocation = new Location("");
            myLocation.setLatitude(params.address.getLatitude());
            myLocation.setLongitude(params.address.getLongitude());
            GetLocations getLocations = new GetLocations(myLocation,params.gaugeList);

            return getLocations.getClosestGauges(distanceAway);
        }
        private ClosestGaugeParamsGPS findClosestGaugeByGPS(ClosestGaugeParamsGPS params){

            Log.d("locations", "list size " + params.gaugeList.size());
            GetLocations getLocations = new GetLocations(params.location,params.gaugeList);
            List<Gauge> locationList = getLocations.getClosestGauges(distanceAway);
            ClosestGaugeParamsGPS newParams = new ClosestGaugeParamsGPS(locationList,params.location);
            return newParams;
        }

        private List<Gauge> getAllGauges(){
            DataBaseHelperGauges myDBHelper = new DataBaseHelperGauges(MainActivity.this);
            try{
                myDBHelper.createDataBase();

            }catch (IOException e){
                throw new Error("unable to create db");
            }
            try{
                myDBHelper.openDataBase();
            }catch (SQLException sqle){
                throw sqle;
            }
            return myDBHelper.getAllGauges();
        }

    }

    private static class ClosestGaugeParams{
        List<Gauge> gaugeList;
        Address address;

        private ClosestGaugeParams(List<Gauge> gaugeList, Address address){
            this.gaugeList = gaugeList;
            this.address = address;
        }
    }

    private static class ClosestGaugeParamsGPS{
        List<Gauge> gaugeList;
        Location location;

        private ClosestGaugeParamsGPS(List<Gauge> gaugeList, Location location){
            this.gaugeList = gaugeList;
            this.location = location;
        }
    }

    private class OpenDataBaseTask extends AsyncTask<Void,Void,Boolean>{

        @Override
        protected Boolean doInBackground(Void...params){

            List<Gauge> gaugeList = new ArrayList<Gauge>();
            int xmlVersion = checkXMLVersion();
            int dbVersion = checkDataBaseVersion();
            if(xmlVersion == -1){
                //could not obtain xml Version
            }
            if(dbVersion != xmlVersion){
                //get xml data and add to database
                Log.d("versionCheck1","versions don't match");
                Log.d("versionCheck2",String.valueOf(xmlVersion) + " " + String.valueOf(dbVersion));
                try {
                    gaugeList = downloadXML();
                    addGaugesToDB(gaugeList,xmlVersion);

                    //add gauges to database
                }catch (IOException e){

                }catch (XmlPullParserException e){

                }
            }else{
                //everything is good
                Log.d("versionCheck3","versions match");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Boolean result){
            ClosestGauge task = new ClosestGauge();
            task.execute();
        }

        private int checkDataBaseVersion(){

            int dataBaseVersion = 0;
            DataBaseHelperGauges myDBHelper = new DataBaseHelperGauges(MainActivity.this);
            try{
                myDBHelper.createDataBase();

            }catch (IOException e){
                throw new Error("unable to create db");

            }
            try{
                myDBHelper.openDataBase();
            }catch (SQLException sqle){
                throw sqle;

            }
            return myDBHelper.getDataVersion();
        }

        private int checkXMLVersion(){

            int xmlVersion = -1;
            Log.d("Location20", "checking xml version");
            Log.d("location30",sPref + " " + ANY);
            Log.d("location31",String.valueOf(sPref.equals(ANY)));
            Log.d("location32",sPref + " " + WIFI);
            Log.d("location33",String.valueOf(sPref.equals(WIFI)));
            if((sPref.equals(ANY)) && (wifiConnected || mobileConnected)) {
                //new DownloadXmlTask().execute(MY_URL);
                try {
                    Log.d("location34","checking version");
                    xmlVersion = checkVersion(MY_URL);
                }catch (IOException e){

                }catch (XmlPullParserException e){

                }
            }
            else if ((sPref.equals(WIFI)) && (wifiConnected)) {
                //new DownloadXmlTask().execute(MY_URL);
                try {
                    Log.d("location35","checking version");
                    xmlVersion = checkVersion(MY_URL);
                }catch (IOException e){

                }catch (XmlPullParserException e){

                }
            } else {
                Log.d("location 21", "error, no data available");
                // new DownloadXmlTask().execute(MY_URL);
            }
            Log.d("location36", String.valueOf(xmlVersion));
            return xmlVersion;
        }


        private List<Gauge> downloadXML() throws XmlPullParserException, IOException {

            Log.d("downloadXML1","Start");
            InputStream stream = null;
            // Instantiate the parser
            WeatherXmlParser weatherXmlParser = new WeatherXmlParser();
            List<Gauge> gaugeList = null;

            try {
                stream = downloadUrl(MY_URL);
                gaugeList = weatherXmlParser.parse(stream);
                // Makes sure that the InputStream is closed after the app is
                // finished using it.
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
            Log.d("downloadXML2","Finish");
            return gaugeList;
        }

        private void addGaugesToDB(List<Gauge> gaugeList, int version){

            DataBaseHelperGauges myDBHelper = new DataBaseHelperGauges(MainActivity.this);
            try{
                myDBHelper.createDataBase();
            }catch (IOException e){
                throw new Error("unable to create db");
            }
            try{
                myDBHelper.openDataBase();
            }catch (SQLException sqle){
                throw sqle;
            }

            myDBHelper.addGauges(gaugeList,version);

        }
    }




}


