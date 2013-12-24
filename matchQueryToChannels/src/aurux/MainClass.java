package aurux;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.tree.TreeModel;


public class MainClass {
	private static HashMap<Integer, TreeMap<Long, Integer>> H ; // the hashmap-treemap data-structure
	
	private static boolean addToH(Integer hashbkt, Long timeInUnixMilliSec, Integer channelId){
		TreeMap<Long, Integer> T = H.get(hashbkt);
		if(T==null){
			T = new TreeMap<Long, Integer>();
			H.put(hashbkt, T);
		}
		T.put(timeInUnixMilliSec, channelId);
		return true;
	}

	private static SortedMap<Long, Integer> extarctRelevantSubTreeMap(Integer hashbkt, Long timeInUnixMilliSec, Integer fingerPrintThr){
		SortedMap<Long, Integer> treemapincl = new TreeMap<Long, Integer>();
		TreeMap<Long, Integer> T = H.get(hashbkt);
		if(T==null){
			return null;
		}
		Long thr = new Long(fingerPrintThr);
		treemapincl=T.subMap(timeInUnixMilliSec-thr,timeInUnixMilliSec+thr);
		
		return treemapincl;
	}
	
	
	//this function extracts the Date from the filename
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
		int intervalThr = 60; // +/- time diff for files to be considered
		int snippetLength = 30;// length of each snippet in sec
		double timeQuantInFP = 0.032; //time is quantized in 0.032 sec bins in the FP computation.
		int fingerPrintThr = 10;//time thr to conisder for matching a finger-print
		String queryFile = "q_2013-12-23-21:50:00.mat";
		String queryPath = "../buildQueryForExpt/";
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
		  H = new HashMap<Integer, TreeMap<Long, Integer>>();
		  for (int i=0;i<validFiles.size();i++){
			  //read the file line by line
			  try {
		            fis = new FileInputStream(path+validFiles.get(i));
		            reader = new BufferedReader(new InputStreamReader(fis));
		          
		            System.out.println("Added File :"+validFiles.get(i));
		          
		            String line = reader.readLine();
		            while(line != null){
		               String[] lineSplit = line.split("\\t");
		               //compute the timeStamp as sum of startime+32 milli-sec*lineSplit[1]
		               Date fileDateTime = getDateFromFileName(validFiles.get(i),0,2);
		               long timeStampInUnixMilliSec =  fileDateTime.getTime() + ( 32L * Long.parseLong(lineSplit[1]) );
		                //System.out.println((timeStampInUnixMilliSec));
		                addToH(new Integer(lineSplit[2]), new Long(timeStampInUnixMilliSec), new Integer(lineSplit[0]));
		              
		                line = reader.readLine();
		            }//end while           
		          
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
			  
		  }//end for files
		  
		  //load the query-File and search for matches
		  int cntNoMatches = 0;
		  try {
	            fis = new FileInputStream(queryPath+queryFile);
	            reader = new BufferedReader(new InputStreamReader(fis));
	          
	            System.out.println("Query File :" + queryFile);
	          
	            String line = reader.readLine();
	            
	            while(line != null){
	               String[] lineSplit = line.split("\\t");
	               //compute the timeStamp as sum of startime + 32 milli-sec*lineSplit[1]
	               Date fileDateTime = getDateFromFileName(queryFile,0,1);
	               long timeStampInUnixMilliSec =  fileDateTime.getTime() + ( 32L * Long.parseLong(lineSplit[1]) );
	               //extract the subTree within fingerPrintThr window
	               int hashBkt = new Integer(lineSplit[2]);
	               SortedMap<Long, Integer> T = extarctRelevantSubTreeMap(new Integer(lineSplit[2]), new Long(timeStampInUnixMilliSec), new Integer(fingerPrintThr));
	               if((T != null) && (T.size() > 0) ){
	            	   		System.out.println((T.size()));
	            	   		
	            	   		cntNoMatches ++;
	               }
	               
	               line = reader.readLine();
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
		  
		  System.out.println("#macthes:" + cntNoMatches);
	
	}//end void main function

}//end class
