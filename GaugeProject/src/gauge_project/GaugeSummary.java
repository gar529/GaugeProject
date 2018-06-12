package gauge_project;

public class GaugeSummary{
	
	String url;
	String name;
	String id;
	double latitude;
	double longitude;
	
	public GaugeSummary(){
		
	}
	
public GaugeSummary(String url, String name, String id){
		
		this.url = url;
		this.name = name;
		this.id = id;

	}
	
	public GaugeSummary(String url, String name, String id, double latitude, double longitude){
		
		this.url = url;
		this.name = name;
		this.id = id;
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public String getUrl(){
		return this.url;
	}
	public void setUrl(String url){
		this.url = url;
	}
	public String getName(){
		return this.name;
	}
	public void setName(String name){
		this.name = name;
	}
	public String getId(){
		return this.id;
	}
	public void setId(String id){
		this.id = id;
	}
	public double getLatitude(){
		return this.latitude;
	}
	public void setLatitude(double latitude){
		this.latitude = latitude;
	}
	public double getLongitude(){
		return this.longitude;
	}
	public void setLongitude(double longitude){
		this.longitude = longitude;
	}
}