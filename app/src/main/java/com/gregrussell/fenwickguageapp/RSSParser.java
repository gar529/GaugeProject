package com.gregrussell.fenwickguageapp;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RSSParser {

    private static final String DESCRIPTION_TAG = "description";


    private static final String ns = null;

    public RSSParsedObj parse(InputStream in) throws XmlPullParserException, IOException {
        Log.d("rssParser","parse");
        try{
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readSite(parser);
        } finally {
            in.close();
        }
    }

    private RSSParsedObj readSite(XmlPullParser parser) throws XmlPullParserException, IOException{
        List gauges = new ArrayList();


        int eventType = parser.getEventType();
        String description = null;
        while(eventType != XmlPullParser.END_DOCUMENT){
            if(eventType == XmlPullParser.START_TAG){
                String name = parser.getName();
                Log.d("rssParser", "name is: " + name);
                if (name == null){
                    continue;
                }
                if (name.equals("description")){
                    Log.d("rssParser", "found a description");
                    description = readDescription(parser);
                }
            }
            eventType = parser.next();
        }

        String action = getAction(description);
        String minor = getMinor(description);
        String moderate = getModerate(description);
        String major= getMajor(description);
        String stage = getStage(description);
        String time = getTime(description);

        RSSParsedObj rssParsed = new RSSParsedObj(action,minor,moderate,major,stage,time);
        return rssParsed;
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.

    private String readRSSData(XmlPullParser parser) throws XmlPullParserException, IOException{
        //Log.d("xmlData", "readGaugeData");
        parser.require(XmlPullParser.START_TAG, ns, "Action");

        String description = null;
        while(parser.next() != XmlPullParser.END_TAG){
            if(parser.getEventType() != XmlPullParser.START_TAG){
                continue;
            }
            String tag = parser.getName();
            if(tag.equals(DESCRIPTION_TAG)){
                description = readDescription(parser);

            }else{
                skip(parser);
            }


        }




        return new String(getAction(description));
    }
    // Processes url tags from site



    // Processes id tags from site.
    private String readDescription(XmlPullParser parser) throws IOException, XmlPullParserException{
        //Log.d("xmlData", "readingPrimary");
        parser.require(XmlPullParser.START_TAG, ns, DESCRIPTION_TAG);
        String description = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, DESCRIPTION_TAG);
        Log.d("rssParser","descriptionRead: " + String.valueOf(description));
        return description;
    }



    //For the tags gaugeName and gaugeID, extracts their text values.
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException{
        //Log.d("xmlData", "readingText");
        String result = "";
        if(parser.next() == XmlPullParser.TEXT){
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    //skip tags we don't want
    private void skip(XmlPullParser parser) throws  XmlPullParserException, IOException{
        //Log.d("xmlData", "skip");
        if(parser.getEventType() != XmlPullParser.START_TAG){
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0){
            switch(parser.next()){
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    private String getAction(String description){


        try {
            int start = description.indexOf("Action");
            int end = description.indexOf("ft", start);
            int colon = description.indexOf(":", start);
            String action = description.substring(colon + 1, end);
            Log.d("rssParserAction", action.trim());
            return action.trim();
        }catch (Exception e){
            return null;
        }


    }
    private String getMinor(String description){

        try {
            int start = description.indexOf("Minor");
            int end = description.indexOf("ft", start);
            int colon = description.indexOf(":", start);
            String minor = description.substring(colon + 1, end);
            Log.d("rssParserMinor", minor.trim());
            return minor.trim();
        }catch (Exception e){
            return null;
        }
    }
    private String getModerate(String description){

        try {
            int start = description.indexOf("Moderate");
            int end = description.indexOf("ft", start);
            int colon = description.indexOf(":", start);
            String moderate = description.substring(colon + 1, end);
            Log.d("rssParserModerate", moderate.trim());
            return moderate.trim();
        }catch (Exception e){
            return null;
        }
    }
    private String getMajor(String description){

        try {
            int start = description.indexOf("Major");
            int end = description.indexOf("ft", start);
            int colon = description.indexOf(":", start);
            String major = description.substring(colon + 1, end);
            Log.d("rssParserMajor", major.trim());
            return major.trim();
        }catch (Exception e){
            return null;
        }
    }
    private String getStage(String description){

        try {
            int start = description.indexOf("Latest Observation:");
            int end = description.indexOf("ft", start);
            int colon = description.indexOf(":", start);
            String stage = description.substring(colon + 1, end);
            Log.d("rssParserStage", stage.trim());
            return stage.trim();
        }catch (Exception e){
            return "";
        }
    }
    private String getTime(String description){

        try {
            int start = description.indexOf("Observation Time:");
            int end = description.indexOf("<", start);
            int colon = description.indexOf(":", start);
            String time = description.substring(colon + 1, end);
            Log.d("rssParserTime", time.trim());
            return time.trim();
        }catch (Exception e){
            return "";
        }
    }




}
