package com.gregrussell.fenwickguageapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;


import com.gregrussell.fenwickguageapp.WeatherXmlParser.Gauge;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

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
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MainFragActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    List<Gauge> myList;
    List<Gauge> allGauges;
    LinearLayout gaugeDataLayout;
    private int distanceAway;
    private Location location;
    private Context mContext;
    private List<Marker> markerList;
    private float zoomLevel[] = {11,10,8,0};
    private float currentZoomLevel = 0;
    private Location previousLocation;
    final double MILE_CONVERTER = .000621371;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_fragment);
        Log.d("Timer","Finish");

        mContext = this;
         //myList = (ArrayList<Gauge>)getIntent().getSerializableExtra("LIST_OF_RESULTS");
         distanceAway = getIntent().getIntExtra("DISTANCE",5);
         location = getIntent().getParcelableExtra("MY_LOCATION");
         previousLocation = location;



        gaugeDataLayout = (LinearLayout) findViewById(R.id.gauge_data_layout);
        RelativeLayout mapContainer = (RelativeLayout)findViewById(R.id.map_container);

        GetGauges task = new GetGauges();
        task.execute();

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;



        // Add a marker in Sydney and move the camera
        LatLng myLocation = new LatLng(location.getLatitude(),location.getLongitude());
        //LatLng farthestLocation = new LatLng(38.830833,-77.134722);

        LatLng sydney = new LatLng(-34, 151);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                Log.d("mapClick","click");
                gaugeDataLayout.setVisibility(View.GONE);
            }
        });
        Marker myLocationMarker = mMap.addMarker(new MarkerOptions().
                position(myLocation).icon(BitmapDescriptorFactory.
                defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).title("My Location"));
        myLocationMarker.setTag(null);
        markerList = new ArrayList<Marker>();
        for(int i=0;i<myList.size();i++){
            LatLng gauge = new LatLng(myList.get(i).getGaugeLatitude(), myList.get(i).getGaugeLongitude());
            //Marker marker = mMap.addMarker(new MarkerOptions().position(gauge).title(myList.get(i).getGaugeName()));
            Marker marker = mMap.addMarker(iconChangedOptions(new MarkerOptions().position(gauge).title(myList.get(i).getGaugeName()),mMap.getCameraPosition().zoom));
            marker.setTag(myList.get(i));
            markerList.add(marker);


        }
        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int i) {
               Log.d("mapPosition1", "camera position: " + String.valueOf(mMap.getCameraPosition()));
            }
        });

        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                Log.d("mapPosition2", "camera position: " + String.valueOf(mMap.getCameraPosition()));
                Log.d("mapPostion8","myList size: " + myList.size());

                Float zoom = mMap.getCameraPosition().zoom;
                Location myLocation = new Location("");
                myLocation.setLatitude(mMap.getCameraPosition().target.latitude);
                myLocation.setLongitude(mMap.getCameraPosition().target.longitude);

                if(previousLocation.distanceTo(myLocation) > 50 * MILE_CONVERTER){
                    GetLocations gl = new GetLocations(myLocation, allGauges);
                    List<Gauge> gaugeList = gl.getClosestGauges(250);

                    Float mZoom;
                    if(zoom > zoomLevel[0]){
                        mZoom = zoomLevel[0];
                    }else if(zoom > zoomLevel[1] && zoom <= zoomLevel[0]){
                        mZoom = zoomLevel[1];
                    }else if(zoom > zoomLevel[2] && zoom <= zoomLevel[1]){
                        mZoom = zoomLevel[2];
                    }else{
                        mZoom = zoomLevel[3];
                    }
                    if(mZoom == currentZoomLevel){
                        for(int i=0;i< gaugeList.size();i++){
                            LatLng gauge = new LatLng(gaugeList.get(i).getGaugeLatitude(), gaugeList.get(i).getGaugeLongitude());
                            //Marker marker = mMap.addMarker(new MarkerOptions().position(gauge).title(gaugeList.get(i).getGaugeName()));


                            Marker marker = mMap.addMarker(iconChangedOptions(new MarkerOptions().position(gauge).title(gaugeList.get(i).getGaugeName()),zoom));
                            marker.setTag(gaugeList.get(i));
                            markerList.add(marker);
                        }

                    }
                    mMap.clear();
                    markerList.clear();
                    for(int i=0;i< gaugeList.size();i++){
                        LatLng gauge = new LatLng(gaugeList.get(i).getGaugeLatitude(), gaugeList.get(i).getGaugeLongitude());
                        //Marker marker = mMap.addMarker(new MarkerOptions().position(gauge).title(gaugeList.get(i).getGaugeName()));
                        Marker marker = mMap.addMarker(iconChangedOptions(new MarkerOptions().position(gauge).title(gaugeList.get(i).getGaugeName()),zoom));
                        marker.setTag(gaugeList.get(i));
                        markerList.add(marker);




                    }
                }

                if(zoom > zoomLevel[0]){

                    if(zoomLevel[0]!= currentZoomLevel) {
                        currentZoomLevel = zoomLevel[0];

                        for (int i = 0; i < markerList.size(); i++) {
                            markerList.get(i).setIcon(BitmapDescriptorFactory.defaultMarker());
                        }

                    }

                }else if(zoom > zoomLevel [1] && zoom <= zoomLevel[0]){
                    if(zoomLevel[1]!= currentZoomLevel) {
                        currentZoomLevel = zoomLevel[1];
                        for (int i = 0; i < markerList.size(); i++) {
                            markerList.get(i).setIcon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(60, 60)));
                        }
                    }
                }else if(zoom > zoomLevel[2] && zoom <= zoomLevel[1]){
                    if(zoomLevel[2]!= currentZoomLevel) {
                        currentZoomLevel = zoomLevel[2];
                        for (int i = 0; i < markerList.size(); i++) {
                            markerList.get(i).setIcon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(40, 40)));
                        }
                    }
                }else{
                    if(zoomLevel[3]!= currentZoomLevel) {
                        currentZoomLevel = zoomLevel[3];
                        for (int i = 0; i < markerList.size(); i++) {
                            markerList.get(i).setIcon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(20, 20)));
                        }
                    }
                }



            }
        });










        //mMap.addMarker(new MarkerOptions().position(farthestLocation));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
        //zoom levels 5 miles - 12, 10miles - 11, 20miles - 10, 50miles - 8, 100miles - 7, 250miles - 6, 500miles < - 5

        int zoomLevel = 0;
        switch (distanceAway){
            case 5:
                zoomLevel = 12;
                break;
            case 10:
                zoomLevel = 11;
                break;
            case 20:
                zoomLevel = 10;
                break;
            case 50:
                zoomLevel = 8;
                break;
            default:
                zoomLevel = 5;
                break;



        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation,zoomLevel));
    }

    private boolean containsMarker(List<Marker> list, String name){

        for(int i = 0; i< list.size();i++){
            Gauge g = (Gauge)list.get(i).getTag();
            if(g.getGaugeName().equals(name)){
                return true;
            }
        }
        return false;
    }

    private MarkerOptions iconChangedOptions (MarkerOptions markerOptions, Float zoom){

        if(zoom > zoomLevel[0]){
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker());
        }else if(zoom > zoomLevel [1] && zoom <= zoomLevel[0]){
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(60, 60)));
        }else if(zoom > zoomLevel[2] && zoom <= zoomLevel[1]){
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(40, 40)));
        }else{
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(20, 20)));
        }
        return markerOptions;
    }



    public Bitmap resizeMapIcons(int width, int height){
        //Log.d("mapPosition6", "drawable value: " + String.valueOf(getResources().getDrawable(R.drawable.marker_circle)));
        Drawable drawable = getResources().getDrawable(R.drawable.marker_circle);
        if(drawable instanceof  BitmapDrawable){
          //  Log.d("mapPosition7", "bitmap drawable");
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0,0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        //Log.d("mapPosition5", "bitmap value: " + String.valueOf(bitmap));
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
        return resizedBitmap;
    }

    @Override
    public boolean onMarkerClick(final Marker marker){

        Gauge gauge = (Gauge)marker.getTag();

        if(gauge != null){
        Log.d("markerClick","gauge clicked on: " + gauge.getGaugeName() + " " + gauge.getGaugeID() + " " + gauge.getGaugeURL());
        LoadGauge task = new LoadGauge();
        task.execute(gauge);
        return false;
        }
        else{
            Log.d("markerClick","gauge clicked on home");
            return true;
        }

    }

    private class GetGauges extends AsyncTask<Void, Void, List<Gauge>>{
        @Override
        protected List<Gauge> doInBackground(Void... params){

            allGauges = getAllGauges();
            GetLocations gl = new GetLocations(location,allGauges);
            return gl.getClosestGauges(100);
        }
        @Override
        protected void onPostExecute(List<Gauge> result){

            myList = result;
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(MainFragActivity.this);


        }

        private List<Gauge> getAllGauges(){
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
            return myDBHelper.getAllGauges();
        }
    }

    private class LoadGauge extends AsyncTask<Gauge, Void, List<Datum>>{


        View loadingPanel = (View)findViewById(R.id.listLoadingPanel);
        View gaugeText = (View)findViewById(R.id.gauge_data_text);
        @Override protected void onPreExecute(){

            gaugeDataLayout.setVisibility(View.VISIBLE);
            gaugeText.setVisibility(View.GONE);
            loadingPanel.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<Datum> doInBackground(Gauge... gauge){

            List<Datum> gaugeData = new ArrayList<Datum>();
            try {
                 gaugeData = readGauge(gauge[0].getGaugeID());
            }catch (IOException e){
                e.printStackTrace();
            }catch (XmlPullParserException e){
                e.printStackTrace();
            }
            Log.d("gaugedata", "size is " + gaugeData.size() + " for " + gauge[0].getGaugeName());
            for(int i = 0; i <gaugeData.size(); i++){
                Log.d("gaugedataBackground", gaugeData.get(i).getPrimary() + " " + gaugeData.get(i).getValid() + gauge[0].getGaugeName() );
            }

            return gaugeData;
        }

        @Override protected void onPostExecute(List<Datum> result){


            if(result.size() > 0) {


                TextView waterHeight = (TextView) findViewById(R.id.water_height);
                TextView time = (TextView) findViewById(R.id.reading_time);
                Log.d("onPostExecute result", "valid is: " + result.get(0).getValid() + " primary is: " + result.get(0).getPrimary());
                waterHeight.setText((result.get(0).getPrimary()) + "ft");
                String dateString = result.get(0).getValid();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm");
                Date convertedDate = new Date();
                try {
                    convertedDate = dateFormat.parse(dateString);


                } catch (ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (NullPointerException e){
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


                Log.d("xmlData", "correctTZDate is: " + correctTZDate.getTime());
                time.setText(formatter.format(correctTZDate));
                loadingPanel.setVisibility(View.GONE);
                gaugeText.setVisibility(View.VISIBLE);
            }
            else{
                TextView waterHeight = (TextView) findViewById(R.id.water_height);
                TextView time = (TextView) findViewById(R.id.reading_time);
                waterHeight.setText("No Data Available. Gauge May Be Offline");
                time.setText("");
                loadingPanel.setVisibility(View.GONE);
                gaugeText.setVisibility(View.VISIBLE);
            }
        }
    }


    private List<Datum> readGauge(String gaugeID) throws IOException, XmlPullParserException{
        String urlString = "https://water.weather.gov/ahps2/hydrograph_to_xml.php?gage=" + gaugeID;
        InputStream stream = null;
        // Instantiate the parser
        GaugeReadingXMLParser gaugeXmlParser = new GaugeReadingXMLParser();
        List<Datum> datums = null;
        String valid = null;
        String primary = null;
        String summary = null;
        Calendar rightNow = Calendar.getInstance();
        DateFormat formatter = new SimpleDateFormat("MMM dd h:mmaa");


        try {
            stream = downloadUrl(urlString);
            datums = gaugeXmlParser.parse(stream);
            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        }
        finally {
            if (stream != null) {
                stream.close();
            }
        }
        //Log.d("readGauge","first datum is " + datums.get(0).getPrimary());
        return datums;
    }



         // Given a string representation of a URL, sets up a connection and gets
             // an input stream.
         private InputStream downloadUrl(String urlString) throws IOException {
            Log.d("urlString",urlString);
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

    private Address getAddress(String zipCode){

        Address address = null;
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try{
            List<Address> addressList = geocoder.getFromLocationName(zipCode,1);
            Log.d("address6",addressList.size() + " " + String.valueOf(addressList));
            if(addressList.size() > 0) {
                address = addressList.get(0);

            }
        }catch (IOException e){
            e.printStackTrace();
        }

        return address;
    }
}
