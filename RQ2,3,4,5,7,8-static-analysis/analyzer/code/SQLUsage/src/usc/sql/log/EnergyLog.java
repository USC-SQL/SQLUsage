package usc.sql.log;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class EnergyLog {
	public static void main(String[] args) throws IOException
	{
		File folder = new File("/home/yingjun/Documents/appset/appDbEnergyNonConcurrTransaction");
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
		  File file = listOfFiles[i];
		  if (file.isFile() && file.getName().endsWith(".txt")) {
		    String content = FileUtils.readFileToString(file);
		    String[] records = content.split("\n");
		    for(String record : records)
		    {
		    	if(record.contains("execSQL"))
		    		System.out.println(record);
		    }
		    
		    /* do somthing with content */
		  } 
		}
	}
}
