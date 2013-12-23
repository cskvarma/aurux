package aurux;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainClass {

	//thid function extarcts the Date from the filename
	private static Date getDateFromFileName(String f, int split1, int split2){
		Date d = null;
		//obtain the datetime object from queryFileName
		String datetimeStr = f.split("\\.")[split1].split("_")[split2];
		//System.out.println(datetimeStr);
				
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
		try
		{
			d = simpleDateFormat.parse(datetimeStr);
			//System.out.println("date : "+simpleDateFormat.format(d));
		}
		catch (ParseException ex)
		{
			System.out.println("Exception "+ex);
		}
		return d;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int intervalThr = 60; // +/- time diff
		int snippetLength = 30;
		String queryFile = "q_2013-12-23-21:50:00.mat";
		Date queryDateTime = getDateFromFileName(queryFile,0,1);
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
		
	    
	    
		//list the fingerprint files and find the ones that fall with in the required intervalThr
		  String path = "../fingerprints/"; 
		  String fileName;
		  File folder = new File(path);
		  File[] listOfFiles = folder.listFiles(); 
		  ArrayList<String> validFiles = new ArrayList<String>();
 				  
		  for (int i = 0; i < listOfFiles.length; i++) 
		  {	  fileName = listOfFiles[i].getName();
			  //System.out.println(fileName);
			  Date fileDateTime = getDateFromFileName(fileName,0,2);
			  //System.out.println("file-date : "+simpleDateFormat.format(fileDateTime));
			  
			  double diffTime = (fileDateTime.getTime()-queryDateTime.getTime())/1000;
			  if ((Math.abs(diffTime) < intervalThr) || (Math.abs(diffTime) < intervalThr-snippetLength)){
				  validFiles.add(fileName);
			  }//end if
		  }//end for loop
		  
		 //Load the validFiles into a hash-Treehash
		  System.out.println((validFiles.size()));
		  
		  
	
	}//end void main function

}//end class
