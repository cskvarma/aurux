package aurux;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MainClass {
	private static HashMap<Integer, TreeMap<Double, Integer>> H ; // the hashmap-treemap data-structure
	
	private static boolean addToH(Integer hashbkt, Double timeInUnix, Integer channelId){
		TreeMap<Double, Integer> T = H.get(hashbkt);
		if(T==null){
			T = new TreeMap<Double, Integer>();
			H.put(hashbkt, T);
		}
		T.put(timeInUnix, channelId);
		return true;
	}

	//thid function extracts the Date from the filename
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
		double timeQuantInFP = 0.032; //time is quantized in 0.032 sec bins in the FP computation.
		String queryFile = "q_2013-12-23-21:50:00.mat";
		Date queryDateTime = getDateFromFileName(queryFile,0,1);
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
		FileInputStream fis = null;
        BufferedReader reader = null;

	    
	    
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
		  H = new HashMap<Integer, TreeMap<Double, Integer>>();
		  for (int i=0;i<1/*validFiles.size()*/;i++){
			  //read the file line by line
			  try {
		            fis = new FileInputStream(path+validFiles.get(i));
		            reader = new BufferedReader(new InputStreamReader(fis));
		          
		            System.out.println("Reading File line by line using BufferedReader:"+validFiles.get(i));
		          
		            String line = reader.readLine();
		            while(line != null){
		                System.out.println(line);
		                line = reader.readLine();
		                if(line !=null){
		                		
		                }//end if
		            }           
		          
		        } //end try
			  catch (Exception ex) {
				  System.out.println("Exception in catch: "+ex);
		        }//end catch 
			  finally {
		            try {
		                reader.close();
		                fis.close();
		            } 
		            catch (Exception ex) {
		            	System.out.println("Exception in Finally: "+ex);
		            }

			  }//end finally
			  
			  
			  //addToH(Integer hashbkt, Double timeInUnix, Integer channelId)
			  
			  
		  }
		  
	
	}//end void main function

}//end class
