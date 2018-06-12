package gauge_project;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Parser {
	
	String content;
	public static final String START_TAG = "td";
	public static final String HEADER = "location_data_id";
	public static final String HOST = "https://water.weather.gov";
	List<GaugeSummary> summaryList = new ArrayList<GaugeSummary>();
	
	public Parser(){
		
	}
	public Parser (String content){
		
		this.content = content;
		
	}
	
	public String getContent(){
		return this.content;
	}
	public void setContent(String content){
		this.content = content;
	}
	
	
	
	
	
	public List<GaugeSummary> getSummaryData(String content){
		
		List<GaugeSummary> summaryDataList = new ArrayList<GaugeSummary>();
		
		return summaryDataList;
		
		
		
	}
	
	public List<String> getRawSummary(){
		
		List<String> test = new ArrayList<String>();
		
		String startTag = "<" + START_TAG;
		String endTag = "</" + START_TAG + ">";
		
		String data = null;
		int i = 0;
		int endPos = 0;
		int startPos = 0;
		System.out.println("content length is: " + content.length());
		while(i < content.length() && i > -1){
			
			startPos = content.indexOf(startTag,i);
			if(startPos > -1){
				endPos = content.indexOf(endTag,startPos)+5;
				if (endPos == -1){
					endPos = content.length()-1;
					System.out.println("stuck 1");
				}else{
					if(content.substring(startPos, endPos).indexOf(HEADER, 0) != -1){
						
						System.out.println("start pos is: " + startPos);
						data = content.substring(startPos, endPos);
						System.out.println("data is: " + data);
						test.add(data);
						System.out.println("endPos is: " + endPos);
						i = endPos + 1;
					}
					else{
						i = endPos +1;
						System.out.println("stuck 2");
					}
					System.out.println("stuck 3");
					
				}
				System.out.println("stuck 4");
			}
			
			i++;
			
			
		}
		
		System.out.println("finished with test");
		return test;
	}
	
	public void parseRawSummary(List<String> dataList){
		
		
		
		
		for(int i = 0; i < dataList.size(); i++){
			
			String data = dataList.get(i);
			String url = null;
			String name = null;
			String id = null;
			int startPoint = 0;
			int endPoint = 0;
			System.out.println("we're in parseRaw, data is: " + data);
			//get url
			if(data.indexOf("href",0) > -1){
				startPoint = data.indexOf("href",0) + 6;
				endPoint = data.indexOf("\"", startPoint);
				url = HOST + data.substring(startPoint, endPoint);
			}else {
				url = "URL could not be found";
			}
				
			//get name
			if(data.indexOf(">",endPoint) > -1 ){
				startPoint = data.indexOf(">",endPoint) + 1;
				endPoint = data.indexOf("(",startPoint);
				name = data.substring(startPoint, endPoint);
			}else{
				name = "name could not be found";
			}
			
			//get id
			if(data.indexOf("(", endPoint) > -1){
				startPoint = endPoint + 1;
				endPoint = data.indexOf(")", startPoint);
				id = data.substring(startPoint,endPoint);
				
			}else{
				id = "ID could not be found";
			}
			
			GaugeSummary summary = new GaugeSummary(url,name,id);
			summary.setLatitude(1.1111);
			summary.setLongitude(1.22222);
			System.out.println("lat is " + summary.getLatitude() + "lon is" + summary.getLongitude());
			summaryList.add(summary);
			
				
		}
		
		return;
		
	}
	
	public void runParser(){
		
		String[] states = {"AK", "AL", "AR", "AZ", "CA", "CO", "CT", "DC",  
			    "DE", "FL", "GA", "HI", "IA", "ID", "IL", "IN", "KS", "KY", "LA",  
			    "MA", "MD", "ME", "MI", "MN", "MO", "MS", "MT", "NC", "ND", "NE",  
			    "NH", "NJ", "NM", "NV", "NY", "OH", "OK", "OR", "PA", "RI", "SC",  
			    "SD", "TN", "TX", "UT", "VA", "VT", "WA", "WI", "WV", "WY"};
			String content = null;
			URLConnection connection = null;
			
			for(int a = 0; a < states.length; a++){
				try {
				  connection =  new URL("https://water.weather.gov/ahps/riversummary.php?state=" + states[a]).openConnection();
				  Scanner scanner = new Scanner(connection.getInputStream());
				  scanner.useDelimiter("\\Z");
				  content = scanner.next();
				}catch ( Exception ex ) {
				    ex.printStackTrace();
				}
				System.out.println(content);
				
				Parser parser = new Parser(content);
				System.out.println("asdfasdf" + parser.getRawSummary().get(0));
				List<String> parseData = parser.getRawSummary();
				System.out.println("lenght of my list of unparsed data is: " + parseData.size());
				for (int i =0; i < parseData.size(); i++){
					System.out.println(parseData.get(i));
				}
				if(parseData.size() > 0){
					parseRawSummary(parseData);
					
					System.out.println("size of the parsed data list is:" + summaryList.size());
					
				}
			
			}
			
			for(int j =0; j< summaryList.size(); j++){
				System.out.println(summaryList.get(j).getUrl());
				System.out.println(summaryList.get(j).getName());
				System.out.println(summaryList.get(j).getId());
			}
			
			System.out.println("size of the parsed data list is:" + summaryList.size());
			
			LocationParser locParser = new LocationParser(summaryList);
			locParser.runLocationParser();
			
			XMLGenerator xmlGen = new XMLGenerator(summaryList);
			xmlGen.generateXML();
		
	}

}
