package com.gregrussell.fenwickguageapp;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import com.gregrussell.fenwickguageapp.WeatherXmlParser.Gauge;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class HomeFragmentActivity extends FragmentActivity {

    public static final String WIFI = "Wi-fi";
    public static final String ANY = "Any";
    private static final String MY_URL = "https://raw.githubusercontent.com/gar529/GaugeProject/master/GaugeProject/xmlData.xml";
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 0;
    private static Location myLocation;
    public static String sPref = null;
    // Whether there is a Wi-Fi connection.
    private static boolean wifiConnected = true;
    // Whether there is a mobile connection.
    private static boolean mobileConnected = false;
    private static Context mContext;


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
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        mContext = this;
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        sPref = sharedPrefs.getString("listPref", "Wi-fi");
        Log.d("shared",sPref);
        updateConnectedFlags();

        setContentView(R.layout.home_fragment_layout);

        OpenDataBaseTask task = new OpenDataBaseTask();
        task.execute();

        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View searchFragLayout = inflater.inflate(R.layout.fragment_home_layout, null);


    }
    private class AppStart extends AsyncTask<Void,Void,Location>{
        @Override
        protected Location doInBackground(Void... params){
            return getMyLocation();

        }

        @Override
        protected void onPostExecute(Location result){

            Location myLocation = new Location("");
            Bundle bundle = new Bundle();
            if(result == null){
                myLocation.setLatitude(38.796395);
                myLocation.setLongitude(-77.072127);
            }else{
                myLocation = result;
            }
            bundle.putParcelable("MY_LOCATION",myLocation);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            FragmentHome fragmentHome = new FragmentHome();
            fragmentHome.setArguments(bundle);
            fragmentTransaction.add(R.id.home_fragment_container, fragmentHome, "home_fragment");
            fragmentTransaction.commit();
        }
    }

    private Location getMyLocation(){
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
        return myLocation;
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


    private class OpenDataBaseTask extends AsyncTask<Void,Void,Location>{

        @Override
        protected Location doInBackground(Void...params){

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
            return getMyLocation();
        }

        @Override
        protected void onPostExecute(Location result){


            Location myLocation = new Location("");
            Bundle bundle = new Bundle();
            if(result == null){
                myLocation.setLatitude(38.796395);
                myLocation.setLongitude(-77.072127);
            }else{
                myLocation = result;
            }
            bundle.putParcelable("MY_LOCATION",myLocation);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            FragmentHome fragmentHome = new FragmentHome();
            fragmentHome.setArguments(bundle);
            fragmentTransaction.add(R.id.home_fragment_container, fragmentHome, "home_fragment");
            fragmentTransaction.commit();

        }

        private int checkDataBaseVersion(){

            int dataBaseVersion = 0;
            DataBaseHelperGauges myDBHelper = new DataBaseHelperGauges(mContext);
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

            DataBaseHelperGauges myDBHelper = new DataBaseHelperGauges(mContext);
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
    }


}
