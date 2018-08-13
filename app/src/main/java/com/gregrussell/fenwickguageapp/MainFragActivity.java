package com.gregrussell.fenwickguageapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

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
import java.util.Random;
import java.util.TimeZone;

public class MainFragActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
        SearchView.OnQueryTextListener, LoaderManager.LoaderCallbacks<Cursor>{

    private static GoogleMap mMap;
    private static List<Gauge> myGaugeList;
    private static List<Gauge> allGauges;
    private LinearLayout gaugeDataLayout;
    private static float mapZoomLevel;

    private Context mContext;
    private static List<Marker> markerList;
    private static float zoomLevel[] = {11,10,8,7,0};
    private static float previousZoom = 0;
    private static Location previousLocation; //used as point to measure distance away of gauges
    final double MILE_CONVERTER = .000621371;
    private static LatLng myLatLng;
    private RelativeLayout invisibleLayout;
    private SimpleCursorAdapter mAdapter;
    private SearchView searchView;
    private static Marker selectedMarker;
    private ListView searchSuggestions;
    private String mCurFilter;
    private FusedLocationProviderClient mFusedLocationClient;
    private static LocationRequest mLocationRequest;
    private static LocationCallback mLocationCallback;
    private static Location updatedLocation;

    public static final String WIFI = "Wi-fi";
    public static final String ANY = "Any";
    private static final String MY_URL = "https://raw.githubusercontent.com/gar529/GaugeProject/master/GaugeProject/xmlData.xml";
    private static Location homeLocation; //User's location
    public static String sPref = null;
    private static boolean wifiConnected = true; // Whether there is a Wi-Fi connection.
    private static boolean mobileConnected = false; // Whether there is a mobile connection.


    private static final int CLOSEST_ZOOM = 12;
    private static final int CLOSE_ZOOM = 11;
    private static final int MIDDLE_ZOOM = 10;
    private static final int FAR_ZOOM = 8;
    private static final int FARTHEST_ZOOM = 5;



    @Override
    public void onBackPressed(){

        Log.d("backpressed",String.valueOf(selectedMarker));

        if(getSupportFragmentManager().findFragmentByTag("favorite_fragment") == null && getSupportFragmentManager().findFragmentByTag("gauge_fragment") == null) {
            if (gaugeDataLayout.getVisibility() == View.VISIBLE) {
                Log.d("backpressed7,",String.valueOf(selectedMarker));
                gaugeDataLayout.setVisibility(View.GONE);
                if (selectedMarker != null) {
                    resetSelectedMarker(mContext);
                    selectedMarker = null;
                }
            } else if (searchSuggestions.getCount() > 0) {
                Log.d("backpressed6,",String.valueOf(selectedMarker));
                searchView.setQuery("", false);
                searchView.clearFocus();
                if (selectedMarker != null) {
                    resetSelectedMarker(mContext);
                    selectedMarker = null;
                }
            }else if(selectedMarker !=null) {
                Log.d("backpressed1,",String.valueOf(selectedMarker));
                if (getSupportFragmentManager().findFragmentByTag("load_gauge_layout") != null) {
                    Log.d("backpressed2",String.valueOf(selectedMarker));
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("gauge", (Gauge)selectedMarker.getTag());
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    LoadGaugeFragment loadGaugeFragment = new LoadGaugeFragment();
                    loadGaugeFragment.setArguments(bundle);
                    fragmentTransaction.replace(R.id.gauge_data_layout, loadGaugeFragment, "load_gauge_fragment");
                    fragmentTransaction.commit();

                }else{
                    Log.d("backpressed3",String.valueOf(selectedMarker));
                    resetSelectedMarker(mContext);
                    selectedMarker = null;
                    super.onBackPressed();


                }
            } else {
                Log.d("backpressed5,",String.valueOf(selectedMarker));
                super.onBackPressed();
            }
        }else{
            if(selectedMarker !=null) {

                Log.d("backpressed8,",String.valueOf(selectedMarker));
                Bundle bundle = new Bundle();
                bundle.putSerializable("gauge", (Gauge)selectedMarker.getTag());
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                LoadGaugeFragment loadGaugeFragment = new LoadGaugeFragment();
                loadGaugeFragment.setArguments(bundle);
                fragmentTransaction.replace(R.id.gauge_data_layout, loadGaugeFragment, "load_gauge_fragment");
                fragmentTransaction.commit();
                super.onBackPressed();

            }else {
                super.onBackPressed();
            }
        }
    }



    @Override
    public void onResume(){
        super.onResume();

        Log.d("onResumeMain","onResume");
        if(selectedMarker != null){
            Gauge gauge = (Gauge)selectedMarker.getTag();

            if(gaugeDataLayout.getVisibility() != View.VISIBLE){
                gaugeDataLayout.setVisibility(View.VISIBLE);
            }
            Bundle bundle = new Bundle();
            bundle.putSerializable("gauge",gauge);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            LoadGaugeFragment loadGaugeFragment = new LoadGaugeFragment();
            loadGaugeFragment.setArguments(bundle);
            if(fragmentManager.findFragmentByTag("load_gauge_fragment") != null) {
                fragmentTransaction.replace(R.id.gauge_data_layout, loadGaugeFragment, "load_gauge_fragment");
            }else{
                fragmentTransaction.replace(R.id.gauge_data_layout, loadGaugeFragment, "load_gauge_fragment");
            }
            fragmentTransaction.commit();
        }else{
            if(gaugeDataLayout.getVisibility() != View.GONE)
            gaugeDataLayout.setVisibility(View.GONE);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_fragment);
        Log.d("Timer","Finish");

        mContext = this;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(MainFragActivity.this);
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5000);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.d("getLocationUpdate5","in onLocationResult");
                if (locationResult == null) {
                    Log.d("getLocationUpdate3","location result is null");
                    return;
                }else{
                    Log.d("getLocationUpdate4","location result is not null");
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    // ...
                    Log.d("getLocationUpdate",location.getLatitude() + ", " + location.getLongitude());
                    if(location!=null) {
                        Log.d("getLocationUpdate",location.getLatitude() + ", " + location.getLongitude());
                        homeLocation = location;
                        stopLocationUpdates(mFusedLocationClient);
                    }
                }
            };
        };
        startLocationUpdates(mContext,mFusedLocationClient);
        LinearLayout mainLayout = (LinearLayout)findViewById(R.id.main_layout);
        mainLayout.requestFocus();
        String s = getIntent().getStringExtra("notification");
        Log.d("intent41",String.valueOf(s));
        if(getIntent().getStringExtra("notification") !=null){

            Log.d("intent40","from notification");

        }
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        sPref = sharedPrefs.getString("listPref", ANY);
        Log.d("shared",sPref);
        updateConnectedFlags();

        invisibleLayout = (RelativeLayout) findViewById(R.id.invisible_layout);
        invisibleLayout.setVisibility(View.GONE);
        gaugeDataLayout = (LinearLayout) findViewById(R.id.gauge_data_layout);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView)findViewById(R.id.map_search_bar);

        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        searchView.setOnQueryTextListener(this);
        View searchField = findViewById(getResources().getIdentifier("android:id/search_src_text", null, null));
        searchField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("searchField","clicked");
                if(gaugeDataLayout.getVisibility() != View.GONE) {
                    gaugeDataLayout.setVisibility(View.GONE);
                    if(selectedMarker != null){
                        resetSelectedMarker(mContext);
                        selectedMarker = null;
                    }
                }
            }
        });
        searchField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                Log.d("searchField","focus changed");
                if(b){
                    if(gaugeDataLayout.getVisibility() != View.GONE) {
                        gaugeDataLayout.setVisibility(View.GONE);
                        if(selectedMarker != null){
                            resetSelectedMarker(mContext);
                            selectedMarker = null;
                        }
                    }
                }
            }
        });
        FloatingActionButton floatingActionButtonFavorites = (FloatingActionButton)findViewById(R.id.floating_button_map_favorites);
        floatingActionButtonFavorites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle bundle = new Bundle();
                if(selectedMarker !=null){

                    bundle.putSerializable("selected_gauge", ((Gauge)selectedMarker.getTag()));
                }
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                FragmentFavorites fragmentFavorites = new FragmentFavorites();
                fragmentFavorites.setArguments(bundle);
                fragmentTransaction.add(R.id.main_layout, fragmentFavorites, "favorite_fragment").addToBackStack("Tag");
                fragmentTransaction.commit();
            }
        });
        FloatingActionButton floatingActionButtonLocation = (FloatingActionButton)findViewById(R.id.floating_button_map_location);
        floatingActionButtonLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                updateLocation(mContext,mFusedLocationClient);
                UpdateLocationParams params = new UpdateLocationParams(mContext,mFusedLocationClient);
                AsyncUpdateLocation asyncUpdateLocation = new AsyncUpdateLocation();
                asyncUpdateLocation.execute(params);
                clearViews(mContext,searchView,gaugeDataLayout);
            }
        });
        gaugeDataLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(selectedMarker != null){
                    Gauge gauge = (Gauge)selectedMarker.getTag();
                    LoadFragmentGaugeParams params = new LoadFragmentGaugeParams(gauge,new GaugeFragParams(null,false,false),getSupportFragmentManager());
                    LoadFragmentGaugeAsync loadFragmentGaugeAsync = new LoadFragmentGaugeAsync();
                    loadFragmentGaugeAsync.execute(params);
                }
            }
        });
        View closeButton = findViewById(getResources().getIdentifier("android:id/search_close_btn", null, null));
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("searchClose","clicked");
                clearViews(mContext,searchView,gaugeDataLayout);

            }
        });
        searchSuggestions = (ListView)findViewById(R.id.search_suggestions);
        searchSuggestions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Log.d("intent2","on click, clicked " + i);
                //Log.d("intent3", getIntent().getAction());
                Cursor c = (Cursor)mAdapter.getItem(i);
                String str = c.getString(4);
                Log.d("intent12",str);
                searchView.setQuery(c.getString(1),false);
                searchView.clearFocus();
                searchSuggestions.setAdapter(null);
                Intent intent = new Intent(mContext,MainFragActivity.class);
                intent.putExtra("DATA",str);
                startActivity(intent);
            }
        });
        mAdapter = new SimpleCursorAdapter(this,android.R.layout.simple_list_item_2, null,
                new String[] {SearchManager.SUGGEST_COLUMN_TEXT_1,SearchManager.SUGGEST_COLUMN_TEXT_2}, new int[] {android.R.id.text1, android.R.id.text2},0);
        searchSuggestions.setAdapter(mAdapter);
        getLoaderManager().initLoader(0,null,this);
        Log.d("searchView3", "adapter empty? " + mAdapter.isEmpty());
        GaugeApplication.myDBHelper.clearMarkers();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        getMyLocation(mContext,mFusedLocationClient,mapFragment);


    }


    @Override
    protected void onNewIntent(Intent intent){
        setIntent(intent);
        Log.d("intent5","on click");
        Log.d("intent6", String.valueOf(getIntent().getAction()));

        handleIntent(intent);
    }



    private static void getMyLocation(Context context,FusedLocationProviderClient providerClient, SupportMapFragment mapFragment){

        boolean b = false;
        UpdateDBParams updateDBParams = new UpdateDBParams(context, b, mapFragment);
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            UpdateDataBaseTask task = new UpdateDataBaseTask();
            task.execute(updateDBParams);
        }else {
            Log.d("locations9", "permission granted");

            getLastKnownLocation(context,providerClient);
            UpdateDataBaseTask task = new UpdateDataBaseTask();
            task.execute(updateDBParams);
        }
    }

    /**
     * Checks if location updates are permitted, then requests location updates in the interval
     * defined by mLocationRequest
     * @param context Activity context
     * @param mFusedLocationClient FusedLocationProviderClient used to initiate location requests
     */
    private static void startLocationUpdates(Context context, FusedLocationProviderClient mFusedLocationClient) {

        Log.d("getLocationUpdate","startLocation");
        if(ContextCompat.checkSelfPermission((Activity)context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback,
                    null /* Looper */);
            Log.d("getLocationUpdate2","getting location");
        }
    }

    /**
     * Stops the receiving of location updates
     * @param mFusedLocationClient FusedLocationProviderClient initiates stoppage of location updates
     */
    private static void stopLocationUpdates(FusedLocationProviderClient mFusedLocationClient) {
        Log.d("getLocationUpdate6","location updates stopped");
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    /**
     * Method used to check status of the internet connection
     * Sets booleans for wifiConnected and mobileConnected
     * If set to false, connection not available
     */
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

    /**
     * Method for handling incoming Intents
     * Intent with a stringExtra "notification" comes from the user clicking the alert notification -
     * The notification intent is handled by displaying FragmentFavorites.
     * Intent with ACTION_SEARCH is an intent that comes from a search query -
     * They query is obtained from the stringExtra, searchView is cleared, and search() is called.
     * Finally, if any other intents are received, the handler checks if the stringExtra "DATA" is
     * attached. Intents with stringExtra "DATA" are sent from clicking a searchSuggestion -
     * The intent is handled by starting a MoveToLocation async Task
     * @param intent the intent that is received by the handler
     */
    private void handleIntent(Intent intent){

        if(intent.getStringExtra("notification") != null){
            Log.d("intent30","from notification");

            FragmentManager fragmentManager = getSupportFragmentManager();

            //if a FavoriteFragment is already in place, remove it and add a new one
            if(fragmentManager.findFragmentByTag("favorite_fragment") != null ){
                fragmentManager.beginTransaction().remove(fragmentManager.findFragmentByTag("favorite_fragment")).commit();
                fragmentManager.popBackStack();

                addFragmentFavorites();

            }else {
                addFragmentFavorites();
            }

        }else {
            if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
                String query = intent.getStringExtra(SearchManager.QUERY);
                Log.d("intent7", "search query: " + query);
                search(query);
                searchView.clearFocus();
                searchSuggestions.setAdapter(null);
            } else {
                String dataString = intent.getStringExtra("DATA");
                if (dataString != null) {
                    Log.d("intent20", dataString);

                    MoveLocationParams params = new MoveLocationParams(dataString,null,gaugeDataLayout,getSupportFragmentManager());
                    MoveToLocation task = new MoveToLocation();
                    task.execute(params);

                }
            }
        }
    }

    /**
     * Adds new FavoriteFragment to main_layout
     */
    private void addFragmentFavorites(){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        FragmentFavorites fragmentFavorites = new FragmentFavorites();
        fragmentTransaction.add(R.id.main_layout, fragmentFavorites, "favorite_fragment").addToBackStack("Tag");
        fragmentTransaction.commit();
    }


    /**
     * Receives a user inputted String, passes it through a Geocoder, and moves the camera to that
     * address if it is valid and located in the US
     * @param query user input string that will be searched
     */
    private void search(String query){

        Geocoder geo = new Geocoder(this, Locale.getDefault());
        List<Address> addressList = new ArrayList<Address>();
        Address address;
        try {
            addressList = geo.getFromLocationName(query, 10);
        }catch (IOException e){
            e.printStackTrace();
        }

        if(!addressList.isEmpty()){
            //the geocoder can return multiple addresses from a single Location Name
            //Uses the first address returned that is in the US
            int i = 0;
            address = addressList.get(i);
            while( i < addressList.size() && !address.getCountryCode().equals("US")){

                Log.d("searchAddress4",address.getLocality() + ", " + address.getAdminArea() + ", " + address.getCountryCode() + "   " + addressList.size());
                i++;
            }


            Log.d("searchAddress5",String.valueOf(address.getCountryCode().equals("US")));

            if(address.getCountryCode().equals("US")){
                LatLng latLng = new LatLng(address.getLatitude(),address.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,CLOSEST_ZOOM));
            }else{
                CharSequence text = "No results for " + query;
                Toast toast = Toast.makeText(mContext,text,Toast.LENGTH_SHORT);
                toast.show();
            }

            }
    }


    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader, so we don't care about the ID.
        // First, pick the base URI to use depending on whether we are
        // currently filtering.

        Log.d("searchview7","create loader");
        Uri baseUri;

        if(mCurFilter != null){
            baseUri = Uri.withAppendedPath(SearchContentProvider.BASE_URI,Uri.encode(mCurFilter));

        }else {

            baseUri = SearchContentProvider.BASE_URI;
        }
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        String select = SearchManager.SUGGEST_COLUMN_TEXT_1 + " LIKE ? OR " +
                SearchManager.SUGGEST_COLUMN_TEXT_2 + " LIKE ?";
        String projection[] = {
                DataBaseHelperGauges.Suggestions._ID,
                SearchManager.SUGGEST_COLUMN_TEXT_1,
                SearchManager.SUGGEST_COLUMN_TEXT_2,
                SearchManager.SUGGEST_COLUMN_INTENT_ACTION,
                SearchManager.SUGGEST_COLUMN_INTENT_DATA,
                SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
                SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA,
                SearchManager.SUGGEST_COLUMN_QUERY};

        Log.d("searchView12","calling provider");
        return new CursorLoader(mContext, baseUri, projection , select, null,
                null);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    public boolean onQueryTextChange(String newText) {
        // Called when the action bar search text has changed.  Update
        // the search filter, and restart the loader to do a new query
        // with this filter.
        searchSuggestions.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
        Log.d("searchView1","text change listener " + newText);
        mCurFilter = !TextUtils.isEmpty(newText) ? newText : null;
        getLoaderManager().restartLoader(0, null, this);
        Log.d("searchView2", "adapter empty? " + mAdapter.isEmpty());
        return true;
    }

    /**
     * Method called when keyboard searchButton is pressed. Calls search()
     * @param query String inputted by user
     * @return
     */
    @Override public boolean onQueryTextSubmit(String query) {

        search(query);
        return true;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app. - Google
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setMinZoomPreference(7.0f);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setCompassEnabled(true);
        Log.d("compass", String.valueOf(mMap.getUiSettings().isCompassEnabled()));
        setCompassPosition();
        myLatLng = new LatLng(homeLocation.getLatitude(),homeLocation.getLongitude());
        mMap.setOnMarkerClickListener(this);

        //when the map is clicked, clear the views
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                Log.d("mapClick","click");
               clearViews(mContext,searchView,gaugeDataLayout);
            }
        });

        Marker myLocationMarker = mMap.addMarker(new MarkerOptions().
                position(myLatLng).icon(BitmapDescriptorFactory.
                defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).title("My Location"));
        myLocationMarker.setTag(null);
        loadInitialMarkers();
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
                Log.d("mapPosition8", "myList size: " + myGaugeList.size());
                markerLoader();

            }
        });


        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng,mapZoomLevel));
        searchView.clearFocus();
    }

    private void loadInitialMarkers(){
        markerList = new ArrayList<Marker>();
        for(Gauge gauge : myGaugeList){
            LatLng latLng = new LatLng(gauge.getGaugeLatitude(), gauge.getGaugeLongitude());
            Marker marker = mMap.addMarker(iconChangedOptions(mContext,new MarkerOptions().position(latLng).title(gauge.getGaugeName()),mMap.getCameraPosition().zoom));
            marker.setTag(gauge);
            markerList.add(marker);
        }
    }

    /**
     * Loads markers based on camera position and zoom level
     */
    private void markerLoader(){
        Float zoom = mMap.getCameraPosition().zoom;

        //myLocation is where the camera has moved to
        Location myLocation = new Location("");
        myLocation.setLatitude(mMap.getCameraPosition().target.latitude);
        myLocation.setLongitude(mMap.getCameraPosition().target.longitude);

        Log.d("markerStuff9","what's the distance" + previousLocation.distanceTo(myLocation) * MILE_CONVERTER);

        //get the zoom of the camera and assign a zoom level based on it
        Float mZoom;
        if(zoom > zoomLevel[0]){
            mZoom = zoomLevel[0];
        }else if(zoom > zoomLevel[1] && zoom <= zoomLevel[0]){
            mZoom = zoomLevel[1];
        }else if(zoom > zoomLevel[2] && zoom <= zoomLevel[1]){
            mZoom = zoomLevel[2];
        }else if(zoom >= zoomLevel[3] && zoom <= zoomLevel[2]){
            mZoom = zoomLevel[3];
        }
        else{
            mZoom = zoomLevel[4];
        }

        //if the camera has moved 5 miles, display a group of markers based on the zoom level


        //Get the groupings of markers and put them in an array
        GetLocations gl = new GetLocations(myLocation, allGauges);
        List[] gaugeListArray = gl.getClosestGaugesArray();
        previousLocation = myLocation;



        //check if the zoom level has changed when the camera position moved
        //if the zoom level didn't change, just add more markers
        if(mZoom == zoomLevel[0] && previousZoom == mZoom){
            Log.d("addMapMarkers2","zoomLevel 0");
            List<Gauge> mList = gaugeListArray[0];
            previousZoom = mZoom;
            addMarkers(mList,mZoom);
        }
        else if(mZoom == zoomLevel[1] && previousZoom == mZoom){
            Log.d("addMapMarkers3","zoomLevel 1");
            List<Gauge> mList = gaugeListArray[1];
            previousZoom = mZoom;
            addMarkers(mList,mZoom);
        }
        else if(mZoom == zoomLevel[2] && previousZoom == mZoom){
            Log.d("addMapMarkers4","zoomLevel 2");
            List<Gauge> mList = gaugeListArray[2];
            previousZoom = mZoom;
            addMarkers(mList,mZoom);
        }
        else if(mZoom == zoomLevel[3] && previousZoom == mZoom){
            Log.d("addMapMarkers5","zoomLevel 3");
            List<Gauge> mList = gaugeListArray[3];
            previousZoom = mZoom;
            addMarkers(mList,mZoom);
        }
        else if(mZoom == zoomLevel[4] && previousZoom == mZoom){
            Log.d("addMapMarkers6","zoomLevel 4");
            List<Gauge> mList = gaugeListArray[4];
            previousZoom = mZoom;
            addMarkers(mList,mZoom);
        }else{
            //zoom level has changed
            //if zoom in clear map and markers
            //if zoom out, resize markers on screen then add new markers
            Log.d("addMapMarkers22","previousZoom: " + previousZoom + " mZoom: " + mZoom);
            if(previousZoom < mZoom){
                //zooming in

                Log.d("addMapMarkers20","zooming in");
                //selectedMarker's tag is cleared with map, set it to a gauge object then re-add
                if(selectedMarker !=null) {
                    Log.d("markerStuff5", ((Gauge) selectedMarker.getTag()).getGaugeID());
                    Gauge gauge = (Gauge) selectedMarker.getTag();
                    mMap.clear();
                    markerList.clear();
                    selectedMarker.setTag(gauge);
                    markerList.add(selectedMarker);
                }else {
                    mMap.clear();
                    markerList.clear();
                }
                Log.d("markersAdded45","clearMarkers");
                GaugeApplication.myDBHelper.clearMarkers();

                //add markers
                Marker myLocationMarker = mMap.addMarker(new MarkerOptions().
                        position(myLatLng).icon(BitmapDescriptorFactory.
                        defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).title("My Location"));
                myLocationMarker.setTag(null);
                if(mZoom == zoomLevel[0]){
                    List<Gauge> mList = gaugeListArray[0];
                    addMarkers(mList,mZoom);
                    previousZoom = mZoom;
                }
                else if(mZoom == zoomLevel[1]){
                    List<Gauge> mList = gaugeListArray[1];
                    addMarkers(mList,mZoom);
                    previousZoom = mZoom;
                }
                else if(mZoom == zoomLevel[2]){
                    List<Gauge> mList = gaugeListArray[2];
                    addMarkers(mList,mZoom);
                    previousZoom = mZoom;
                }
                else if(mZoom == zoomLevel[3]){
                    List<Gauge> mList = gaugeListArray[3];
                    addMarkers(mList,mZoom);
                    previousZoom = mZoom;
                }
                else if(mZoom == zoomLevel[4]){
                    List<Gauge> mList = gaugeListArray[4];
                    addMarkers(mList,mZoom);
                    previousZoom = mZoom;
                }

            }else{
                //zooming out
                Log.d("addMapMarkers21","zooming out");
                Log.d("addMapMarkers24","mZoom is: " + mZoom + " previousZoom: " + previousZoom);
                Log.d("addMapMarkers25", "markerList size: " + markerList.size());

                //resize old markers first, then add new ones
                if(mZoom == zoomLevel[0]){
                    if(zoomLevel[0]!= previousZoom) {
                        previousZoom = zoomLevel[0];
                        for (Marker marker : markerList) {
                            if (marker != selectedMarker) {
                                marker.setIcon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(mContext,60, 60)));
                            }
                        }
                        List<Gauge> mList = gaugeListArray[0];
                        addMarkers(mList,mZoom);
                    }
                }else if(mZoom == zoomLevel[1]){
                    if(zoomLevel[1]!= previousZoom) {
                        previousZoom = zoomLevel[1];
                        for (Marker marker : markerList) {
                            if (marker != selectedMarker) {
                                marker.setIcon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(mContext,60, 60)));
                            }
                        }
                        List<Gauge> mList = gaugeListArray[1];
                        addMarkers(mList,mZoom);
                    }
                }else if(mZoom == zoomLevel[2]){
                    if(zoomLevel[2]!= previousZoom) {
                        previousZoom = zoomLevel[2];
                        for (Marker marker : markerList) {
                            if ( marker != selectedMarker) {
                                marker.setIcon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(mContext,40, 40)));
                            }
                        }
                        List<Gauge> mList = gaugeListArray[2];
                        addMarkers(mList,mZoom);
                    }
                }else if(mZoom == zoomLevel[3]){
                    if(zoomLevel[3]!= previousZoom) {
                        previousZoom = zoomLevel[3];
                        for (Marker marker : markerList) {
                            if (marker != selectedMarker) {
                                marker.setIcon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(mContext,20, 20)));
                            }
                        }
                        List<Gauge> mList = gaugeListArray[3];
                        addMarkers(mList,mZoom);
                    }
                }else{
                    if(zoomLevel[4]!= previousZoom) {
                        previousZoom = zoomLevel[4];
                        for (Marker marker : markerList) {
                            if (marker != selectedMarker) {
                                marker.setIcon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(mContext,20, 20)));
                            }
                        }
                        List<Gauge> mList = gaugeListArray[4];
                        addMarkers(mList,mZoom);
                    }
                }
            }
        }
    }

    /**
     * This method sets the position of the map compass
     */
    private void setCompassPosition(){

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        int topPadding = height / 6;
        int leftPadding = width / 20;
        Log.d("compass","Screen height: " + height +"; top padding: " + topPadding);
        mMap.setPadding(leftPadding,topPadding,0,0);

    }

    
    private void addMarkers(List<Gauge> gaugeList, Float zoom){
        Log.d("markersAdded1", "STart");
        //invisibleLayout.setVisibility(View.VISIBLE);
        Log.d("markersAdded3", String.valueOf(invisibleLayout.getVisibility()));
        List<Marker> nMarkerList = new ArrayList<Marker>();
        Log.d("markersAdded5","loop started");
        int i = 0;
        for(Gauge gauge : gaugeList){
            LatLng latLng = new LatLng(gauge.getGaugeLatitude(), gauge.getGaugeLongitude());
            Log.d("markersAdded6","Gauge being checked: " + gauge.getGaugeName() + " " + gauge.getGaugeID());
            if(!checkMarkers(gauge.getGaugeID())){
                //start selectedMarker added code
                if(selectedMarker != null){
                    Marker marker;
                    if(((Gauge)selectedMarker.getTag()).getGaugeID().equals(gauge.getGaugeID())){
                        marker = mMap.addMarker(new MarkerOptions().position(latLng).title(gauge.getGaugeName()));
                        i++;
                    }else {
                        marker = mMap.addMarker(iconChangedOptions(mContext,new MarkerOptions().position(latLng).title(gauge.getGaugeName()), zoom));
                        i++;
                    }
                    marker.setTag(gauge);
                    markerList.add(marker);
                    nMarkerList.add(marker);
                    Log.d("markerStuff",String.valueOf(selectedMarker) +"   " + ((Gauge)selectedMarker.getTag()).getGaugeID());
                    if(((Gauge)selectedMarker.getTag()).getGaugeID().equals(gauge.getGaugeID())){
                        markerList.remove(selectedMarker);
                        selectedMarker = marker;
                    }
                }else { //end selectedMarker added code

                    i++;
                    Marker marker = mMap.addMarker(iconChangedOptions(mContext,new MarkerOptions().position(latLng).title(gauge.getGaugeName()), zoom));
                    marker.setTag(gauge);
                    markerList.add(marker);
                    nMarkerList.add(marker);
                }

            }
            Log.d("markersAdded5", "number of markers added: " + i);
        }
        addMarkersToDB(nMarkerList);
        Log.d("markersAdded2", "finish");
        //invisibleLayout.setVisibility(View.GONE);
        Log.d("markersAdded4", String.valueOf(invisibleLayout.getVisibility()));
    }

    private void addMarkersToDB(List<Marker> mList){

        GaugeApplication.myDBHelper.addMarkers(mList);
    }

    private boolean checkMarkers(String identifier){

        return GaugeApplication.myDBHelper.checkMarkerExists(identifier);
    }


    private static MarkerOptions iconChangedOptions (Context context, MarkerOptions markerOptions, Float zoom){



        if(zoom == zoomLevel[0]){
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(context,60, 60)));
        }else if(zoom == zoomLevel [1]){
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(context,60, 60)));
        }else if(zoom == zoomLevel[2]){
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(context,40, 40)));
        }else{
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(context,20, 20)));
        }
        return markerOptions;
    }


    private static void resetSelectedMarker(Context context){

        Gauge oldGauge = (Gauge) selectedMarker.getTag();
        markerList.remove(selectedMarker);
        selectedMarker.remove();
        LatLng oldLatLng = new LatLng(oldGauge.getGaugeLatitude(),oldGauge.getGaugeLongitude());
        Marker oldMarker = mMap.addMarker(iconChangedOptions(context,new MarkerOptions().position(oldLatLng).title(oldGauge.getGaugeName()),previousZoom));
        oldMarker.setTag(oldGauge);
        markerList.add(oldMarker);

    }

    private static Bitmap resizeMapIcons(Context context,int width, int height){

        Log.d("bitmap1","start");
        //Log.d("mapPosition6", "drawable value: " + String.valueOf(getResources().getDrawable(R.drawable.marker_circle)));

        Drawable drawable = context.getResources().getDrawable(R.drawable.marker_circle);
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
        Log.d("bitmap2","finish");
        return resizedBitmap;
    }

    @Override
    public boolean onMarkerClick(final Marker marker){


        Gauge gauge = (Gauge)marker.getTag();
        searchView.clearFocus();
        if(gauge != null){
            if(selectedMarker != null) {
                if (!((Gauge) selectedMarker.getTag()).getGaugeID().equals(gauge.getGaugeID())) {
                    Log.d("markerStuff7", "different Marker clicked");
                    resetSelectedMarker(mContext);
                } else {
                    Log.d("markerStuff7", "same Marker clicked");
                    //do nothing
                }
            }
            markerList.remove(marker);
            marker.remove();
            LatLng latLng = new LatLng(gauge.getGaugeLatitude(),gauge.getGaugeLongitude());
            Marker nMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(gauge.getGaugeName()));
            nMarker.setTag(gauge);
            selectedMarker = nMarker;
            Log.d("markerStuff2",String.valueOf(selectedMarker));
            Log.d("markerStuff3",((Gauge)selectedMarker.getTag()).getGaugeID());
            markerList.add(nMarker);
            Log.d("markerClick","gauge clicked on: " + gauge.getGaugeName() + " " + gauge.getGaugeID() + " " + gauge.getGaugeURL());
            //loadGaugeTask = new LoadGauge();
            //loadGaugeTask.execute(gauge);
            if(gaugeDataLayout.getVisibility() != View.VISIBLE){
                gaugeDataLayout.setVisibility(View.VISIBLE);
            }
            Bundle bundle = new Bundle();
            bundle.putSerializable("gauge",gauge);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            LoadGaugeFragment loadGaugeFragment = new LoadGaugeFragment();
            loadGaugeFragment.setArguments(bundle);
            if(fragmentManager.findFragmentByTag("load_gauge_fragment") != null) {
                fragmentTransaction.replace(R.id.gauge_data_layout, loadGaugeFragment, "load_gauge_fragment");
            }else{
                fragmentTransaction.replace(R.id.gauge_data_layout, loadGaugeFragment, "load_gauge_fragment");
            }
            fragmentTransaction.commit();
            return false;
        }
        else{
            Log.d("markerClick","gauge clicked on home");
            return true;
        }

    }

    private static class GetGaugesParams{
        Context context;
        List<Gauge> gauges;
        SupportMapFragment mapFragment;
        private GetGaugesParams(Context context, List<Gauge> gauges, SupportMapFragment mapFragment){
            this.context = context;
            this.gauges = gauges;
            this.mapFragment = mapFragment;
        }

    }

    private static class GetGauges extends AsyncTask<GetGaugesParams, Void, GetGaugesParams>{
        @Override
        protected GetGaugesParams doInBackground(GetGaugesParams... params){

            Context context = params[0].context;
            SupportMapFragment mapFragment = params[0].mapFragment;
            Log.d("getGauges",String.valueOf(homeLocation));
            allGauges = getAllGauges();
            GetLocations getLocations = new GetLocations(homeLocation,allGauges);
            return new GetGaugesParams(context,getLocations.getClosestGauges(250),mapFragment);
        }
        @Override
        protected void onPostExecute(GetGaugesParams result){


            Context context = result.context;
            myGaugeList = result.gauges;
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = result.mapFragment;
            mapFragment.getMapAsync((OnMapReadyCallback) context);


        }

        private List<Gauge> getAllGauges(){

            return GaugeApplication.myDBHelper.getAllGauges();
        }
    }









    private static class MoveLocationParams{
        String id;
        Gauge gauge;
        LinearLayout gaugeDataLayout;
        FragmentManager fragmentManager;
        private MoveLocationParams(String id, Gauge gauge, LinearLayout gaugeDataLayout, FragmentManager fragmentManager){
            this.id = id;
            this.gauge = gauge;
            this.gaugeDataLayout = gaugeDataLayout;
            this.fragmentManager = fragmentManager;
        }
    }




    private static class MoveToLocation extends AsyncTask<MoveLocationParams,Void,MoveLocationParams>{

        @Override
        protected MoveLocationParams doInBackground(MoveLocationParams... params){

            String id = params[0].id;
            LinearLayout gaugeDataLayout = params[0].gaugeDataLayout;
            FragmentManager fragmentManager = params[0].fragmentManager;
            Gauge gauge = GaugeApplication.myDBHelper.getLocationFromIdentifier(id);
            if(gauge.getGaugeName() != null){
                return new MoveLocationParams(id,gauge,gaugeDataLayout,fragmentManager);
            }
            return null;
        }

        @Override
        protected void onPostExecute(MoveLocationParams moveLocationParams){

            if(moveLocationParams != null) {

                Gauge gauge = moveLocationParams.gauge;
                LinearLayout gaugeDataLayout = moveLocationParams.gaugeDataLayout;
                FragmentManager fragmentManager = moveLocationParams.fragmentManager;

                mMap.clear();
                markerList.clear();
                selectedMarker = null;
                GaugeApplication.myDBHelper.clearMarkers();
                Marker myLocationMarker = mMap.addMarker(new MarkerOptions().
                        position(myLatLng).icon(BitmapDescriptorFactory.
                        defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).title("My Location"));
                myLocationMarker.setTag(null);

                Marker marker = mMap.addMarker(new MarkerOptions().position(new LatLng(gauge.getGaugeLatitude(),gauge.getGaugeLongitude())).title(gauge.getGaugeName()));
                marker.setTag(gauge);
                selectedMarker = marker;
                marker.remove();
                selectedMarker.setTag(gauge);
                Log.d("markerStuff10",String.valueOf(selectedMarker) + " , " + ((Gauge)selectedMarker.getTag()).getGaugeID());


                LatLng latLng = new LatLng(gauge.getGaugeLatitude(), gauge.getGaugeLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));

                if(gaugeDataLayout.getVisibility() != View.VISIBLE){
                    gaugeDataLayout.setVisibility(View.VISIBLE);
                }

                Bundle bundle = new Bundle();
                bundle.putSerializable("gauge",gauge);
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                LoadGaugeFragment loadGaugeFragment = new LoadGaugeFragment();
                loadGaugeFragment.setArguments(bundle);
                if(fragmentManager.findFragmentByTag("load_gauge_fragment") != null) {
                    fragmentTransaction.replace(R.id.gauge_data_layout, loadGaugeFragment, "load_gauge_fragment");
                }else{
                    fragmentTransaction.replace(R.id.gauge_data_layout, loadGaugeFragment, "load_gauge_fragment");
                }
                fragmentTransaction.commit();
            }
        }
    }

    private static class UpdateDBParams{
        Context context;
        boolean dbEmpty;
        SupportMapFragment mapFragment;
        private UpdateDBParams(Context context, boolean dbEmpty,SupportMapFragment mapFragment){
            this.context = context;
            this.dbEmpty = dbEmpty;
            this.mapFragment = mapFragment;
        }
    }

    //Makes sure the DB data is up to date with the latest source XML data from MY_URL
    private static class UpdateDataBaseTask extends AsyncTask<UpdateDBParams,Void,UpdateDBParams>{

        /*
         *checks if the DB data version and the source XML data version match
         *if versions match, continue without updating DB
         *if versions don't match, download the data from source and update the DB
         */
        @Override
        protected UpdateDBParams doInBackground(UpdateDBParams...params){

            boolean dbEmpty;
            SupportMapFragment mapFragment = params[0].mapFragment;
            List<Gauge> gaugeList = new ArrayList<Gauge>();
            int xmlVersion = checkXMLVersion();
            int dbVersion = checkDataBaseVersion();
            if(dbVersion != xmlVersion && xmlVersion != -1){
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
            if(GaugeApplication.myDBHelper.getGaugesCount() > 0){
                dbEmpty = false;
            }else {
                dbEmpty = true;
            }

            Context context = params[0].context;
            return new UpdateDBParams(context,dbEmpty,mapFragment);
        }

        @Override
        protected void onPostExecute(UpdateDBParams result){

            SupportMapFragment mapFragment = result.mapFragment;
            boolean databaseEmpty = result.dbEmpty;
            Context context = result.context;
            Location location = new Location("");

            //set a default location if location data cannot be obtained
            if(homeLocation == null){
                location.setLatitude(38.904722);
                location.setLongitude(-77.016389);
            }else{
                location = homeLocation;
            }
            //if we had to set the default default location, use a middle zoom
            //if we were able to obtain user's location, use closest zoom
            if(homeLocation == null){
                homeLocation = location;
                mapZoomLevel = MIDDLE_ZOOM;
            }else{
                mapZoomLevel = CLOSEST_ZOOM;
            }
            previousLocation = homeLocation;

            if(databaseEmpty) {
                CharSequence text = "Error: Could not obtain gauge data";
                Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
                toast.show();
            }

            GetGaugesParams params = new GetGaugesParams(context,null,mapFragment);
            GetGauges task = new GetGauges();
            task.execute(params);
        }

        /**
         * Check the version of the data in Table.Gauges
         * @return Returns the version as an Integer
         */
        private int checkDataBaseVersion(){

            return GaugeApplication.myDBHelper.getDataVersion();
        }

        /**
         * Check if data is enabled, then check the version of the XML source
         * @return returns the source XML version as an Integer
         */
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

        /**
         *Downloads the source data from MY_URL and sends it to WeatherXmlParser for parsing
         * @return Returns a List of Gauges that has been parsed from the XML source data
         * @throws XmlPullParserException thrown if error in parsing XML data
         * @throws IOException thrown if error in reading data
         */
        private List<Gauge> downloadXML() throws XmlPullParserException, IOException {

            Log.d("downloadXML1","Start");
            InputStream stream = null;
            // Instantiate the parser
            WeatherXmlParser weatherXmlParser = new WeatherXmlParser();
            List<Gauge> gaugeList;

            try {
                stream = downloadUrl(MY_URL);
                gaugeList = weatherXmlParser.parse(stream);
                // Makes sure that the InputStream is closed after the app is finished using it.
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
            Log.d("downloadXML2","Finish, gauge list size: " + gaugeList.size());
            return gaugeList;
        }

        /**
         * Receives a list of Gauges to add to the DB, along with the version number of the XML source
         * @param gaugeList List<Gauge> to be added to the database
         * @param version Version number as Integer of the XML to be appended to each DB entry
         */
        private void addGaugesToDB(List<Gauge> gaugeList, int version){

            GaugeApplication.myDBHelper.addGauges(gaugeList,version);

        }

        /**
         * Returns the version of the XML file hosted at MY_URL as an Integer
         * Uses CheckXMLVersion to parse the data received from downloadUrl()
         * @param urlString The URL as a string
         * @return Returns the version of the XML file as an Integer
         * @throws XmlPullParserException if an error occurs while parsing the XML data
         * @throws IOException if there is an error while reading the data
         */
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

        /**
         * Returns the source data from MY_URL as an InputStream
         * @param urlString Receives the URL as a string
         * @return Returns the data as an InputStream
         * @throws IOException if an error occurs while reading the site data
         */
        private InputStream downloadUrl(String urlString) throws IOException {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            //don't use a cached version
            conn.setUseCaches(false);
            conn.setDefaultUseCaches(false);
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            return conn.getInputStream();
        }
    }

    /**
     * Class used to store parameters needed for LoadFragmentGaugeAsync
     */
    private static class LoadFragmentGaugeParams{
        Gauge gauge;
        GaugeFragParams gaugeFragParams;
        FragmentManager fragmentManager;
        private LoadFragmentGaugeParams(Gauge gauge, GaugeFragParams gaugeFragParams, FragmentManager fragmentManager){
            this.gauge = gauge;
            this.gaugeFragParams = gaugeFragParams;
            this.fragmentManager = fragmentManager;
        }
    }

    /*Loads the FragmentGauge that displays all information about the selected Gauge
     *Receives the selected Gauge as a parameter
     */
    private static class LoadFragmentGaugeAsync extends AsyncTask<LoadFragmentGaugeParams,Void,LoadFragmentGaugeParams>{

        //Check if Gauge is a favorite and has notifications available
        //Bundled into GaugeFragParams obj and passed to onPostExecute
        @Override
        protected LoadFragmentGaugeParams doInBackground(LoadFragmentGaugeParams... params){

            Gauge gauge = params[0].gauge;
            FragmentManager fragmentManager = params[0].fragmentManager;
            boolean isFavorite = GaugeApplication.myDBHelper.isFavorite(gauge);
            Log.d("fragFave",String.valueOf(isFavorite));
            int notificationState = 0;
            if(isFavorite) {
                notificationState = GaugeApplication.myDBHelper.getFavoriteNotificationState(gauge);
            }
            boolean isNotifiable = true;
            if(notificationState == 0){
                isNotifiable = false;
            }

            GaugeFragParams gaugeFragParams = new GaugeFragParams(gauge,isFavorite,isNotifiable);
            return new LoadFragmentGaugeParams(null,gaugeFragParams,fragmentManager);
        }

        /*Creates a Bundle that includes two instances of the selected gauge, and two booleans,
         *one for if notifications are enabled and one for if a favorite
         */
        @Override
        protected void onPostExecute(LoadFragmentGaugeParams result){

            GaugeFragParams params = result.gaugeFragParams;
            FragmentManager fragmentManager = result.fragmentManager;
            Bundle bundle = new Bundle();
            /*selected_tag used as a check by FragmentGauge to return MainFragActivity to the
             *correct state
             */
            bundle.putSerializable("selected_gauge",params.getGauge());
            bundle.putSerializable("gauge",params.getGauge());
            bundle.putBoolean("isFavorite",params.isFavorite());
            bundle.putBoolean("isNotifiable",params.isNotifiable());
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            FragmentGauge fragmentGauge = new FragmentGauge();
            fragmentGauge.setArguments(bundle);
            fragmentTransaction.replace(R.id.main_layout,fragmentGauge,"gauge_fragment").addToBackStack("Tag");
            fragmentTransaction.commit();

        }
    }


    /**
     * Class used to store parameters needed for AsyncUpdateLocation
     */
    private class UpdateLocationParams{
        private Context context;
        private FusedLocationProviderClient providerClient;
        private Location location;

        private UpdateLocationParams(Context context, FusedLocationProviderClient providerClient){
            this.context = context;
            this.providerClient = providerClient;
        }
        private UpdateLocationParams(Context context, FusedLocationProviderClient providerClient, Location location){
            this.context = context;
            this.providerClient = providerClient;
            this.location = location;
        }

        public Context getContext() {
            return context;
        }

        public FusedLocationProviderClient getProviderClient() {
            return providerClient;
        }

        public void setContext(Context context) {
            this.context = context;
        }

        public void setLocation(Location location) {
            this.location = location;
        }

        public Location getLocation() {
            return location;
        }

        public void setProviderClient(FusedLocationProviderClient providerClient) {
            this.providerClient = providerClient;
        }
    }

    /**
     * Async class used to update location when button is pressed
     * Receives UpdateLocationParams and passes a new UpdateLocationParams to onPostExecute
     * Waits for two seconds or for location to be updated using a background thread
     * When the background task is complete, if static Location updatedLocation comes back null
     * because it was not updated within two seconds, getLastKnownLocation is called and updatedLocation
     * takes its value
     * Camera is then moved to updatedLocation
     *
     */
    private static class AsyncUpdateLocation extends AsyncTask<UpdateLocationParams,Void,UpdateLocationParams>{

        @Override
        protected UpdateLocationParams doInBackground(UpdateLocationParams... params){

            UpdateLocationParams updateLocationParams = params[0];
            Log.d("getLocationUpdate9Async",String.valueOf(updatedLocation));
            Long future = System.currentTimeMillis() + 2000;
            while(updatedLocation == null && System.currentTimeMillis()<future){
                Log.d("getLocationUpdate9Async",String.valueOf(updatedLocation));
            }

            updateLocationParams.setLocation(updatedLocation);
            return updateLocationParams;

        }

        @Override
        protected void onPostExecute(UpdateLocationParams params){

            Context context = params.getContext();
            FusedLocationProviderClient providerClient = params.getProviderClient();
            Location location = params.getLocation();
            //only want location updates running until a location is found
            stopLocationUpdates(providerClient);
            if(location != null) {
                Log.d("getLocationUpdate7Async", location.getLatitude() + ", " + location.getLongitude());
            }
            if(location == null){
                Log.d("getLocationUpdate7Async", "ran out of time");
                getLastKnownLocation(context,providerClient);
                location = homeLocation;
            }

            LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
            moveCamera(latLng,latLng,CLOSEST_ZOOM);


        }
    }

    /**
     * Method used to start the process of updating the location by defining the LocationCallback
     * When LocationCallback mLocationCallback is called from startLocationUpdates(),
     * static Location updatedLocation gets updated with the most recent location obtained
     * After defining LocationCallback, startLocationUpdates() is called
     * @param context Activity Context that must be passed to startLocationUpdates()
     * @param mFusedLocationProviderClient FusedLocationProviderClient that must be passed to
     *                                     startLocationUpdates()
     */
    private static void updateLocation(Context context, FusedLocationProviderClient mFusedLocationProviderClient) {

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.d("getLocationUpdateAsync5","in onLocationResult");
                if (locationResult == null) {
                    Log.d("getLocationUpdateAsync3","location result is null");
                    return;
                }else{
                    Log.d("getLocationUpdateAsync4","location result is not null");
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    // ...

                    Log.d("getLocationUpdateAsync",location.getLatitude() + ", " + location.getLongitude());
                    updatedLocation = location;
                }
            }
        };

        startLocationUpdates(context,mFusedLocationProviderClient);
    }

    /**
     * Checks if location permission is enabled, then gets the last known location
     * Updates the static Location homeLocation with new last known location
     * @param context Activty Context
     * @param mFusedLocationClient FusedLocationProviderClient used to get the location
     */
    public static void getLastKnownLocation(Context context, FusedLocationProviderClient mFusedLocationClient){

        //run the code if location permission is enabled
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.getLastLocation().addOnSuccessListener((Activity)context, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    Log.d("location5", "logging");
                    if (location != null) {
                        //do location stuff
                        Log.d("location10", location.getLatitude() + ", " + location.getLongitude());
                        homeLocation = location;
                    }
                }
            });
        }
    }

    /**
     * Moves the map camera to a new position
     * Clears all the markers from the map and from storage
     * Adds the user's location to the map
     * @param homeLatLngPos The lat long coordinates of user's location
     * @param targetLatLngPos The lat long coordinates of where the camera will move to
     * @param zoom Floating point that represents the zoom level of the camera
     */

    private static void moveCamera(LatLng homeLatLngPos, LatLng targetLatLngPos, float zoom){

        mMap.clear();
        markerList.clear();
        selectedMarker = null;
        GaugeApplication.myDBHelper.clearMarkers();
        Marker myLocationMarker = mMap.addMarker(new MarkerOptions().
                position(homeLatLngPos).icon(BitmapDescriptorFactory.
                defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).title("My Location"));
        myLocationMarker.setTag(null);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLatLngPos, zoom));


    }

    /**
     * Clears the views from the map
     * Clears search text and removes focus to hide keyboard
     * Hides the layout that holds the LoadGaugeFragment
     * Resets the selected marker to null
     * @param context Activity's context
     * @param searchView Activity's searchView that will be cleared
     * @param gaugeDataLayout Linear Layout that holds the LoadGaugeFragment fragment
     */
    private static void clearViews(Context context,SearchView searchView, LinearLayout gaugeDataLayout){
        searchView.setQuery("",false);
        searchView.clearFocus();
        if(gaugeDataLayout.getVisibility() != View.GONE) {
            gaugeDataLayout.setVisibility(View.GONE);
            if(selectedMarker != null){
                resetSelectedMarker(context);
                selectedMarker = null;
            }
        }

    }



}
