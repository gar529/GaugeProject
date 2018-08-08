package com.gregrussell.fenwickguageapp;

import android.Manifest;
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

    private GoogleMap mMap;
    List<Gauge> myList;
    List<Gauge> allGauges;
    LinearLayout gaugeDataLayout;
    private int distanceAway;
    //private Location location;
    private Context mContext;
    private List<Marker> markerList;
    private float zoomLevel[] = {11,10,8,7,0};
    private float currentZoomLevel = 0;
    private Location previousLocation;
    final double MILE_CONVERTER = .000621371;
    private LatLng myLatLng;
    private LatLng searchLatLng;
    private DataBaseHelperGauges myDBHelper;
    private int bitmapCounter;
    private RelativeLayout invisibleLayout;
    private SimpleCursorAdapter mAdapter;
    private SearchView searchView;
    private Marker selectedMarker;
    private ListView searchSuggestions;
    private String mCurFilter;
    private ImageView favoriteButton;
    private LoadGauge loadGaugeTask;

    public static final String WIFI = "Wi-fi";
    public static final String ANY = "Any";
    private static final String MY_URL = "https://raw.githubusercontent.com/gar529/GaugeProject/master/GaugeProject/xmlData.xml";
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 0;
    private static Location homeLocation;
    public static String sPref = null;
    // Whether there is a Wi-Fi connection.
    private static boolean wifiConnected = true;
    // Whether there is a mobile connection.
    private static boolean mobileConnected = false;
    public static final String CHANNEL_ID = "Flood Warning";



    @Override
    public void onBackPressed(){

        Log.d("backpressed",String.valueOf(getSupportFragmentManager().findFragmentByTag("favorite_fragment")));

        if(getSupportFragmentManager().findFragmentByTag("favorite_fragment") == null && getSupportFragmentManager().findFragmentByTag("gauge_fragment") == null) {
            if (gaugeDataLayout.getVisibility() == View.VISIBLE) {
                gaugeDataLayout.setVisibility(View.GONE);
                if (selectedMarker != null) {
                    resetSelectedMarker();
                    selectedMarker = null;
                }
            } else if (searchSuggestions.getCount() > 0) {
                searchView.setQuery("", false);
                searchView.clearFocus();
                if (selectedMarker != null) {
                    resetSelectedMarker();
                    selectedMarker = null;
                }
            } else {
                super.onBackPressed();
            }
        }else{
            super.onBackPressed();
        }

    }



    @Override
    public void onResume(){
        super.onResume();

        Log.d("onResumeMain","onResume");
        if(selectedMarker != null){
            Gauge gauge = (Gauge)selectedMarker.getTag();
            loadGaugeTask = new LoadGauge();
            loadGaugeTask.execute(gauge);
        }else{
            if(gaugeDataLayout.getVisibility() != View.GONE)
            gaugeDataLayout.setVisibility(View.GONE);
        }

    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    getMyLocation();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.




                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.permission_dialog_message)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                    getMyLocation();
                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
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
        setContentView(R.layout.map_fragment);
        Log.d("Timer","Finish");

        mContext = this;


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

        ComponentName receiver = new ComponentName(this, StartAlarmAtBoot.class);
        PackageManager pm = this.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        createNotificationChannel();

        AlarmManager alarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this,AlarmReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this,0,intent,0);
        intent.setAction("com.gregrussell.alarmtest.SEND_BROADCAST");

        if(Build.VERSION.SDK_INT >= 23) {
            alarmMgr.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + Constants.FIFTEEN_MINUTES_MILLIS, alarmIntent);
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            alarmMgr.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + Constants.FIFTEEN_MINUTES_MILLIS, alarmIntent);
        }else{
            Random random = new Random();
            int randomTimeMillis = random.nextInt(Constants.UPPER_BOUND_MILLIS - Constants.LOWER_BOUND_MILLIS) + Constants.LOWER_BOUND_MILLIS;
            alarmMgr.set(AlarmManager.ELAPSED_REALTIME,SystemClock.elapsedRealtime() + randomTimeMillis,alarmIntent);
        }

        getLocationPermission();





        //myList = (ArrayList<Gauge>)getIntent().getSerializableExtra("LIST_OF_RESULTS");
        //distanceAway = getIntent().getIntExtra("DISTANCE",5);
        //location = getIntent().getParcelableExtra("MY_LOCATION");

        invisibleLayout = (RelativeLayout) findViewById(R.id.invisible_layout);


        myDBHelper = new DataBaseHelperGauges(mContext);
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
                        resetSelectedMarker();
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
                            resetSelectedMarker();
                            selectedMarker = null;
                        }
                    }
                }

            }
        });

        FloatingActionButton floatingActionButton = (FloatingActionButton)findViewById(R.id.floating_button_map);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                FragmentFavorites fragmentFavorites = new FragmentFavorites();
                fragmentTransaction.add(R.id.main_layout, fragmentFavorites, "favorite_fragment").addToBackStack("Tag");
                fragmentTransaction.commit();
            }
        });

        gaugeDataLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(selectedMarker != null){



                    Gauge gauge = (Gauge)selectedMarker.getTag();
                    LoadGaugeFragment loadGaugeFragment = new LoadGaugeFragment();
                    loadGaugeFragment.execute(gauge);
                }
            }
        });



        View closeButton = findViewById(getResources().getIdentifier("android:id/search_close_btn", null, null));
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("searchClose","clicked");
                searchView.setQuery("",false);
                searchView.clearFocus();
                if(gaugeDataLayout.getVisibility() != View.GONE) {
                    gaugeDataLayout.setVisibility(View.GONE);
                    if(selectedMarker != null){
                        resetSelectedMarker();
                        selectedMarker = null;
                    }
                }

            }
        });

        favoriteButton = (ImageView)findViewById(R.id.favorite_button);
        favoriteButton.setClickable(true);
        favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(favoriteButton.isSelected()){
                    favoriteButton.setSelected(false);
                    Gauge gauge = (Gauge)selectedMarker.getTag();
                    RemoveFavorite task = new RemoveFavorite();
                    task.execute(gauge);
                }else{
                    favoriteButton.setSelected(true);
                    if(selectedMarker != null){
                        Gauge gauge = (Gauge)selectedMarker.getTag();
                        AddFavorite task = new AddFavorite();
                        task.execute(gauge);
                    }
                }
            }
        });





        searchSuggestions = (ListView)findViewById(R.id.search_suggestions);

        mAdapter = new SimpleCursorAdapter(this,android.R.layout.simple_list_item_2, null,
                new String[] {SearchManager.SUGGEST_COLUMN_TEXT_1,SearchManager.SUGGEST_COLUMN_TEXT_2}, new int[] {android.R.id.text1, android.R.id.text2},0);
        searchSuggestions.setAdapter(mAdapter);
        getLoaderManager().initLoader(0,null,this);

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


                //Log.d("intent4",getIntent().getDataString());
                //handleIntent(getIntent());
            }
        });

        Log.d("searchView3", "adapter empty? " + mAdapter.isEmpty());

        myDBHelper.clearMarkers();





    }


    @Override
    protected void onNewIntent(Intent intent){
        setIntent(intent);
        Log.d("intent5","on click");
        Log.d("intent6", String.valueOf(getIntent().getAction()));



        handleIntent(intent);
    }

    private void getLocationPermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("locations8", "permission not granted");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                getMyLocation();
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }else {
            getMyLocation();
        }
    }

    private void getMyLocation(){
        final FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        //mFusedLocationClient.setMockLocation(loc);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            OpenDataBaseTask task = new OpenDataBaseTask();
            task.execute();

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
                        homeLocation = location;

                        //loadPage();
                        //runGeo(location);


                    } else {
                        Log.d("location2", String.valueOf(location));
                    }
                }
            });
            OpenDataBaseTask task = new OpenDataBaseTask();
            task.execute();
        }



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

    private void handleIntent(Intent intent){

        if(intent.getStringExtra("notification") != null){
            Log.d("intent30","from notification");

            FragmentManager fragmentManager = getSupportFragmentManager();

            if(fragmentManager.findFragmentByTag("favorite_fragment") != null ){
                fragmentManager.beginTransaction().remove(fragmentManager.findFragmentByTag("favorite_fragment")).commit();
                fragmentManager.popBackStack();

                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                FragmentFavorites fragmentFavorites = new FragmentFavorites();
                fragmentTransaction.add(R.id.main_layout, fragmentFavorites, "favorite_fragment").addToBackStack("Tag");
                fragmentTransaction.commit();

            }else {
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                FragmentFavorites fragmentFavorites = new FragmentFavorites();
                fragmentTransaction.add(R.id.main_layout, fragmentFavorites, "favorite_fragment").addToBackStack("Tag");
                fragmentTransaction.commit();
            }

        }else {
            if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
                String query = intent.getStringExtra(SearchManager.QUERY);
                Log.d("intent7", "search query: " + query);
                search(query);
                searchView.clearFocus();
                searchSuggestions.setAdapter(null);

            } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                Uri data = intent.getData();
                Log.d("intent1", data.getLastPathSegment());
            } else {
                String dataString = intent.getStringExtra("DATA");
                if (dataString != null) {
                    Log.d("intent20", dataString);

                    MoveToLocation task = new MoveToLocation();
                    task.execute(dataString);

                } else {
                    Log.d("intent21", String.valueOf(dataString));
                }
            }
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setDescription(description);
            channel.setVibrationPattern(new long[]{1000, 1000, 1000});
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }




    private void search(String query){

        Geocoder geo = new Geocoder(this, Locale.getDefault());
        List<Address> addressList = new ArrayList<Address>();
        Address address;
        Boolean validAddress = false;
        try {
            addressList = geo.getFromLocationName(query, 10);
        }catch (IOException e){
            e.printStackTrace();
        }

        if(addressList.size() > 0){


            int i = 0;
            do{

                address = addressList.get(i);
                Log.d("searchAddress4",address.getLocality() + ", " + address.getAdminArea() + ", " + address.getCountryCode() + "   " + addressList.size());
                i++;
            }
            while( i < addressList.size() && !address.getCountryCode().equals("US"));

            Log.d("searchAddress5",String.valueOf(address.getCountryCode().equals("US")));

            if(address.getCountryCode().equals("US")){
                validAddress = true;
                LatLng latLng = new LatLng(address.getLatitude(),address.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,12));
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

    @Override public boolean onQueryTextSubmit(String query) {
        // Don't care about this.
        search(query);
        return true;
    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        // Insert desired behavior here.
        Log.i("FragmentComplexList", "Item clicked: " + id);
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
        mMap.setMinZoomPreference(7.0f);

        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setCompassEnabled(true);

        Log.d("compass", String.valueOf(mMap.getUiSettings().isCompassEnabled()));
        setCompassPosition();


        // Add a marker in Sydney and move the camera
        myLatLng = new LatLng(homeLocation.getLatitude(),homeLocation.getLongitude());
        //LatLng farthestLocation = new LatLng(38.830833,-77.134722);

        LatLng sydney = new LatLng(-34, 151);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                Log.d("mapClick","click");

                searchView.setQuery("",false);
                searchView.clearFocus();
                gaugeDataLayout.setVisibility(View.GONE);
                if(selectedMarker != null){
                    resetSelectedMarker();
                    selectedMarker = null;
                }
            }
        });
        Marker myLocationMarker = mMap.addMarker(new MarkerOptions().
                position(myLatLng).icon(BitmapDescriptorFactory.
                defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).title("My Location"));
        myLocationMarker.setTag(null);
        markerList = new ArrayList<Marker>();

        //addMarkers(myList,mMap.getCameraPosition().zoom);

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

                if(previousLocation.distanceTo(myLocation) > 5 * MILE_CONVERTER){
                    GetLocations gl = new GetLocations(myLocation, allGauges);
                    List<Gauge> gaugeList = gl.getClosestGauges(250);
                    List[] gaugeListArray = gl.getClosestGaugesArray();

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
                    if(mZoom == currentZoomLevel && mZoom == zoomLevel[0]){
                        Log.d("addMapMarkers2","zoomLevel 0");
                        List<Gauge> mList = gaugeListArray[0];

                       /*AddMarkerParams params = new AddMarkerParams(mList,zoom);
                        AddMarkerAsync task = new AddMarkerAsync();
                        task.execute(params);*/
                        addMarkersUIThread(mList,zoom);
                    }
                    else if(mZoom == currentZoomLevel && mZoom == zoomLevel[1]){
                        Log.d("addMapMarkers3","zoomLevel 1");
                        List<Gauge> mList = gaugeListArray[1];
                        /*AddMarkerParams params = new AddMarkerParams(mList,zoom);
                        AddMarkerAsync task = new AddMarkerAsync();
                        task.execute(params);*/
                        addMarkersUIThread(mList,zoom);
                    }
                    else if(mZoom == currentZoomLevel && mZoom == zoomLevel[2]){
                        Log.d("addMapMarkers4","zoomLevel 2");
                        List<Gauge> mList = gaugeListArray[2];
                        /*AddMarkerParams params = new AddMarkerParams(mList,zoom);
                        AddMarkerAsync task = new AddMarkerAsync();
                        task.execute(params);*/
                        addMarkersUIThread(mList,zoom);
                    }
                    else if(mZoom == currentZoomLevel && mZoom == zoomLevel[3]){
                        Log.d("addMapMarkers5","zoomLevel 3");
                        List<Gauge> mList = gaugeListArray[3];
                        /*AddMarkerParams params = new AddMarkerParams(mList,zoom);
                        AddMarkerAsync task = new AddMarkerAsync();
                        task.execute(params);*/
                        addMarkersUIThread(mList,zoom);
                    }
                    else if(mZoom == currentZoomLevel && mZoom == zoomLevel[4]){
                        Log.d("addMapMarkers6","zoomLevel 4");
                        //addMarkers(gaugeList,zoom);
                        List<Gauge> mList = gaugeListArray[4];
                        /*AddMarkerParams params = new AddMarkerParams(mList,zoom);
                        AddMarkerAsync task = new AddMarkerAsync();
                        task.execute(params);*/
                        addMarkersUIThread(mList,zoom);
                    }else{
                        Log.d("addMapMarkers1","clearing map");
                        Log.d("addMapMarkers7", "mZoom is: " + mZoom + ", currentZoom is: " + currentZoomLevel);

                        if(selectedMarker !=null) {
                            Log.d("markerStuff5", ((Gauge) selectedMarker.getTag()).getGaugeID());
                            Gauge gauge = (Gauge) selectedMarker.getTag();
                            mMap.clear();
                            markerList.clear();
                            selectedMarker.setTag(gauge);

                        }else {
                            mMap.clear();
                            markerList.clear();
                        }

                        Log.d("markerStuff4",String.valueOf(selectedMarker));
                        if(selectedMarker != null) {
                            markerList.add(selectedMarker); //selectedMarker code
                            Log.d("markerStuff5",((Gauge)selectedMarker.getTag()).getGaugeID());
                        }

                        myDBHelper.clearMarkers();
                        Marker myLocationMarker = mMap.addMarker(new MarkerOptions().
                                position(myLatLng).icon(BitmapDescriptorFactory.
                                defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).title("My Location"));
                        myLocationMarker.setTag(null);
                        /*for(int i=0;i< gaugeList.size();i++) {
                            LatLng gauge = new LatLng(gaugeList.get(i).getGaugeLatitude(), gaugeList.get(i).getGaugeLongitude());
                            //Marker marker = mMap.addMarker(new MarkerOptions().position(gauge).title(gaugeList.get(i).getGaugeName()));

                            Marker marker = mMap.addMarker(iconChangedOptions(new MarkerOptions().position(gauge).title(gaugeList.get(i).getGaugeName()), zoom));
                            marker.setTag(gaugeList.get(i));
                            markerList.add(marker);
                        }*/
                        if(mZoom == zoomLevel[0]){

                            List<Gauge> mList = gaugeListArray[0];
                            /*AddMarkerParams params = new AddMarkerParams(mList,zoom);
                            AddMarkerAsync task = new AddMarkerAsync();
                            task.execute(params);*/
                            addMarkersUIThread(mList,zoom);
                        }
                        else if(mZoom == zoomLevel[1]){
                            List<Gauge> mList = gaugeListArray[1];
                            /*AddMarkerParams params = new AddMarkerParams(mList,zoom);
                            AddMarkerAsync task = new AddMarkerAsync();
                            task.execute(params);*/
                            addMarkersUIThread(mList,zoom);
                        }
                        else if(mZoom == zoomLevel[2]){
                            List<Gauge> mList = gaugeListArray[2];
                            /*AddMarkerParams params = new AddMarkerParams(mList,zoom);
                            AddMarkerAsync task = new AddMarkerAsync();
                            task.execute(params);*/
                            addMarkersUIThread(mList,zoom);
                        }
                        else if(mZoom == zoomLevel[3]){
                            List<Gauge> mList = gaugeListArray[3];
                            /*AddMarkerParams params = new AddMarkerParams(mList,zoom);
                            AddMarkerAsync task = new AddMarkerAsync();
                            task.execute(params);*/
                            addMarkersUIThread(mList,zoom);
                        }
                        else if(mZoom == zoomLevel[4]){
                            //addMarkers(gaugeList,zoom);
                            List<Gauge> mList = gaugeListArray[4];
                            /*AddMarkerParams params = new AddMarkerParams(mList,zoom);
                            AddMarkerAsync task = new AddMarkerAsync();
                            task.execute(params);*/
                            addMarkersUIThread(mList,zoom);
                        }




                    }
                }

                if(zoom > zoomLevel[0]){

                    if(zoomLevel[0]!= currentZoomLevel) {
                        currentZoomLevel = zoomLevel[0];

                        for (int i = 0; i < markerList.size(); i++) {
                            if(selectedMarker != null && markerList.get(i) != selectedMarker) {
                                //markerList.get(i).setIcon(BitmapDescriptorFactory.defaultMarker());
                                markerList.get(i).setIcon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(60, 60)));
                            }
                        }

                    }

                }else if(zoom > zoomLevel [1] && zoom <= zoomLevel[0]){
                    if(zoomLevel[1]!= currentZoomLevel) {
                        currentZoomLevel = zoomLevel[1];
                        for (int i = 0; i < markerList.size(); i++) {
                            if(selectedMarker != null && markerList.get(i) != selectedMarker) {
                                markerList.get(i).setIcon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(60, 60)));
                            }

                        }
                    }
                }else if(zoom > zoomLevel[2] && zoom <= zoomLevel[1]){
                    if(zoomLevel[2]!= currentZoomLevel) {
                        currentZoomLevel = zoomLevel[2];
                        for (int i = 0; i < markerList.size(); i++) {
                            if(selectedMarker != null && markerList.get(i) != selectedMarker) {
                                markerList.get(i).setIcon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(40, 40)));
                            }

                        }
                    }
                }else if(zoom >= zoomLevel[3] && zoom <= zoomLevel[2]){
                    if(zoomLevel[3]!= currentZoomLevel) {
                        currentZoomLevel = zoomLevel[3];
                        for (int i = 0; i < markerList.size(); i++) {
                            if(selectedMarker != null && markerList.get(i) != selectedMarker) {
                                markerList.get(i).setIcon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(20, 20)));
                            }

                        }
                    }
                }else{
                    if(zoomLevel[4]!= currentZoomLevel) {
                        currentZoomLevel = zoomLevel[4];
                        for (int i = 0; i < markerList.size(); i++) {
                            if(selectedMarker != null && markerList.get(i) != selectedMarker) {
                                markerList.get(i).setIcon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(20, 20)));
                            }

                        }
                    }
                }



            }
        });










        //mMap.addMarker(new MarkerOptions().position(farthestLocation));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
        //zoom levels 5 miles - 12, 10miles - 11, 20miles - 10, 50miles - 8, 100miles - 7, 250miles - 6, 500miles < - 5

        int mapZoomLevel = 0;
        switch (distanceAway){
            case 5:
                mapZoomLevel = 12;
                break;
            case 10:
                mapZoomLevel = 11;
                break;
            case 20:
                mapZoomLevel = 10;
                break;
            case 50:
                mapZoomLevel = 8;
                break;
            default:
                mapZoomLevel = 5;
                break;



        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng,mapZoomLevel));
        searchView.clearFocus();
    }

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
        for(int i=0; i<gaugeList.size();i++){
            LatLng gauge = new LatLng(gaugeList.get(i).getGaugeLatitude(), gaugeList.get(i).getGaugeLongitude());
            if(!checkMarkers(gaugeList.get(i).getGaugeID())){
                //start selectedMarker added code
                if(selectedMarker != null){

                    Marker marker;
                    if(((Gauge)selectedMarker.getTag()).getGaugeID().equals(gaugeList.get(i).getGaugeID())){
                        marker = mMap.addMarker(new MarkerOptions().position(gauge).title(gaugeList.get(i).getGaugeName()));
                    }else {
                        marker = mMap.addMarker(iconChangedOptions(new MarkerOptions().position(gauge).title(gaugeList.get(i).getGaugeName()), zoom));
                    }
                    marker.setTag(gaugeList.get(i));
                    markerList.add(marker);
                    nMarkerList.add(marker);
                    Log.d("markerStuff",String.valueOf(selectedMarker) +"   " + ((Gauge)selectedMarker.getTag()).getGaugeID());
                    if(((Gauge)selectedMarker.getTag()).getGaugeID().equals(gaugeList.get(i).getGaugeID())){

                        markerList.remove(selectedMarker);

                        selectedMarker = marker;
                    }
                }else { //end selectedMarker added code

                    Marker marker = mMap.addMarker(iconChangedOptions(new MarkerOptions().position(gauge).title(gaugeList.get(i).getGaugeName()), zoom));
                    marker.setTag(gaugeList.get(i));
                    markerList.add(marker);
                    nMarkerList.add(marker);
                }

            }
        }
        addMarkersToDB(nMarkerList);
        Log.d("markersAdded2", "finish");
        //invisibleLayout.setVisibility(View.GONE);
        Log.d("markersAdded4", String.valueOf(invisibleLayout.getVisibility()));


    }


    private void addMarkersUIThread(List<Gauge> gaugeList, Float zoom){

        invisibleLayout.setVisibility(View.VISIBLE);
        long currentTime = System.currentTimeMillis();
        long futureTime = currentTime + 200;
        while(System.currentTimeMillis() < futureTime){

        }

        Log.d("markersAdded1", "STart");
        //invisibleLayout.setVisibility(View.VISIBLE);
        Log.d("markersAdded3", String.valueOf(invisibleLayout.getVisibility()));
        List<Marker> nMarkerList = new ArrayList<Marker>();
        Log.d("markersAdded5","loop started");
        for(int i=0; i<gaugeList.size();i++){
            LatLng gauge = new LatLng(gaugeList.get(i).getGaugeLatitude(), gaugeList.get(i).getGaugeLongitude());
            if(!checkMarkers(gaugeList.get(i).getGaugeID())){
                //start selectedMarker added code
                if(selectedMarker != null){

                    Marker marker;
                    if(((Gauge)selectedMarker.getTag()).getGaugeID().equals(gaugeList.get(i).getGaugeID())){
                        marker = mMap.addMarker(new MarkerOptions().position(gauge).title(gaugeList.get(i).getGaugeName()));
                    }else {
                        marker = mMap.addMarker(iconChangedOptions(new MarkerOptions().position(gauge).title(gaugeList.get(i).getGaugeName()), zoom));
                    }
                    marker.setTag(gaugeList.get(i));
                    markerList.add(marker);
                    nMarkerList.add(marker);
                    Log.d("markerStuff",String.valueOf(selectedMarker) +"   " + ((Gauge)selectedMarker.getTag()).getGaugeID());
                    if(((Gauge)selectedMarker.getTag()).getGaugeID().equals(gaugeList.get(i).getGaugeID())){

                        markerList.remove(selectedMarker);

                        selectedMarker = marker;
                    }
                }else { //end selectedMarker added code

                    Marker marker = mMap.addMarker(iconChangedOptions(new MarkerOptions().position(gauge).title(gaugeList.get(i).getGaugeName()), zoom));
                    marker.setTag(gaugeList.get(i));
                    markerList.add(marker);
                    nMarkerList.add(marker);
                }

            }
        }
        addMarkersToDB(nMarkerList);
        Log.d("markersAdded2", "finish");
        //invisibleLayout.setVisibility(View.GONE);
        Log.d("markersAdded4", String.valueOf(invisibleLayout.getVisibility()));

        addMarkers(gaugeList,zoom);
        invisibleLayout.setVisibility(View.GONE);

    }


    private void addMarkersToDB(List<Marker> mList){

        myDBHelper.addMarkers(mList);
    }

    private boolean checkMarkers(String identifier){

        return myDBHelper.checkMarkerExists(identifier);
    }


    private MarkerOptions iconChangedOptions (MarkerOptions markerOptions, Float zoom){

        if(zoom > zoomLevel[0]){
            //markerOptions.icon(BitmapDescriptorFactory.defaultMarker());
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(60, 60)));
        }else if(zoom > zoomLevel [1] && zoom <= zoomLevel[0]){
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(60, 60)));
        }else if(zoom > zoomLevel[2] && zoom <= zoomLevel[1]){
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(40, 40)));
        }else{
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(20, 20)));
        }
        return markerOptions;
    }


    private void resetSelectedMarker(){

        Gauge oldGauge = (Gauge) selectedMarker.getTag();
        markerList.remove(selectedMarker);
        selectedMarker.remove();
        LatLng oldLatLng = new LatLng(oldGauge.getGaugeLatitude(),oldGauge.getGaugeLongitude());
        Marker oldMarker = mMap.addMarker(iconChangedOptions(new MarkerOptions().position(oldLatLng).title(oldGauge.getGaugeName()),mMap.getCameraPosition().zoom));
        oldMarker.setTag(oldGauge);
        markerList.add(oldMarker);

    }

    public Bitmap resizeMapIcons(int width, int height){

        Log.d("bitmap1","start");
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
        Log.d("bitmap2","finish");
        return resizedBitmap;
    }

    @Override
    public boolean onMarkerClick(final Marker marker){

        if(loadGaugeTask != null) {
            if (loadGaugeTask.getStatus() == AsyncTask.Status.RUNNING || loadGaugeTask.getStatus() == AsyncTask.Status.PENDING) {
                loadGaugeTask.cancel(true);
                Log.d("loadGauge", "cancelled");
            }
        }
        Gauge gauge = (Gauge)marker.getTag();

        //ActivateFavoriteButton buttonTask = new ActivateFavoriteButton();
        //buttonTask.execute(gauge);




        searchView.clearFocus();
        if(gauge != null){
            if(selectedMarker != null) {
                if (!((Gauge) selectedMarker.getTag()).getGaugeID().equals(gauge.getGaugeID())) {
                    Log.d("markerStuff7", "different Marker clicked");
                    /*Gauge oldGauge = (Gauge) selectedMarker.getTag();
                    markerList.remove(selectedMarker);
                    selectedMarker.remove();
                    LatLng oldLatLng = new LatLng(oldGauge.getGaugeLatitude(),oldGauge.getGaugeLongitude());
                    Marker oldMarker = mMap.addMarker(iconChangedOptions(new MarkerOptions().position(oldLatLng).title(oldGauge.getGaugeName()),mMap.getCameraPosition().zoom));
                    oldMarker.setTag(oldGauge);
                    markerList.add(oldMarker);*/
                    resetSelectedMarker();


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
            loadGaugeTask = new LoadGauge();
            loadGaugeTask.execute(gauge);
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

            Log.d("getGauges",String.valueOf(homeLocation));
            allGauges = getAllGauges();
            GetLocations gl = new GetLocations(homeLocation,allGauges);
            return gl.getClosestGauges(250);
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

            return myDBHelper.getAllGauges();
        }
    }

    public class LoadGauge extends AsyncTask<Gauge, Void, LoadGaugeParams>{


        View loadingPanel = (View)findViewById(R.id.listLoadingPanel);
        View gaugeText = (View)findViewById(R.id.gauge_data_text);
        @Override protected void onPreExecute(){

            gaugeDataLayout.setVisibility(View.VISIBLE);
            gaugeText.setVisibility(View.GONE);
            loadingPanel.setVisibility(View.VISIBLE);
        }

        @Override
        protected LoadGaugeParams doInBackground(Gauge... gauge){



            GaugeData gaugeData = new GaugeData(gauge[0].getGaugeID());
            GaugeReadParseObject gaugeReadParseObject = gaugeData.getData();
            boolean isFavorite = myDBHelper.isFavorite(gauge[0]);

            LoadGaugeParams params = new LoadGaugeParams(gaugeReadParseObject,gauge[0],isFavorite);

            return params;
        }

        @Override protected void onPostExecute(LoadGaugeParams result){



            if(result.isFavorite){
                favoriteButton.setSelected(true);
            }else{
                favoriteButton.setSelected(false);
            }
            TextView gaugeNameText = (TextView)findViewById(R.id.gauge_name);
            TextView waterHeight = (TextView) findViewById(R.id.water_height);
            TextView time = (TextView) findViewById(R.id.reading_time);
            TextView floodWarning = (TextView)findViewById(R.id.flood_warning);
            waterHeight.setTextSize(TypedValue.COMPLEX_UNIT_SP,40);


            if(result.gaugeReadParseObject.getDatumList() != null && result.gaugeReadParseObject.getDatumList().size() > 0) {


                String gaugeName = result.gauge.getGaugeName();
                String water = result.gaugeReadParseObject.getDatumList().get(0).getPrimary() + getResources().getString(R.string.feet_unit);


                Log.d("onPostExecute result", "valid is: " + result.gaugeReadParseObject.getDatumList().get(0).getValid() + " primary is: " + result.gaugeReadParseObject.getDatumList().get(0).getPrimary());
                gaugeNameText.setText(gaugeName);
                waterHeight.setText(water);
                floodWarning.setText(getFloodWarning(result.gaugeReadParseObject.getSigstages(),result.gaugeReadParseObject.getDatumList().get(0).getPrimary()));
                String dateString = result.gaugeReadParseObject.getDatumList().get(0).getValid();
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
                gaugeNameText.setText(result.gauge.getGaugeName());
                waterHeight.setText(getResources().getString(R.string.no_data));
                waterHeight.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
                time.setText("");
                loadingPanel.setVisibility(View.GONE);
                gaugeText.setVisibility(View.VISIBLE);
            }
        }

        private String getFloodWarning(Sigstages sigstages, String waterHeight){

            double minorDouble = 0.0;
            double majorDouble = 0.0;
            double moderateDouble = 0.0;
            double waterDouble = 0.0;

            if(sigstages.getMajor() == null && sigstages.getModerate() == null && sigstages.getFlood() == null){
                return "";
            }else {

                try {
                    waterDouble = Double.parseDouble(waterHeight);

                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }

                if(sigstages.getMajor() != null ){

                    try {
                        majorDouble = Double.parseDouble(sigstages.getMajor());
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }

                    if(waterDouble >= majorDouble){
                        return getResources().getString(R.string.major_flooding);
                    }
                }

                if(sigstages.getModerate() !=null){
                    try{
                        moderateDouble = Double.parseDouble(sigstages.getModerate());
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }
                    if(waterDouble >= moderateDouble){
                        return getResources().getString(R.string.moderate_flooding);
                    }
                }
                if(sigstages.getFlood() !=null){

                    try{
                        minorDouble = Double.parseDouble(sigstages.getFlood());
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }
                    if(waterDouble >= minorDouble){
                        return getResources().getString(R.string.minor_flooding);
                    }
                }


            }
            return "";

        }
    }











    private class MoveToLocation extends AsyncTask<String,Void,Gauge>{

        @Override
        protected Gauge doInBackground(String... params){


            Gauge gauge = myDBHelper.getLocationFromIdentifier(params[0]);
            if(gauge.getGaugeName() != null){

                return gauge;

            }



            return null;
        }

        @Override
        protected void onPostExecute(Gauge gauge){

            if(gauge != null) {



                int a = 0;
                if(a == 0){
                    mMap.clear();
                    markerList.clear();
                    selectedMarker = null;
                    myDBHelper.clearMarkers();
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

                }




                LatLng latLng = new LatLng(gauge.getGaugeLatitude(), gauge.getGaugeLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));




                LoadGauge task = new LoadGauge();
                task.execute(gauge);
            }

        }
    }

    private class LoadGaugeParams{

        GaugeReadParseObject gaugeReadParseObject;
        Gauge gauge;
        boolean isFavorite;

        private LoadGaugeParams(GaugeReadParseObject gaugeReadParseObject,Gauge gauge, boolean isFavorite){

            this.gaugeReadParseObject = gaugeReadParseObject;
            this.gauge = gauge;
            this.isFavorite = isFavorite;
        }
    }

    private class AddFavorite extends AsyncTask<Gauge,Void,Gauge>{

        @Override
        protected Gauge doInBackground(Gauge... params){

            myDBHelper.addFavorite(params[0]);
            Log.d("numFavorites",String.valueOf(myDBHelper.getFavoritesCount()));
            return params[0];
        }

        @Override
        protected void onPostExecute(Gauge result){
            String toastText =result.getGaugeName() + getResources().getString(R.string.add_favorite);
            Toast toast = Toast.makeText(mContext,toastText,Toast.LENGTH_SHORT);
            toast.show();
        }
    }


    private class RemoveFavorite extends AsyncTask<Gauge,Void,Gauge>{

        @Override
        protected Gauge doInBackground(Gauge... params){

            myDBHelper.removeFavorite(params[0]);
            Log.d("numFavorites",String.valueOf(myDBHelper.getFavoritesCount()));
            return params[0];
        }

        @Override
        protected void onPostExecute(Gauge result){
            String toastText = result.getGaugeName() + getResources().getString(R.string.remove_favorite);
            Toast toast = Toast.makeText(mContext,toastText,Toast.LENGTH_SHORT);
            toast.show();

        }
    }

    private class ActivateFavoriteButton extends AsyncTask<Gauge, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Gauge... params){
            if(myDBHelper.isFavorite(params[0])){
                return true;
            }else {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result){

            if(result){
                favoriteButton.setSelected(true);
            }else{
                favoriteButton.setSelected(false);
            }

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
            return homeLocation;
        }

        @Override
        protected void onPostExecute(Location result){


            Location location = new Location("");
            Bundle bundle = new Bundle();
            if(result == null){
                location.setLatitude(38.904722);
                location.setLongitude(-77.016389);
            }else{
                location = result;
            }


            if(homeLocation == null){
                distanceAway = 20;
            }else{
                distanceAway = 5;
            }


            previousLocation = homeLocation;
            GetGauges task = new GetGauges();
            task.execute();


            //switch to launching to mapFragment, commenting below out
            /*bundle.putParcelable("MY_LOCATION",location);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            FragmentHome fragmentHome = new FragmentHome();
            fragmentHome.setArguments(bundle);
            fragmentTransaction.add(R.id.home_fragment_container, fragmentHome, "home_fragment");
            fragmentTransaction.commit();*/



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
            Log.d("downloadXML2","Finish, gauge list size: " + gaugeList.size());
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

    private class LoadGaugeFragment extends AsyncTask<Gauge,Void,GaugeFragParams>{

        @Override
        protected GaugeFragParams doInBackground(Gauge... params){

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
            Gauge gauge = params[0];
            boolean isFavorite = myDBHelper.isFavorite(gauge);
            Log.d("fragFave",String.valueOf(isFavorite));
            int notificationState = 0;
            if(isFavorite) {
                notificationState = myDBHelper.getFavoriteNotificationState(gauge);
            }
            boolean isNotifiable = true;
            if(notificationState == 0){
                isNotifiable = false;
            }

            GaugeFragParams gaugeFragParams = new GaugeFragParams(gauge,isFavorite,isNotifiable);
            return gaugeFragParams;
        }

        @Override
        protected void onPostExecute(GaugeFragParams params){

            Bundle bundle = new Bundle();
            bundle.putSerializable("gauge",params.getGauge());
            bundle.putBoolean("isFavorite",params.isFavorite());
            bundle.putBoolean("isNotifiable",params.isNotifiable());
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            FragmentGauge fragmentGauge = new FragmentGauge();
            fragmentGauge.setArguments(bundle);
            fragmentTransaction.replace(R.id.main_layout,fragmentGauge,"gauge_fragment").addToBackStack("Tag");
            fragmentTransaction.commit();

        }
    }



}
