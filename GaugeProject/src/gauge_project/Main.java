package gauge_project;



import java.util.ArrayList;
import java.util.List;



public class Main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Parser parser = new Parser();
		parser.runParser();
		
		
		
		List<GaugeSummary> list = new ArrayList<GaugeSummary>();
		GaugeSummary gauge = new GaugeSummary();
		gauge.setUrl("https://water.weather.gov/ahps2/hydrograph.php?gage=hpkv2&wfo=lwx");
		gauge.setName("test name");
		gauge.setId("testID");
		
		
		list.add(gauge);
		
		//LocationParser locParse = new LocationParser(list);
		//locParse.runLocationParser();

	}

}
