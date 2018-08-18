package com.gregrussell.fenwickguageapp;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;

public class SearchContentProvider extends ContentProvider {


    public static final String AUTHORITY = "com.gregrussell.fenwickgaugeapp.SearchContentProvider";
    public static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);
    public static final Uri CONTENT_URI = Uri.parse("com.gregrussell.fenwickgaugeapp.SearchContentProvider/suggestions");




    @Override
    public boolean onCreate() {

        Log.d("searchView6","content provider on create");

        //Log.d("searchview8",CONTENT_URI.getAuthority());


        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {

        Log.d("searchProvider1", "query");
        Log.d("searchProvider4", uri.toString());
        Log.d("searchProvider5", String.valueOf(selection));
       Cursor cursor;
       if(uri.getLastPathSegment() != null){

           String query = uri.getLastPathSegment().toLowerCase();
           String args[] = {"%"+query+"%","%"+query+"%"};
           Log.d("searchProvider6",query);
           SQLiteDatabase db = GaugeApplication.myDBHelper.getReadableDatabase();
           String str = "";
           Log.d("searchProvider3", String.valueOf(selection));
           cursor = db.query(DataBaseHelperGauges.Suggestions.TABLE_NAME,
                   projection,selection,args,null,null,sortOrder,"5");

           if(cursor != null && cursor.moveToFirst()){
               Log.d("searchProvider27", "cursor isn't null");
               cursor.moveToFirst();
               try {
                   str = cursor.getString(1);
                   Log.d("searchProvider15",cursor.toString());
                   Log.d("searchProvider100",String.valueOf(cursor.getCount()));


               } catch (Exception e) {
                   e.printStackTrace();

               }
               Log.d("searchProvider2", str);
           }else{
               Log.d("searchProvider28", "cursor is empty " + String.valueOf(cursor));
           }

           return cursor;
       }else {

           return null;
       }
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }
}
