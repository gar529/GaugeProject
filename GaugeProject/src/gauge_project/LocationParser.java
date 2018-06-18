package gauge_project;



import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Scanner;



public class LocationParser {

	List<GaugeSummary> gaugeList;
	
	public LocationParser(){
		
	}
	
	public LocationParser(List<GaugeSummary> gaugeList){
		
		this.gaugeList = gaugeList;
		
	}
	
	
	public void runLocationParser(){
		
		
		String content = null;
		URLConnection connection = null;
		String latitude = null;
		String longitude = null;
		double latCoordinate;
		double lonCoordinate;
		long startTime = 0;
		long delay = 0;
		
		for(int i =0; i< gaugeList.size(); i++){
			
			startTime = System.currentTimeMillis();
			delay = startTime + 50;
			
			try {
				  connection =  new URL(gaugeList.get(i).getUrl()).openConnection();
				  Scanner scanner = new Scanner(connection.getInputStream());
				  scanner.useDelimiter("\\Z");
				  content = scanner.next();
				  
				}catch ( Exception ex ) {
				    ex.printStackTrace();
				}
			if(content != null) {
				//System.out.println(gaugeList.get(i).getName() + " " + gaugeList.get(i).getId());
				latitude = getLatitude(content);
				longitude = getLongitude(content);
				
				if(latitude != null && longitude != null){
					latCoordinate = parseLatitude(latitude);
					lonCoordinate = parseLongitude(longitude);
					
					gaugeList.get(i).setLatitude(latCoordinate);
					gaugeList.get(i).setLongitude(lonCoordinate);
					
						
				}else{
					//location unavailable
					//System.out.println("location not available");
				}
			}else{
				//location unavailable
				//System.out.println("location not available");
			}
			
			double percentCompleted = (i * 1.0)/(gaugeList.size()*1.0)*100.0;
			DecimalFormat format = new DecimalFormat("#.#");
			System.out.println("current item: " + i + "/" + gaugeList.size() + "..." + format.format(percentCompleted) + "% complete");
			
			
			while(startTime < delay){
				startTime = System.currentTimeMillis();
				
			}
			
			
		}
		
	}
	
	private String getLatitude(String html){
		//North = positive
		//West = negative
		
		String latitude = null;
		
		String start = "Latitude:";
		String end = ",";
		
		int startPos = 0;
		int endPos = 0;
		
		startPos = html.indexOf(start);
		endPos = html.indexOf(end,startPos);
		if(startPos > 0 && endPos > startPos){
		
			latitude = html.substring(startPos, endPos);
			//System.out.println("the latitude is: " + latitude);
			
		}else{
			
			//System.out.println("location not available");
		}
		return latitude;
		
	}
	
	private String getLongitude(String html){
		
		//System.out.println("getLongitude called");
		String longitude = null;
		
		String start = "Longitude:";
		String end = ",";
		
		int startPos = 0;
		int endPos = 0;
		
		startPos = html.indexOf(start);
		endPos = html.indexOf(end,startPos);
		if(startPos > 0 && endPos > startPos){
		
			longitude = html.substring(startPos, endPos);
			//System.out.println("the longitude is: " + longitude);
		}else{
			
			//System.out.println("location not available");
		}
		
		return longitude;
	}
	
	private double parseLatitude(String latitude){
		
		String parsedLatitude = null;
		double latCoordinate = 0.00;
		
		int startPos = latitude.indexOf(":")+1;
		int endPos = latitude.indexOf("&");
		String direction = latitude.substring(latitude.length()-1);
		parsedLatitude = latitude.substring(startPos,endPos).trim();
		try{
			latCoordinate = Double.parseDouble(parsedLatitude);
		}catch (NumberFormatException e){
			e.printStackTrace();
		}
		if(direction.toUpperCase().equals("S")){
			latCoordinate = latCoordinate * -1;
		}
		
		//System.out.println("parsed latitude is: " + parsedLatitude + direction);
		//System.out.println("lat coordinate is: " + latCoordinate);
		
		
		
		
		
		return latCoordinate;
	}
	
	private double parseLongitude(String longitude){
		String parsedLongitude = null;
		double lonCoordinate = 0.00;
		
		int startPos = longitude.indexOf(":")+1;
		int endPos = longitude.indexOf("&");
		String direction = longitude.substring(longitude.length()-1);
		parsedLongitude = longitude.substring(startPos,endPos).trim();
		try{
			lonCoordinate = Double.parseDouble(parsedLongitude);
		}catch(NumberFormatException e){
			e.printStackTrace();
		}
		if(direction.toUpperCase().equals("W")){
			lonCoordinate = lonCoordinate * -1;
		}
		//System.out.println("parsed longitude is: " + parsedLongitude + direction);
		//System.out.println("lon coordinate is: " + lonCoordinate);
		return lonCoordinate;
	}
	
	
}
