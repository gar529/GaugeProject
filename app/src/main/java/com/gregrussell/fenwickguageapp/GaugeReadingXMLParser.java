package com.gregrussell.fenwickguageapp;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GaugeReadingXMLParser {


    private static final String ns = null;
    List<Datum> datums = new ArrayList<Datum>();
    public List<Datum> parse(InputStream in) throws XmlPullParserException, IOException {

        List<Datum> list = new ArrayList<Datum>();
       try{
                XmlPullParser parser = Xml.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(in, null);
                parser.nextTag();
                Log.d("parseData","we are here");
                list = readSite(parser);
                if(list.size() > 0) {
                    Log.d("parseData", " hello " + list.get(0).getPrimary());
                }
                return list;
            } finally {
                in.close();
            }
    }

    private List<Datum> readSite(XmlPullParser parser) throws XmlPullParserException, IOException{



         datums.clear();
             Log.d("readSite", "readingSite");


           int eventType = parser.getEventType();
           Datum dat = new Datum();

             while(eventType != XmlPullParser.END_DOCUMENT){
             if(eventType == XmlPullParser.START_TAG){
                     String name = parser.getName();
                     Log.d("xmldata", "name is: " + name);
                     if (name == null){
                             continue;
                         }
                     if (name.equals("datum")){
                             Log.d("xmldata", "found a datum");
                             dat = readDatum(parser);
                             if(dat.getPrimary() != null && dat.getValid() !=null) {
                                 datums.add(dat);
                             }
                             Log.d("addDatum","datum being added to list " + dat.getValid() + " " + dat.getPrimary());
                             //Log.d("addDatum", "datum was added " + datums.get(datums.size()-1).getPrimary());
                         }
                         if(datums.size() > 0) {
                             //Log.d("addDatum", "first dataum added aaa " + datums.get(0).getPrimary());
                         }
                 }
             eventType = parser.next();
         }
        if(datums.size() > 0) {
           // Log.d("addDatum", "first dataum added " + datums.get(0).getPrimary());
        }
             return  datums;
    }

      

             // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
             // to their respective "read" methods for processing. Otherwise, skips the tag.

             private Datum readDatum(XmlPullParser parser) throws XmlPullParserException, IOException{
                 Log.d("xmlData", "readDatum");
                 parser.require(XmlPullParser.START_TAG, ns, "datum");
                 String valid = null;
                 String primary = null;
                 Datum datum = new Datum();
                 int i = 0;
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
                             Log.d("xmlParser","skip");
                                 skip(parser);
                             }

                             Log.d("xmlData", "ind the readDatum loop, valid is: " + valid + " counter " + i);
                         i++;
                     }
                     datum.setPrimary(primary);
                        datum.setValid(valid);
                 return datum;
             }
     // Processes valid tags in the site.
             private String readValid(XmlPullParser parser) throws IOException, XmlPullParserException{
                 Log.d("xmlData", "readingValid");
                 parser.require(XmlPullParser.START_TAG, ns, "valid");
                 String valid = readText(parser);
                 Log.d("xmlData", "Valid was read as " + valid);
                 parser.require(XmlPullParser.END_TAG, ns, "valid");
                 return valid;
             }

             // Processes primary tags in the site.
             private String readPrimary(XmlPullParser parser) throws IOException, XmlPullParserException{
                 Log.d("xmlData", "readingPrimary");
                 parser.require(XmlPullParser.START_TAG, ns, "primary");
                 String primary = readText(parser);
                 Log.d("xmlData", "primary was read as " + primary);
                 parser.require(XmlPullParser.END_TAG, ns, "primary");
                 return primary;
             }

             //For the tags valid and primary, extracts their text values.
             private String readText(XmlPullParser parser) throws IOException, XmlPullParserException{
                 Log.d("xmlData", "readingText");
                 String result = "";
                 if(parser.next() == XmlPullParser.TEXT){
                            Log.d("xmlData", "pull parser read" + parser.getText());
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
