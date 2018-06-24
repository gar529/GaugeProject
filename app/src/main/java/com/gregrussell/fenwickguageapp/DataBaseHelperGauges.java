package com.gregrussell.fenwickguageapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.provider.BaseColumns;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import com.gregrussell.fenwickguageapp.WeatherXmlParser.Gauge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataBaseHelperGauges extends SQLiteOpenHelper{

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_PATH = "/data/data/com.gregrussell.fenwickguageapp/databases/";
    public static final String DATABASE_NAME = "gauges.db";
    private SQLiteDatabase myDataBase;


    public static class Gauges implements BaseColumns {
        public static final String TABLE_NAME = "gauges";
        public static final String COLUMN_ID = "gauge_id";
        public static final String COLUMN_NAME = "gauge_name";
        public static final String COLUMN_IDENTIFIER = "gauge_identifier";
        public static final String COLUMN_URL = "gauge_url";
        public static final String COLUMN_LATITUDE = "gauge_latitude";
        public static final String COLUMN_LONGITUDE = "gauge_longitude";
        public static final String COLUMN_ACTIVE = "gauge_active";
        public static final String COLUMN_VERSION = "gauge_version";
        public static final String COLUMN_TIMESTAMP = "gauge_timestamp";
    }

    public static class Favorites implements BaseColumns{
        public static final String TABLE_NAME = "favorites";
        public static final String COLUMN_ID = "favorite_id";
        public static final String COLUMN_IDENTIFIER = "gauge_identifier";
        public static final String COLUMN_ACTIVE = "favorite_active";
        public static final String COLUMN_TIMESTAMP = "favorite_timestamp";
    }

    private static final String SQL_CREATE_TABLE_GAUGES = "CREATE TABLE " + Gauges.TABLE_NAME + " (" +
            Gauges.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + Gauges.COLUMN_IDENTIFIER + " TEXT UNIQUE, " +
            Gauges.COLUMN_NAME + " TEXT, " + Gauges.COLUMN_URL + " TEXT, " + Gauges.COLUMN_LATITUDE + " NUMERIC, " +
            Gauges.COLUMN_LONGITUDE + " NUMERIC, " + Gauges.COLUMN_ACTIVE + " INTEGER NOT NULL DEFAULT 1, " +
            Gauges.COLUMN_VERSION + " INTEGER NOT NULL DEFAULT 1, " + Gauges.COLUMN_TIMESTAMP + " INTEGER)";

    private static final String SQL_CREATE_TABLE_FAVORITES = "CREATE TABLE " + Favorites.TABLE_NAME +
            " (" + Favorites.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + Favorites.COLUMN_IDENTIFIER +
            " TEXT, " + Favorites.COLUMN_ACTIVE + " TEXT, " + Favorites.COLUMN_TIMESTAMP +
            " INTEGER, FOREIGN KEY(" + Favorites.COLUMN_IDENTIFIER +") REFERENCES " +
            Gauges.TABLE_NAME + "(" + Gauges.COLUMN_IDENTIFIER + "))";

    private static final String SQL_DELETE_GAUGES ="DROP TABLE IF EXISTS " + Gauges.TABLE_NAME;

    private static final String SQL_DELETE_FAVORITES = "DROP TABLE IF EXISTS " + Favorites.TABLE_NAME;


    public DataBaseHelperGauges(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);


    }

    public void onCreate(SQLiteDatabase db){

    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        db.execSQL(SQL_DELETE_GAUGES);
        db.execSQL(SQL_DELETE_FAVORITES);
        onCreate(db);
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
        }else{
            //create database
            Log.d("databaseHelper2", "database doesn't exist, creating...");
            SQLiteDatabase db = this.getReadableDatabase();
            db.execSQL(SQL_CREATE_TABLE_GAUGES);
            db.execSQL(SQL_CREATE_TABLE_FAVORITES);
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
            dataVersion = cursor.getInt(Constants.GAUGES_VERSION_POSITION);
            cursor.close();
        }catch (Exception e){
            e.printStackTrace();
            cursor.close();
        }
        Log.d("getDataVersion",String.valueOf(dataVersion));
        db.close();
        return dataVersion;
    }

    public void addGauges(List<Gauge> gaugeList, int version){

        Log.d("addGauges1", "Start");
        SQLiteDatabase db = this.getWritableDatabase();
        /*ContentValues values = new ContentValues();

        for(int i = 0; i < gaugeList.size(); i++){
            Log.d("addGauges3", "progress " + i + "/" + gaugeList.size());
            values.put(Gauges.COLUMN_IDENTIFIER, gaugeList.get(i).getGaugeID());
            values.put(Gauges.COLUMN_NAME, gaugeList.get(i).getGaugeName());
            values.put(Gauges.COLUMN_URL,gaugeList.get(i).getGaugeURL());
            values.put(Gauges.COLUMN_LATITUDE,gaugeList.get(i).getGaugeLatitude());
            values.put(Gauges.COLUMN_LONGITUDE,gaugeList.get(i).getGaugeLongitude());
            values.put(Gauges.COLUMN_ACTIVE,1);
            values.put(Gauges.COLUMN_VERSION,version);
            values.put(Gauges.COLUMN_TIMESTAMP,System.currentTimeMillis());
            db.insert(Gauges.TABLE_NAME,null,values);

        }*/

        try{
            db.beginTransaction();
            String query = "INSERT INTO " + Gauges.TABLE_NAME + " (" + Gauges.COLUMN_IDENTIFIER + ", " +
                    Gauges.COLUMN_NAME + ", " + Gauges.COLUMN_URL + ", " + Gauges.COLUMN_LATITUDE +
                    ", " + Gauges.COLUMN_LONGITUDE + ", " + Gauges.COLUMN_ACTIVE + ", " +
                    Gauges.COLUMN_VERSION + ", " + Gauges.COLUMN_TIMESTAMP + ") VALUES (?,?,?,?,?,?,?,?)";
            SQLiteStatement statement = db.compileStatement(query);

            for(int i =0; i<gaugeList.size();i++){
                //Log.d("addGauges3", "progress " + i + "/" + gaugeList.size());
                statement.clearBindings();
                statement.bindString(1,gaugeList.get(i).getGaugeID());
                statement.bindString(2,gaugeList.get(i).getGaugeName());
                statement.bindString(3,gaugeList.get(i).getGaugeURL());
                statement.bindDouble(4,gaugeList.get(i).getGaugeLatitude());
                statement.bindDouble(5,gaugeList.get(i).getGaugeLongitude());
                statement.bindLong(6,1);
                statement.bindLong(7,version);
                statement.bindLong(8, System.currentTimeMillis());
                statement.executeInsert();


            }
            db.setTransactionSuccessful();
        }catch (Exception e){
            Log.d("addGauges4","didn't work");
            e.printStackTrace();
        }finally {
            db.endTransaction();
        }

        db.close();
        Log.d("addGauges2","Finish");
    }

    public List<Gauge> getAllGauges(){

        List<Gauge> gaugeList = new ArrayList<Gauge>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + Gauges.TABLE_NAME;
        Cursor cursor = db.rawQuery(query,null);
        if(cursor.moveToFirst()){
            do{
                Gauge gauge = new Gauge(cursor.getString(Constants.GAUGES_URL_POSITION),
                        cursor.getString(Constants.GAUGES_NAME_POSITION),
                        cursor.getString(Constants.GAUGES_IDENTIFIER_POSITION),
                        cursor.getDouble(Constants.GAUGES_LATITUDE_POSITION),
                        cursor.getDouble(Constants.GAUGES_LONGITUDE_POSITION));
                gaugeList.add(gauge);
            }while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return gaugeList;
    }



}
