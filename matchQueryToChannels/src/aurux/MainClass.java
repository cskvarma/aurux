package aurux;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
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
	
	private static ArrayList<Integer> smoothWindowByMode(ArrayList<Integer> matchedChannels, int window) {
		ArrayList<Integer> smoothedMatchedChannels= new ArrayList<Integer>();
		for (int i= 0;i<matchedChannels.size();i++){
			  int endIdx = Math.min(i+window,matchedChannels.size()-1);
			  List<Integer> subList = matchedChannels.subList(i,endIdx);
			  
			  int maxFreqElement = -1;
			  int maxFreq=0;
			  for(int j=0;j<subList.size();j++){
				  if (Collections.frequency(subList, subList.get(j)) > maxFreq){
					  maxFreq = Collections.frequency(subList, subList.get(j));
					  maxFreqElement = subList.get(j);
				  }
			  }//end for j
			  smoothedMatchedChannels.add(maxFreqElement);
			  //System.out.println(matchedTimeStamps.get(i)+":"+matchedChannels.get(i));
			  
		  }//end for i
		return smoothedMatchedChannels;
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
	
	
	//this function extracts the length from the filename
		private static int getLengthFromFileName(String f, int split1){
			
			//obtain the datetime object from queryFileName
			String tmp = f.split("\\.")[split1];
			//System.out.println(datetimeStr);
			int d = Integer.parseInt(tmp);
			
			return d;
			
		}
		
	public static Long getMode(Long[] numberList) {
		    HashMap<Long,Long> freqs = new HashMap<Long,Long>();
		    for (Long d: numberList) {
		    		Long freq = freqs.get(d);
		        freqs.put(d, (freq == null ? 1 : freq + 1));   
		    }
		    long mode = 0;
		    long maxFreq = 0;    
		    for (Map.Entry<Long,Long> entry : freqs.entrySet()) {     
		    	Long freq = entry.getValue();
		        if (freq > maxFreq) {
		            maxFreq = freq;
		            mode = entry.getKey();
		        }
		    }    
		    return mode;
		}
	
	private static void consistentMatches(ArrayList<Long> queryTimeStamps,ArrayList<Long> matchedTimeStamps,ArrayList<Integer>matchedChannels){
		Set<Integer> uniquevalues = new HashSet<Integer>(matchedChannels);
		
		ArrayList<Long> qTS=null;
		ArrayList<Long> mTS=null;
		long tmp;
		System.out.println("\n consistentMatches\n***********\n");
		for(int chID : uniquevalues){
			System.out.println("chID:"+chID);
			//get all the queryTimestamps and matchedTimeStamps with this chID
			qTS= new ArrayList<Long>();
			mTS = new ArrayList<Long>();
			
			for(int i=0;i<matchedChannels.size();i++){
				if(matchedChannels.get(i)==chID){
					qTS.add(queryTimeStamps.get(i));
					mTS.add(matchedTimeStamps.get(i));
				}//end if
			}//end for i
			
			//get diff of qTS and mTS
			Long [] diff = new Long[qTS.size()];
			for(int i=0;i<diff.length;i++)
				diff[i] = qTS.get(i)-mTS.get(i);
			
			//get mode
			Long mode = getMode(diff);
			System.out.println("mode:"+mode);
			
			//remove all elements with (qTS-mTS)>MODE +/- 1000 (i.e., 1 sec)
			int noMatches=0;
			for(int i=0;i<matchedChannels.size();i++){
				if(matchedChannels.get(i)==chID){
					tmp = queryTimeStamps.get(i)-matchedTimeStamps.get(i);
					if((tmp >= mode-2000) && (tmp<=mode+2000) ){
						//System.out.println("matched:"+queryTimeStamps.get(i)+"|"+matchedTimeStamps.get(i));
						noMatches++;
					}
					else{
						matchedChannels.remove(i);
						queryTimeStamps.remove(i);
						matchedTimeStamps.remove(i);
					}
				}//end if
			}//end for i
			System.out.println("Summary:"+diff.length+"\t"+noMatches);
			
			//if too few matches remove all the matches for a chID
			if(noMatches < 0.1*diff.length ){
				
				for(int i=0;i<matchedChannels.size();i++){
					if(matchedChannels.get(i)==chID){
							System.out.println("Eliminated "+chID+","+i);
							matchedChannels.remove(i);
							queryTimeStamps.remove(i);
							matchedTimeStamps.remove(i);
						
					}//end if
				}//end for i
			}//end if 0.1
		}//end for chID
	}
		  
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		
		// TODO Auto-generated method stub
		int intervalThr = 60; // +/- time diff for files to be considered
		int snippetLength = 30;// length of each snippet in sec
		double timeQuantInFP = 0.032; //time is quantized in 0.032 sec bins in the FP computation.
		int fingerPrintThrMilliSeconds = 30*1000;//time thr to conisder for matching a finger-print
		String queryFile = args[0];//"qMobile_2014-01-22-21:15:58.mat";//"qMobile_2013-12-23-21:50:00.mat1";//"q_2013-12-23-21:50:00.mat";
		String outputFile =args[1];
		String outputFolder="../phpCode/results/";
		String queryPath = "../buildQueryForExpt/";
		Date queryDateTime = getDateFromFileName(queryFile,0,1);
		int queryLength = getLengthFromFileName(queryFile,2);
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
		FileInputStream fis = null;
        BufferedReader reader = null;
        System.out.println("queryLen:"+queryLength);
	    
	    
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
			  
			  double diffTime = (fileDateTime.getTime()-queryDateTime.getTime())/1000;
			  
			  System.out.println(fileDateTime.getTime()+"|"+queryDateTime.getTime() +"|"+diffTime);
			  //if ((Math.abs(diffTime) < intervalThr) || (Math.abs(diffTime) < intervalThr-snippetLength)){
				if( (diffTime >= -1*intervalThr) && (diffTime < (queryLength+intervalThr)) ){ 
					validFiles.add(fileName);
					System.out.println("Added:"+fileName);
			  }//end if
		  }//end for loop
		  
		 //Load the validFiles into a hash-Treehash
		  H = new HashMap<Integer, TreeMap<Long, Integer>>();
		  int noElemAdded =0;
		  for (int i=0;i<validFiles.size();i++){
			  //read the file line by line
			  try {
		            fis = new FileInputStream(path+validFiles.get(i));
		            reader = new BufferedReader(new InputStreamReader(fis));
		            Date fileDateTime = getDateFromFileName(validFiles.get(i),0,2);
		            
		            System.out.println(validFiles.get(i));
		            //System.out.println(fileDateTime.getTime());
		          
		            String line = reader.readLine();
		            while(line != null){
		               String[] lineSplit = line.split("\\t");
		               //compute the timeStamp as sum of startime+32 milli-sec*lineSplit[1]
		               long timeStampInUnixMilliSec =  fileDateTime.getTime() + ( 32L * Long.parseLong(lineSplit[1]) );
		               	//System.out.println(line);
		                //System.out.println((timeStampInUnixMilliSec));
		                addToH(new Integer(lineSplit[2]), new Long(timeStampInUnixMilliSec), new Integer(lineSplit[0]));
		                noElemAdded ++;
		              
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
		  System.out.println("# Elements Added:"+noElemAdded);
		  System.out.println("Size of HashMap:"+H.size());
		  /*//Debug
		  System.out.println("Treemap Size for an element (1002233):" + (H.get(1002233)).size());
		  TreeMap tm = H.get(1002233);
		  // Get a set of the entries
	      Set set = tm.entrySet();
	      // Get an iterator
	      Iterator i = set.iterator();
	      // Display elements
	      while(i.hasNext()) {
	         Map.Entry me = (Map.Entry)i.next();
	         System.out.print(me.getKey() + ": ");
	         System.out.println(me.getValue());
	      }
		  */
		  long endTime   = System.currentTimeMillis();
		  long totalTime = endTime - startTime;
		  System.out.println("Time:"+totalTime);
		  
		  //load the query-File and search for matches
		  int cntNoMatches = 0;
		  ArrayList<Integer> matchedChannels= new ArrayList<Integer>();
		  ArrayList<Integer> matchedQueryIndex= new ArrayList<Integer>();
		  ArrayList<Long> queryTimeStamps= new ArrayList<Long>();
          ArrayList<Long> matchedTimeStamps= new ArrayList<Long>();
          ArrayList<Integer> smoothedMatchedChannels= new ArrayList<Integer>();
		  try {
	            fis = new FileInputStream(queryPath+queryFile);
	            reader = new BufferedReader(new InputStreamReader(fis));
	            Date fileDateTime = getDateFromFileName(queryFile,0,1);
	            
	            System.out.println("Query File :" + queryFile);
	            System.out.println(fileDateTime.getTime());
	            String line = reader.readLine();
	            
	            
	            
	            
	          while(line != null){
	            	String[] lineSplit = line.split("\\t");
	            	//compute the timeStamp as sum of startime + 32 milli-sec*lineSplit[1]

	            	long timeStampInUnixMilliSec =  fileDateTime.getTime() + ( 32L * Long.parseLong(lineSplit[1]) );
	            	//extract the subTree within fingerPrintThr window
	            	int hashBkt = new Integer(lineSplit[2]);
	            	SortedMap<Long, Integer> T = extarctRelevantSubTreeMap(new Integer(lineSplit[2]), new Long(timeStampInUnixMilliSec), new Integer(fingerPrintThrMilliSeconds));
	            	if((T != null) && (T.size() > 0) ){
	            		//System.out.println((T.size()));
	            		//add each Element of T into matchedChannels(value) and matchedTimeStamps(key);

	            		for (Map.Entry<Long, Integer> entry : T.entrySet()) {
	            			matchedChannels.add(new Integer(entry.getValue()));
	            			matchedTimeStamps.add(new Long(entry.getKey()));
	            			queryTimeStamps.add(new Long(timeStampInUnixMilliSec));
	            			matchedQueryIndex.add(Integer.parseInt(lineSplit[1]));
	            		}//end for iterator
	            		cntNoMatches ++;
	            		//System.out.println("matched:"+lineSplit[1]);
	               }
	            	else{
	            		//System.out.println("not-matched:"+lineSplit[1]);
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
		/*
		  for( int i=0;i<matchedChannels.size();i++){
			  
			  System.out.println(queryTimeStamps.get(i)+"|"+matchedTimeStamps.get(i)+"|"+matchedQueryIndex.get(i)+"|"+matchedChannels.get(i));  
		  }
		  */
		  
		  //Perform cosnistent matching and remove inconsistent matches
		  consistentMatches(queryTimeStamps,matchedTimeStamps,matchedChannels);
		  
		  /*System.out.println("\n After Consistent Matching\n");
		  
		  for( int i=0;i<matchedChannels.size();i++){
			 System.out.println(queryTimeStamps.get(i)+"|"+matchedTimeStamps.get(i)+"|"+matchedQueryIndex.get(i)+"|"+matchedChannels.get(i));  
		  }
		  */
		  
		  //process the matches + AlgoSplitting
		  //the timeStamps are sorted as the query-mat file has timestamps in sorted order 
		  
		  //algo: from the next modeWindow channelIds, pick the one with max-freq.
		  int modeWindow = 10;
		  smoothedMatchedChannels = matchedChannels;//smoothWindowByMode(matchedChannels, modeWindow);
		  
		  
		  Writer writer = null;
		  try {
     		   writer = new BufferedWriter(new OutputStreamWriter(
     		          new FileOutputStream(outputFolder+outputFile), "utf-8"));
     		
     		   for( int i=0;i<matchedChannels.size();i++){
     			   Date q = new Date(queryTimeStamps.get(i));
     			   Date c = new Date (matchedTimeStamps.get(i));
     			   //System.out.println(queryTimeStamps.get(i)+"|"+matchedTimeStamps.get(i)+"|"+matchedQueryIndex.get(i)+"|"+matchedChannels.get(i)+"|"+smoothedMatchedChannels.get(i));
     			  writer.write(q+"\t"+c+"\t"+queryTimeStamps.get(i)+"\t"+matchedChannels.get(i)+"\n");  
				 
     		   }
		  }
		  catch (IOException ex) {
      		  // report
      		} finally {
      		   try {writer.close();} catch (Exception ex) {}
      		}
		  
		 /* 
		//Write output to file
      	Writer writer = null;
      	Date prevDate=null;
      	try {
      		   writer = new BufferedWriter(new OutputStreamWriter(
      		          new FileOutputStream(outputFolder+outputFile), "utf-8"));
      		   for(int i=0;i<smoothedMatchedChannels.size()-2;i++){
      			  if(i==0){
      				  Date q = new Date(queryTimeStamps.get(i));
      				  writer.write(q+"\t");
      				  prevDate=new Date(queryTimeStamps.get(i));
      			  	}
      			  if( (smoothedMatchedChannels.get(i+1) != smoothedMatchedChannels.get(i)) || (i==smoothedMatchedChannels.size()-3)){
      				  Date q = new Date(queryTimeStamps.get(i));
      				  Date q1 = new Date(queryTimeStamps.get(i+1));
      				  double diff = (q1.getTime()-prevDate.getTime())/1000.0;
      				  
      				  writer.write(q+"\t"+diff+"\t"+smoothedMatchedChannels.get(i)+"\n");
   				  
      				  
      				  writer.write(q+"\t");
      				  prevDate=q1;
      			  }
      		   }
      		 }
      		 catch (IOException ex) {
      		  // report
      		} finally {
      		   try {writer.close();} catch (Exception ex) {}
      		}
      	
      	
		  System.out.println("\n\nOutput:");
		  System.out.println("**************");
		  
		  for(int i=0;i<smoothedMatchedChannels.size()-2;i++){
			  if(i==0){
				  Date q = new Date(queryTimeStamps.get(i));
				  System.out.print(q+"\t");
				  prevDate=new Date(queryTimeStamps.get(i));
			  }
			  if( (smoothedMatchedChannels.get(i+1) != smoothedMatchedChannels.get(i)) || (i==smoothedMatchedChannels.size()-3)){
				  Date q = new Date(queryTimeStamps.get(i));
				  Date q1 = new Date(queryTimeStamps.get(i+1));
				  double diff = (q1.getTime()-prevDate.getTime())/1000;
				  if(diff>1.0){
				  	System.out.print(q+"\t"+diff+"\t"+smoothedMatchedChannels.get(i)+"\n");
				  	System.out.print(q1+"\t");
				  }
				  prevDate=q1;
			  }
		  }
		  */
		  System.out.println("\n\n\n------DONE------");
		  
		  endTime   = System.currentTimeMillis();
		  totalTime = endTime - startTime;
		  System.out.println("Time:"+totalTime);
		  
		  
	}//end void main function

}//end class
