package com.gregrussell.fenwickguageapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Class for creating custom map markers
 */
public class CustomMapMarkerIcon {

    Context context;
    private float zoomLevel[] = {11,10,8,7,0};

    /**
     * Creates a custom marker or marker option
     * @param context Activity context
     */
    public CustomMapMarkerIcon(Context context){
        this.context = context;
    }


    /**
     * Creates a custom MarkerOption for map Markers
     * @param markerOptions MarkerOption that the custom icon will be added to
     * @param zoom Float that determines the size of the custom icon
     * @return A MarkerOption with custom icon
     */
    public MarkerOptions resizedMarkerOptions(MarkerOptions markerOptions, Float zoom){


        if(zoom == zoomLevel[0]){
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(60, 60)));
        }else if(zoom == zoomLevel [1]){
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(60, 60)));
        }else if(zoom == zoomLevel[2]){
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(40, 40)));
        }else{
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(20, 20)));
        }
        return markerOptions;
    }

    /**
     * This method returns a custom icon for the map markers. Converts a drawable resource into a
     * bitmap of custom width and height
     * @param width Width of the custom marker icon
     * @param height Height of the custom marker icon
     * @return A Bitmap of custom width and height. Converted from a drawable resource
     */
    public Bitmap resizeMapIcons(int width, int height){

        Log.d("bitmap1","start");
        Drawable drawable = context.getResources().getDrawable(R.drawable.marker_circle);
        if(drawable instanceof BitmapDrawable){

            return ((BitmapDrawable)drawable).getBitmap();
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0,0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
        Log.d("bitmap2","finish");
        return resizedBitmap;
    }

    /**
     * This method is used to get a custom icon for the home position marker
     * @return A bitmap created from a drawable resource
     */
    public Bitmap homeMarkerBitmap(){

        Drawable drawable = context.getResources().getDrawable(R.drawable.marker_circle_home);
        if(drawable instanceof  BitmapDrawable){

            return ((BitmapDrawable)drawable).getBitmap();
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0,0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return Bitmap.createScaledBitmap(bitmap, 60, 60, false);
    }


}
