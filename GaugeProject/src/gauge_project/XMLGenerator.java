package gauge_project;

import gauge_project.Parser.GaugeSummary;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;



public class XMLGenerator {
	
	List<GaugeSummary> list;

	public XMLGenerator(){
		
	}
	
	public XMLGenerator(List<GaugeSummary> list){
		
		this.list = list;
		
	}
	
	//file saved to *current workspace*/GaugeProject/xmlData.xml
	public void generateXML(){
		
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data>";
		
		for(int i=0; i<list.size();i++){
						
			GaugeSummary summary = list.get(i);
			summary.setUrl(addExtension(summary.getUrl()));
			summary.setName(addExtension(summary.getName()));
			summary.setId(addExtension(summary.getId()));
			xml = xml + "<gauge><url>" + list.get(i).getUrl() + "</url>" +
					"<name>" + list.get(i).getName() + "</name>" +
					"<id>" + list.get(i).getId() + "</id></gauge>";
		}
		xml = xml+"</data>";
		
		try{
			File file = new File("xmlData.xml");
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.write(xml);
			fileWriter.flush();
			fileWriter.close();
			System.out.println("file created successfully");
		}catch (IOException e){
			e.printStackTrace();
			
		}
	}
	
	private String addExtension(String myString){
		
		String fixedString = myString;
		int start = 0;
		for(int j = 0; j< fixedString.length(); j++){
			if(fixedString.indexOf("&",j) != -1){
				start = myString.indexOf("&",j)+1;
				String parsedString = fixedString.substring(j, start) + "amp;" +fixedString.substring(start,fixedString.length());
				j = start +1;
				fixedString = parsedString;
			}
		}
		
		return fixedString;
		
	}
	
}