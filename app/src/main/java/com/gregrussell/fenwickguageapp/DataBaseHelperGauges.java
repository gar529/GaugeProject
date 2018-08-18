package com.gregrussell.fenwickguageapp;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.provider.BaseColumns;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.maps.model.Marker;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DataBaseHelperGauges extends SQLiteOpenHelper{

    public static final int DATABASE_VERSION = 4;
    public static final String DATABASE_PATH = "/data/data/com.gregrussell.fenwickguageapp/databases/";
    public static final String DATABASE_NAME = "gauges.db";
    private static Context mContext;
    private SQLiteDatabase myDataBase;


    public static class Gauges implements BaseColumns {
        public static final String TABLE_NAME = "gauges";
        public static final String COLUMN_ID = "gauge_id";
        public static final String COLUMN_NAME = "gauge_name";
        public static final String COLUMN_IDENTIFIER = "gauge_identifier";
        public static final String COLUMN_URL = "gauge_url";
        public static final String COLUMN_LATITUDE = "gauge_latitude";
        public static final String COLUMN_LONGITUDE = "gauge_longitude";
        public static final String COLUMN_ADDRESS = "gauge_address";
        public static final String COLUMN_ACTIVE = "gauge_active";
        public static final String COLUMN_VERSION = "gauge_version";
        public static final String COLUMN_TIMESTAMP = "gauge_timestamp";
    }

    public static class Favorites implements BaseColumns{
        public static final String TABLE_NAME = "favorites";
        public static final String COLUMN_ID = "favorite_id";
        public static final String COLUMN_IDENTIFIER = "gauge_identifier";
        public static final String COLUMN_NOTIFICATION = "favorite_notification";
        public static final String COLUMN_ACTIVE = "favorite_active";
        public static final String COLUMN_TIMESTAMP = "favorite_timestamp";
    }

    public static class Markers implements BaseColumns{
        public static final String TABLE_NAME = "markers";
        public static final String COLUMN_ID = "marker_id";
        public static final String COLUMN_IDENTIFIER = "gauge_identifier";
    }

    public static class Suggestions implements BaseColumns {
        public static final String TABLE_NAME = "suggestions";
        public static final String _ID = "_id";
    }

    private static final String SQL_CREATE_TABLE_GAUGES = "CREATE TABLE " +
            Gauges.TABLE_NAME + " (" +
            Gauges.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            Gauges.COLUMN_IDENTIFIER + " TEXT UNIQUE, " +
            Gauges.COLUMN_NAME + " TEXT, " +
            Gauges.COLUMN_URL + " TEXT, " +
            Gauges.COLUMN_LATITUDE + " NUMERIC, " +
            Gauges.COLUMN_LONGITUDE + " NUMERIC, " +
            Gauges.COLUMN_ADDRESS + " TEXT, " +
            Gauges.COLUMN_ACTIVE + " INTEGER NOT NULL DEFAULT 1, " +
            Gauges.COLUMN_VERSION + " INTEGER NOT NULL DEFAULT 1, " +
            Gauges.COLUMN_TIMESTAMP + " INTEGER)";

    private static final String SQL_CREATE_TABLE_FAVORITES = "CREATE TABLE " +
            Favorites.TABLE_NAME + " (" +
            Favorites.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            Favorites.COLUMN_IDENTIFIER +
            " TEXT UNIQUE, " +
            Favorites.COLUMN_NOTIFICATION + " NOT NULL DEFAULT 1," +
            Favorites.COLUMN_ACTIVE + " NOT NULL DEFAULT 1, " +
            Favorites.COLUMN_TIMESTAMP + " INTEGER, FOREIGN KEY(" +
            Favorites.COLUMN_IDENTIFIER +") REFERENCES " +
            Gauges.TABLE_NAME + "(" + Gauges.COLUMN_IDENTIFIER + "))";

    private static final String SQL_CREATE_TABLE_MARKERS = "CREATE TABLE " + Markers.TABLE_NAME + " (" +
            Markers.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + Markers.COLUMN_IDENTIFIER +
            " TEXT UNIQUE, FOREIGN KEY(" + Markers.COLUMN_IDENTIFIER + ") REFERENCES " +
            Gauges.TABLE_NAME + "(" + Gauges.COLUMN_IDENTIFIER + "))";

    private static final String SQL_CREATE_TABLE_SUGGESTIONS = "CREATE TABLE " + Suggestions.TABLE_NAME +
            " ( " + Suggestions._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            SearchManager.SUGGEST_COLUMN_TEXT_1 + " TEXT, " +
            SearchManager.SUGGEST_COLUMN_TEXT_2 + " TEXT, " +
            SearchManager.SUGGEST_COLUMN_INTENT_ACTION + " TEXT, " +
            SearchManager.SUGGEST_COLUMN_INTENT_DATA + " TEXT, " +
            SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID + " TEXT, " +
            SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA + " TEXT, " +
            SearchManager.SUGGEST_COLUMN_QUERY + " TEXT)";

    private static final String SQL_DELETE_GAUGES ="DROP TABLE IF EXISTS " + Gauges.TABLE_NAME;

    private static final String SQL_DELETE_FAVORITES = "DROP TABLE IF EXISTS " + Favorites.TABLE_NAME;

    private static final String SQL_DELETE_MARKERS = "DROP TABLE IF EXISTS " + Markers.TABLE_NAME;

    private static final String SQL_DELETE_SUGGESTIONS = "DROP TABLE IF EXISTS " + Suggestions.TABLE_NAME;


    public DataBaseHelperGauges(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;

    }

    public void onCreate(SQLiteDatabase db){

    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        Log.d("onUpgrade","on Upgrade");
        db.execSQL(SQL_DELETE_GAUGES);
        db.execSQL(SQL_DELETE_FAVORITES);
        db.execSQL(SQL_DELETE_MARKERS);
        db.execSQL(SQL_DELETE_SUGGESTIONS);


        db.execSQL(SQL_CREATE_TABLE_GAUGES);
        db.execSQL(SQL_CREATE_TABLE_FAVORITES);
        db.execSQL(SQL_CREATE_TABLE_MARKERS);
        db.execSQL(SQL_CREATE_TABLE_SUGGESTIONS);


    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
        onUpgrade(db, oldVersion,newVersion);
    }

    public void createDataBase() throws IOException{
        boolean dbExist = checkDataBase();
        if(dbExist){
            //do nothing, database already exists
            Log.d("databaseHelper1", "database exists");

            SQLiteDatabase db = this.getWritableDatabase();
            if(!checkTableExists(Gauges.TABLE_NAME)){
                db.execSQL(SQL_CREATE_TABLE_GAUGES);
                Log.d("databaseHelper5", String.valueOf(checkTableExists(Gauges.TABLE_NAME)));
            }
            if(!checkTableExists(Favorites.TABLE_NAME)){
                db.execSQL(SQL_CREATE_TABLE_FAVORITES);
                Log.d("databaseHelper6", String.valueOf(checkTableExists(Favorites.TABLE_NAME)));
            }
            if(!checkTableExists(Markers.TABLE_NAME)){
                db.execSQL(SQL_CREATE_TABLE_MARKERS);
                Log.d("databaseHelper7", String.valueOf(checkTableExists(Markers.TABLE_NAME)));
            }
            if(!checkTableExists(Suggestions.TABLE_NAME)){
                db.execSQL(SQL_CREATE_TABLE_SUGGESTIONS);

            }

        }else{
            //create database
            Log.d("databaseHelper2", "database doesn't exist, creating...");
            SQLiteDatabase db = this.getReadableDatabase();
            db.execSQL(SQL_CREATE_TABLE_GAUGES);
            db.execSQL(SQL_CREATE_TABLE_FAVORITES);
            db.execSQL(SQL_CREATE_TABLE_MARKERS);
            db.execSQL(SQL_CREATE_TABLE_SUGGESTIONS);

        }
    }

    private boolean checkDataBase(){
        SQLiteDatabase checkDB = null;
        try{
            checkDB = SQLiteDatabase.openDatabase(DATABASE_PATH + DATABASE_NAME,null,SQLiteDatabase.OPEN_READONLY);
        }catch (SQLiteException e){
            //database doesn't exist
            e.printStackTrace();
            Log.d("databaseHelper3", "check database, database doesn't exist");
        }
        if(checkDB != null){
            checkDB.close();
        }
        return checkDB != null ? true : false;
    }

    private boolean checkTableExists(String table){
        try{
            openDataBase();
        }catch (SQLException sqle){
            throw sqle;
        }

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '" + table + "'",null);

        if(cursor!=null){
            if(cursor.getCount() > 0){
                cursor.close();
                return true;
            }
            cursor.close();
        }
        return false;

    }


    public void openDataBase() throws  SQLiteException{
        String myPath = DATABASE_PATH + DATABASE_NAME;
        Log.d("databasehelper4", myPath);
       myDataBase = SQLiteDatabase.openDatabase(myPath,null,SQLiteDatabase.OPEN_READONLY);
    }

    @Override
    public synchronized void close(){
        if(myDataBase !=null){
            myDataBase.close();
        }
        super.close();
    }

    public int getDataVersion(){

        int dataVersion = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + Gauges.TABLE_NAME;
        Cursor cursor = db.rawQuery(selectQuery,null);
        cursor.moveToFirst();
        try{
            dataVersion = cursor.getInt(GaugeApplication.GAUGES_VERSION_POSITION);
            cursor.close();
        }catch (Exception e){
            e.printStackTrace();
            cursor.close();
        }
        Log.d("getDataVersion",String.valueOf(dataVersion));

        return dataVersion;
    }

    public void addGauges(List<Gauge> gaugeList, int version){

        //delete records from gauges table and suggestions table in order to add up to date records
        clearTables();

        Log.d("addGauges1", "Start");
        SQLiteDatabase db = this.getWritableDatabase();

        try{
            db.beginTransaction();
            String query = "INSERT INTO " + Gauges.TABLE_NAME + " (" + Gauges.COLUMN_IDENTIFIER + ", " +
                    Gauges.COLUMN_NAME + ", " + Gauges.COLUMN_URL + ", " + Gauges.COLUMN_LATITUDE +
                    ", " + Gauges.COLUMN_LONGITUDE + ", " + Gauges.COLUMN_ADDRESS + ", " + Gauges.COLUMN_ACTIVE + ", " +
                    Gauges.COLUMN_VERSION + ", " + Gauges.COLUMN_TIMESTAMP + ") VALUES (?,?,?,?,?,?,?,?,?)";
            SQLiteStatement statement = db.compileStatement(query);

            for(int i =0; i<gaugeList.size();i++){
                //Log.d("addGauges3", "progress " + i + "/" + gaugeList.size());
                statement.clearBindings();
                statement.bindString(1,gaugeList.get(i).getGaugeID());
                statement.bindString(2,gaugeList.get(i).getGaugeName());
                statement.bindString(3,gaugeList.get(i).getGaugeURL());
                statement.bindDouble(4,gaugeList.get(i).getGaugeLatitude());
                statement.bindDouble(5,gaugeList.get(i).getGaugeLongitude());
                statement.bindString(6,gaugeList.get(i).getGaugeAddress());
                statement.bindLong(7,1);
                statement.bindLong(8,version);
                statement.bindLong(9, System.currentTimeMillis());
                statement.executeInsert();
            }
            db.setTransactionSuccessful();
        }catch (SQLiteException e){
            StringWriter error = new StringWriter();
            e.printStackTrace(new PrintWriter(error));
            Log.d("addGauges",error.toString());

        }finally {
            db.endTransaction();
        }
        Log.d("addGauges2","Finish");
        addSuggestions(gaugeList);
    }

    private void clearTables(){

        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + Gauges.TABLE_NAME);
        db.execSQL("DELETE FROM " + Suggestions.TABLE_NAME);
    }

    public void addMarkers(List<Marker> markerList){
        Log.d("addMarkers1", "Start");

        SQLiteDatabase db = this.getWritableDatabase();
        try{
            db.beginTransaction();
            String query = "INSERT INTO " + Markers.TABLE_NAME + " (" + Markers.COLUMN_IDENTIFIER + ") VALUES (?)";
            SQLiteStatement statement = db.compileStatement(query);

            for(int i =0; i<markerList.size();i++){
                //Log.d("addGauges3", "progress " + i + "/" + gaugeList.size());
                statement.clearBindings();
                statement.bindString(1,((Gauge)markerList.get(i).getTag()).getGaugeID());
                statement.executeInsert();
            }
            db.setTransactionSuccessful();
        }catch (Exception e){
            Log.d("addMarkers",e.getMessage());

        }finally {
            db.endTransaction();
        }

        Log.d("addMarkers2","Finish");
    }

    public void addSingleMarker(Marker marker){
        SQLiteDatabase db = this.getWritableDatabase();
        try{
            db.beginTransaction();
            String query = "INSERT INTO " + Markers.TABLE_NAME + " (" + Markers.COLUMN_IDENTIFIER + ") VALUES (?)";
            SQLiteStatement statement = db.compileStatement(query);

            statement.bindString(1,((Gauge)marker.getTag()).getGaugeID());
            statement.executeInsert();
            db.setTransactionSuccessful();
        }catch (Exception e){

            e.printStackTrace();
        }finally {
            db.endTransaction();
        }

    }

    public void clearMarkers(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + Markers.TABLE_NAME);

    }

    public boolean checkMarkerExists(String identifier){

        Log.d("markersAdded7","IN DB, STRING IS " + identifier);

        String idInTable;
        SQLiteDatabase db = this.getReadableDatabase();
        try{
            db.beginTransaction();
            String query = "SELECT " + Markers.COLUMN_IDENTIFIER + " FROM " + Markers.TABLE_NAME +
                    " WHERE " + Markers.COLUMN_IDENTIFIER + " LIKE ?";
            SQLiteStatement statement = db.compileStatement(query);
            statement.bindString(1,identifier);
            idInTable = statement.simpleQueryForString();
        }catch (Exception e){
            StringWriter error = new StringWriter();
            e.printStackTrace(new PrintWriter(error));
            Log.d("checkMarkerExists",error.toString());
            return false;
        }finally {
        db.endTransaction();
        }
        if(idInTable.toUpperCase().equals(identifier.toUpperCase())){
            Log.d("markersAdded9", "IN DB, RETURN TRUE: id in table: " + idInTable.toUpperCase() + ",  id: " + identifier.toUpperCase());
            return true;
        }
        Log.d("markersAdded10", "IN DB, RETURN FALSE: id in table: " + idInTable.toUpperCase() + ",  id: " + identifier.toUpperCase());
        return false;

    }

    public List<Gauge> getAllGauges(){

        List<Gauge> gaugeList = new ArrayList<Gauge>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + Gauges.TABLE_NAME + " WHERE " + Gauges.COLUMN_ACTIVE + " = ?";
        Cursor cursor = db.rawQuery(query,new String[] {"1"});
        if(cursor.moveToFirst()){
            do{
                Gauge gauge = new Gauge(cursor.getString(GaugeApplication.GAUGES_URL_POSITION),
                        cursor.getString(GaugeApplication.GAUGES_NAME_POSITION),
                        cursor.getString(GaugeApplication.GAUGES_IDENTIFIER_POSITION),
                        cursor.getDouble(GaugeApplication.GAUGES_LATITUDE_POSITION),
                        cursor.getDouble(GaugeApplication.GAUGES_LONGITUDE_POSITION),
                        cursor.getString(GaugeApplication.GAUGES_ADDRESS_POSITION));
                gaugeList.add(gauge);
            }while (cursor.moveToNext());
        }
        cursor.close();

        return gaugeList;
    }

    private void addSuggestions(List<Gauge> gaugeList){

        Log.d("addSuggestions","start");
        SQLiteDatabase db = this.getWritableDatabase();

        try{
            db.beginTransaction();
            String query = "INSERT INTO " + Suggestions.TABLE_NAME + " (" +
                    SearchManager.SUGGEST_COLUMN_TEXT_1 + ", " +
                    SearchManager.SUGGEST_COLUMN_TEXT_2 + ", " +
                    SearchManager.SUGGEST_COLUMN_INTENT_ACTION + ", " +
                    SearchManager.SUGGEST_COLUMN_INTENT_DATA + ") VALUES (?,?,?,?)";
            SQLiteStatement statement = db.compileStatement(query);

            for(int i =0; i<gaugeList.size();i++){
                //Log.d("addGauges3", "progress " + i + "/" + gaugeList.size());
                statement.clearBindings();
                statement.bindString(1,gaugeList.get(i).getGaugeName());
                statement.bindString(2,gaugeList.get(i).getGaugeAddress());
                statement.bindString(3,"android.intent.action.VIEW");
                statement.bindString(4,gaugeList.get(i).getGaugeID());
                statement.executeInsert();
            }
            db.setTransactionSuccessful();
        }catch (Exception e){
            StringWriter error = new StringWriter();
            e.printStackTrace(new PrintWriter(error));
            Log.d("addSuggestions",error.toString());
        }finally {
            db.endTransaction();
        }

        Log.d("addSuggestions","finish");


    }

    private String getAddressFromCoordinates(double lat, double lon, int total, int current){

        Log.d("addSuggestionsGeo","start");
        List<Address> addressList = new ArrayList<Address>();
        Geocoder geo = new Geocoder(mContext, Locale.getDefault());
        try {
            addressList = geo.getFromLocation(lat, lon, 1);
        }catch (IOException e){
            e.printStackTrace();
        }
        Address address = addressList.get(0);
        String addressString = address.getLocality() + ", " + address.getAdminArea();
        Log.d("addSuggestionsGeo","finish " + current + "/" + total);
        return addressString;
    }

    public Gauge getLocationFromIdentifier(String identifier){

        String url = null;
        String name = null;
        String id = null ;
        double lat = 0;
        double lon = 0;
        String address = null;
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + Gauges.TABLE_NAME + " WHERE " + Gauges.COLUMN_IDENTIFIER +
                " LIKE ?";
        Cursor cursor = db.rawQuery(query,new String[]{identifier});
        cursor.moveToFirst();
        try{
            url = cursor.getString(GaugeApplication.GAUGES_URL_POSITION);
            name = cursor.getString(GaugeApplication.GAUGES_NAME_POSITION);
            id = cursor.getString(GaugeApplication.GAUGES_IDENTIFIER_POSITION);
            lat = cursor.getDouble(GaugeApplication.GAUGES_LATITUDE_POSITION);
            lon = cursor.getDouble(GaugeApplication.GAUGES_LONGITUDE_POSITION);
            address = cursor.getString(GaugeApplication.GAUGES_ADDRESS_POSITION);
        }catch (SQLiteException e){
            e.printStackTrace();
        }
        Gauge gauge = new Gauge(url,name,id,lat,lon,address);

        return gauge;


    }

    public long addFavorite(Gauge gauge){

        SQLiteDatabase db = this.getWritableDatabase();
        long row = 0;
        try{
            db.beginTransaction();
            String query = "INSERT INTO " + Favorites.TABLE_NAME + " (" + Favorites.COLUMN_IDENTIFIER + ", " + Favorites.COLUMN_TIMESTAMP + ") VALUES (?,?)";
            SQLiteStatement statement = db.compileStatement(query);
            statement.bindString(1,gauge.getGaugeID());
            statement.bindLong(2,System.currentTimeMillis());
            row = statement.executeInsert();
            db.setTransactionSuccessful();
        }catch (SQLiteException e){
            e.printStackTrace();
        }finally {
            db.endTransaction();

        }
        return row;
    }

    public void removeFavorite(Gauge gauge){


        SQLiteDatabase db = this.getWritableDatabase();
        try{
            db.beginTransaction();
            String query = "DELETE FROM " + Favorites.TABLE_NAME + " WHERE " + Favorites.COLUMN_IDENTIFIER + " LIKE ?";
            SQLiteStatement statement = db.compileStatement(query);
            statement.bindString(1,gauge.getGaugeID());
            statement.executeUpdateDelete();
            db.setTransactionSuccessful();
        }catch (SQLiteException e){
            e.printStackTrace();
        }finally {
            db.endTransaction();
        }
    }

    public boolean isFavorite(Gauge gauge){

        Log.d("isFavorite",gauge.getGaugeID());
        String idInTable;
        SQLiteDatabase db = this.getReadableDatabase();
        try{
            db.beginTransaction();
            String query = "SELECT * FROM " + Favorites.TABLE_NAME +
                    " WHERE " + Favorites.COLUMN_IDENTIFIER + " LIKE ?";
            SQLiteStatement statement = db.compileStatement(query);
            statement.bindString(1,gauge.getGaugeID());
            idInTable = statement.simpleQueryForString();
            Log.d("isFavorite2",idInTable);
        }catch (SQLiteException e){
            StringWriter error = new StringWriter();
            e.printStackTrace(new PrintWriter(error));
            Log.d("isFavorite",error.toString());
            return false;
        }finally {
            db.endTransaction();
        }
        if(idInTable != null){
            return true;
        }else {
            return false;
        }


    }

    public int getFavoritesCount(){

        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + Favorites.TABLE_NAME;
        Cursor cursor = db.rawQuery(query,null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    public int getGaugesCount(){

        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + Gauges.TABLE_NAME;
        Cursor cursor = db.rawQuery(query,null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    public List<Gauge> getAllFavorites(){

        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + Gauges.TABLE_NAME + " WHERE " + Gauges.COLUMN_IDENTIFIER + " IN ( SELECT " + Favorites.COLUMN_IDENTIFIER + " FROM " + Favorites.TABLE_NAME + ")";
        Cursor cursor = db.rawQuery(query,null);
        Log.d("getAllFavorites","favorites size: " + cursor.getCount());
        List <Gauge> list = new ArrayList<Gauge>();

        if(cursor.moveToFirst()){
            do{
                Gauge gauge = new Gauge(cursor.getString(GaugeApplication.GAUGES_URL_POSITION),
                        cursor.getString(GaugeApplication.GAUGES_NAME_POSITION),
                        cursor.getString(GaugeApplication.GAUGES_IDENTIFIER_POSITION),
                        cursor.getDouble(GaugeApplication.GAUGES_LATITUDE_POSITION),
                        cursor.getDouble(GaugeApplication.GAUGES_LONGITUDE_POSITION),
                        cursor.getString(GaugeApplication.GAUGES_ADDRESS_POSITION));
                list.add(gauge);
            }while (cursor.moveToNext());
            cursor.close();
        }

        return list;


    }


    public List<Gauge> getNotifiableFavorites(){

        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + Gauges.TABLE_NAME + " WHERE " + Gauges.COLUMN_IDENTIFIER + " IN ( SELECT " + Favorites.COLUMN_IDENTIFIER + " FROM " + Favorites.TABLE_NAME + " WHERE " + Favorites.COLUMN_NOTIFICATION + " = " + 1+")";
        Cursor cursor = db.rawQuery(query,null);
        Log.d("getNotifiableFavorites","favorites size: " + cursor.getCount());
        List <Gauge> list = new ArrayList<Gauge>();

        if(cursor.moveToFirst()){
            do{
                Gauge gauge = new Gauge(cursor.getString(GaugeApplication.GAUGES_URL_POSITION),
                        cursor.getString(GaugeApplication.GAUGES_NAME_POSITION),
                        cursor.getString(GaugeApplication.GAUGES_IDENTIFIER_POSITION),
                        cursor.getDouble(GaugeApplication.GAUGES_LATITUDE_POSITION),
                        cursor.getDouble(GaugeApplication.GAUGES_LONGITUDE_POSITION),
                        cursor.getString(GaugeApplication.GAUGES_ADDRESS_POSITION));
                list.add(gauge);
            }while (cursor.moveToNext());
            cursor.close();
        }

        return list;


    }

    public int getFavoriteNotificationState(Gauge gauge){

        int notification = 1;
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + Favorites.TABLE_NAME + " WHERE " + Favorites.COLUMN_IDENTIFIER + " LIKE ?";

        Cursor cursor = db.rawQuery(query,new String[]{gauge.getGaugeID()});
        cursor.moveToFirst();
        try{
            notification = cursor.getInt(GaugeApplication.FAVORITES_NOTIFICATION_POSITION);
        }catch (SQLiteException e){
            StringWriter error = new StringWriter();
            e.printStackTrace(new PrintWriter(error));
            Log.d("getFaveNotificationStat",error.toString());

        }

        return notification;
    }

    public void changeFavoriteNotificationState(Gauge gauge, boolean checked){

        int notification;
        if(checked){
            notification = 1;
        }else{
            notification = 0;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Favorites.COLUMN_NOTIFICATION,notification);
        String selection = Favorites.COLUMN_IDENTIFIER + " LIKE ?";
        db.update(Favorites.TABLE_NAME,values,selection,new String[]{gauge.getGaugeID()});

    }



}
