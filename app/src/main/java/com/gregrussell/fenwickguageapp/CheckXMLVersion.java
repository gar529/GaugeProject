package com.gregrussell.fenwickguageapp;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class CheckXMLVersion {

    private static final String ns = null;
    private static final String VERSION_TAG = "version";


    public int parse(InputStream in) throws XmlPullParserException, IOException {
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

    private int readSite(XmlPullParser parser) throws XmlPullParserException, IOException{

        int version = 0;

        Log.d("checkXML1", "readingSite");
        //parser.require(XmlPullParser.START_TAG, ns, "site");
        int eventType = parser.getEventType();

        while(eventType != XmlPullParser.END_TAG){
            if(eventType == XmlPullParser.START_TAG){
                String name = parser.getName();
                Log.d("checkXML2", "name is: " + name);
                if (name == null){
                    continue;
                }
                if (name.equals(VERSION_TAG)){
                    Log.d("checkXML3", "found a version");
                    version = (readVersion(parser));
                }
            }
            eventType = parser.next();
        }

        return version;
    }

    //processes lon tags from site
    private int readVersion(XmlPullParser parser) throws IOException, XmlPullParserException{
        parser.require(XmlPullParser.START_TAG,ns,VERSION_TAG);
        String intString = readText(parser);
        parser.require(XmlPullParser.END_TAG,ns,VERSION_TAG);
        int version = 0;
        try{
            version = Integer.parseInt(intString);
        }catch (NumberFormatException e){
            e.printStackTrace();
        }
        return version;
    }


    //For the tags gaugeName and gaugeID, extracts their text values.
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException{
        Log.d("xmlData", "readingText");
        String result = "";
        if(parser.next() == XmlPullParser.TEXT){
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }


}
