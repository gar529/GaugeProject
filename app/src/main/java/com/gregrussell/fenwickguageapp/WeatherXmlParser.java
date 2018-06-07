package com.gregrussell.fenwickguageapp;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by greg on 6/3/2018.
 */

public class WeatherXmlParser {

    private static final String ns = null;

    public List parse(InputStream in) throws XmlPullParserException, IOException{
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

    private List readSite(XmlPullParser parser) throws XmlPullParserException, IOException{
        List datums = new ArrayList();

        Log.d("xmlData", "readingSite");



        /*parser.require(XmlPullParser.START_TAG, ns, "site");
        while (parser.next() != XmlPullParser.END_TAG){
            if(parser.getEventType() != XmlPullParser.START_TAG){
                continue;
            }
            String name = parser.getName();
            Log.d("xmldata", "name is: " + name);
            //Starts by looking for the entery tag
            if (name.equals("datum")){
                Log.d("xmlData", "found a datum");
                datums.add(readDatum(parser));
            }
            else {
                skip(parser);
            }
        }*/

        //parser.require(XmlPullParser.START_TAG, ns, "site");
        int eventType = parser.getEventType();

        while(eventType != XmlPullParser.END_DOCUMENT){
            if(eventType == XmlPullParser.START_TAG){
                String name = parser.getName();
                Log.d("xmldata", "name is: " + name);
                if (name == null){
                    continue;
                }
                if (name.equals("datum")){
                    Log.d("xmldata", "found a datum");
                    datums.add(readDatum(parser));
                }
            }
            eventType = parser.next();
        }

        return  datums;
    }

    public static class Datum{
        public final String valid;
        public final String primary;

        private Datum(String valid, String primary){
            this.valid = valid;
            this.primary = primary;
        }
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.

    private Datum readDatum(XmlPullParser parser) throws XmlPullParserException, IOException{
        Log.d("xmlData", "readDatum");
        parser.require(XmlPullParser.START_TAG, ns, "datum");
        String valid = null;
        String primary = null;
        while(parser.next() != XmlPullParser.END_TAG){
            if(parser.getEventType() != XmlPullParser.START_TAG){
                continue;
            }
            String name = parser.getName();
            if(name.equals("valid")){
                valid = readValid(parser);
            }else if(name.equals("primary")){
                primary = readPrimary(parser);
            }else{
                skip(parser);
            }
        }
        return new Datum(valid,primary);
    }
    // Processes valid tags in the site.
    private String readValid(XmlPullParser parser) throws IOException, XmlPullParserException{
        Log.d("xmlData", "readingValid");
        parser.require(XmlPullParser.START_TAG, ns, "valid");
        String valid = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "valid");
        return valid;
    }

    // Processes primary tags in the site.
    private String readPrimary(XmlPullParser parser) throws IOException, XmlPullParserException{
        Log.d("xmlData", "readingPrimary");
        parser.require(XmlPullParser.START_TAG, ns, "primary");
        String primary = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "primary");
        return primary;
    }

    //For the tags valid and primary, extracts their text values.
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException{
        Log.d("xmlData", "readingText");
        String result = "";
        if(parser.next() == XmlPullParser.TEXT){
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    //skip tags we don't want
    private void skip(XmlPullParser parser) throws  XmlPullParserException, IOException{
        Log.d("xmlData", "skip");
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



}
