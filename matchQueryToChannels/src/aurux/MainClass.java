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
		System.out.println(f);
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
		    System.out.println("----diff-mode-freq:"+maxFreq);
		    return mode;
		}
	
	private static void consistentMatches(ArrayList<Long> queryTimeStamps,ArrayList<Long> matchedTimeStamps,ArrayList<Integer>matchedChannels){
		Set<Integer> uniquevalues = new HashSet<Integer>(matchedChannels);
		
		ArrayList<Long> qTS=null;
		ArrayList<Long> mTS=null;
		long tmp;
		int noValidMatches=0;
		for(int chID : uniquevalues){
			System.out.println("---chID:"+chID+"---");
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
			for(int i=0;i<diff.length;i++){
				diff[i] = qTS.get(i)-mTS.get(i);
				System.out.println("-----diff\t"+qTS.get(i)+"\t"+diff[i]);
			}
			System.out.println("----#original macthes:"+qTS.size() +"\t"+mTS.size());
			
			//get mode
			//int freq= new Integer(0);
			Long mode = getMode(diff);
			System.out.println("----diff-mode:"+mode);
			int thr=1000;
			System.out.println("----remove all matches in a range of +/-"+thr+" of mode");
			//remove all elements with (qTS-mTS)>MODE +/- 1000 (i.e., 1 sec)
			
			long startTime= queryTimeStamps.get(0);
			ListIterator<Integer> it1 = matchedChannels.listIterator(); 
			ListIterator<Long> it2 = queryTimeStamps.listIterator();
			ListIterator<Long> it3 = matchedTimeStamps.listIterator();
			
			//for(int i=0;i<matchedChannels.size();i++){
			System.out.println("PREV:"+matchedChannels.size());
			while(it1.hasNext()){
				int mc = it1.next();
				long qTS1 = it2.next();
				long mTS1 = it3.next();
				if(mc==chID){
					tmp = qTS1-mTS1;//queryTimeStamps.get(i)-matchedTimeStamps.get(i);
					if((tmp >= mode-thr) && (tmp<=mode+thr) ){
						double timeFromStart = (double)(qTS1-startTime)/1000.0;
						//System.out.println("-----matched:"+tmp+"\t"+timeFromStart);
						noValidMatches++;
					}
					else{
						//System.out.println("1.Cons-Chk:"+matchedChannels.size());
						it1.remove();
						//System.out.println("2.Cons-Chk:"+matchedChannels.size());
						it2.remove();
						it3.remove();
						
					}
				}//end if
			}//end while
			System.out.println("----#macthes after removing spurious ones:"+noValidMatches+"\t"+queryTimeStamps.size());
			/*
			//if too few matches remove all the matches for a chID
			if(noMatches < 0.1*diff.length ){
				System.out.println("---Elimination due to >90% spurious matches "+chID);
				for(int i=0;i<matchedChannels.size();i++){
					if(matchedChannels.get(i)==chID){
							
							matchedChannels.remove(i);
							queryTimeStamps.remove(i);
							matchedTimeStamps.remove(i);
						
					}//end if
				}//end for i
			}//end if 0.1
			*/
		}//end for chID
		System.out.println("----Final#macthes after removing spurious ones:"+noValidMatches+"\t"+queryTimeStamps.size());
	}
		  
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("-MatchQueriestoChannels START-");
		long startTime = System.currentTimeMillis();
		
		// TODO Auto-generated method stub
		int intervalThr = 60; // +/- time diff for files to be considered
		int snippetLength = 30;// length of each snippet in sec
		double timeQuantInFP = 0.032; //time is quantized in 0.032 sec bins in the FP computation.
		int fingerPrintThrMilliSeconds = 10*1000;//time thr to conisder for matching a finger-print
		String queryFile = args[0];//"qMobile_2014-01-22-21:15:58.mat";//"qMobile_2013-12-23-21:50:00.mat1";//"q_2013-12-23-21:50:00.mat";
		String outputFile =args[1];
		String outputFolder="../phpCode/results/";//"../expt/";//"../phpCode/results/";
		String queryPath = args[3];//"../buildQueryForExpt/";//"../expt/";
		Date queryDateTime = getDateFromFileName(queryFile,0,1);
		int queryLength = getLengthFromFileName(queryFile,2);
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
		FileInputStream fis = null;
        BufferedReader reader = null;
        System.out.println("queryLen:"+queryLength);
	    
	    
		//list the fingerprint files and find the ones that fall with in the required intervalThr
		  String path = args[2];//"../fingerprints/";//"../expt/fingerprints/";//; 
		  String fileName;
		  File folder = new File(path);
		  File[] listOfFiles = folder.listFiles(); 
		  ArrayList<String> validFiles = new ArrayList<String>();
		  System.out.println("--List of files shortlisted--");		  
		  for (int i = 0; i < listOfFiles.length; i++) 
		  {	  fileName = listOfFiles[i].getName();
			  //System.out.println(fileName);
			  Date fileDateTime = getDateFromFileName(fileName,0,2);
			  //System.out.println(fileDateTime);
			  double diffTime = (fileDateTime.getTime()-queryDateTime.getTime())/1000;
			  
			  System.out.println(fileDateTime.getTime()+"\t"+queryDateTime.getTime() +"\t"+diffTime);
			  //if ((Math.abs(diffTime) < intervalThr) || (Math.abs(diffTime) < intervalThr-snippetLength)){
				if( (diffTime >= -1*intervalThr) && (diffTime < (queryLength+intervalThr)) ){ 
					validFiles.add(fileName);
					System.out.println("ValidFiles:"+fileName);
			  }//end if
		  }//end for loop
		  
		 //Load the validFiles into a hash-Treehash
		  System.out.println("--Loading FP in above files to hashMap--");
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
		            int noElemAddedPerFile=0;
		            while(line != null){
		               String[] lineSplit = line.split("\\t");
		               //compute the timeStamp as sum of startime+32 milli-sec*lineSplit[1]
		               long timeStampInUnixMilliSec =  fileDateTime.getTime() + ( 32L * Long.parseLong(lineSplit[1]) );
		               	//System.out.println(line);
		                //System.out.println((timeStampInUnixMilliSec));
		                addToH(new Integer(lineSplit[2]), new Long(timeStampInUnixMilliSec), new Integer(lineSplit[0]));
		                noElemAdded ++;
		                noElemAddedPerFile++;
		                line = reader.readLine();
		            }//end while           
		          System.out.println("Elem Count for "+validFiles.get(i)+"\t"+noElemAddedPerFile);
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
		  System.out.println("--Matching Query FP-file to Channels-DS--");
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
	            			//System.out.println("---matched:"+)
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
		  
		  System.out.println("#matches:" + cntNoMatches);
		  long startT = queryTimeStamps.get(0);
		  for( int i=0;i<matchedChannels.size();i++){
			  double diff = (double)(queryTimeStamps.get(i)-startT)/1000;
			  System.out.println(diff+"\t"+matchedChannels.get(i));  
		  }
		  
		  
		  //Perform cosnistent matching and remove inconsistent matches
		  System.out.println("--Consistent Matches--");
		  consistentMatches(queryTimeStamps,matchedTimeStamps,matchedChannels);
		  System.out.println("#matches after consistent Matching:" + queryTimeStamps.size());
		  /*System.out.println("\n After Consistent Matching\n");
		  
		  for( int i=0;i<matchedChannels.size();i++){
			 System.out.println(queryTimeStamps.get(i)+"|"+matchedTimeStamps.get(i)+"|"+matchedQueryIndex.get(i)+"|"+matchedChannels.get(i));  
		  }
		  */
		  
		  //process the matches + AlgoSplitting
		  //the timeStamps are sorted as the query-mat file has timestamps in sorted order 
		  
		  //algo: from the next modeWindow channelIds, pick the one with max-freq.
		  int modeWindow = 10;
		  //smoothedMatchedChannels = matchedChannels;//smoothWindowByMode(matchedChannels, modeWindow);
		  
		  long startQTS = queryTimeStamps.get(0);
		  Writer writer = null;
		  try {
     		   writer = new BufferedWriter(new OutputStreamWriter(
     		          new FileOutputStream(outputFolder+outputFile), "utf-8"));
     		
     		   for( int i=0;i<matchedChannels.size();i++){
     			   Date q = new Date(queryTimeStamps.get(i));
     			   Date c = new Date (matchedTimeStamps.get(i));
     			   //System.out.println(queryTimeStamps.get(i)+"|"+matchedTimeStamps.get(i)+"|"+matchedQueryIndex.get(i)+"|"+matchedChannels.get(i)+"|"+smoothedMatchedChannels.get(i));
     			  //writer.write(q+"\t"+c+"\t"+queryTimeStamps.get(i)+"\t"+matchedChannels.get(i)+"\n");
     			  double diff = (double)(queryTimeStamps.get(i)-startQTS)/1000.0;
     			   writer.write(diff+"\t"+matchedChannels.get(i)+"\n");
				 
     		   }
		  }
		  catch (IOException ex) {
      		  // report
      		} finally {
      		   try {writer.close();} catch (Exception ex) {}
      		}
		  
		  
		  
		  //print values for each 5 sec window
		  //long qEnd = queryTimeStamps.get(queryTimeStamps.size()-1)/1000;
		  int noBuckets  = (int) Math.floor(queryLength/5);
		  System.out.println("#buckets"+noBuckets);
		  long startTS = queryTimeStamps.get(0);
		  for ( int bkt=0;bkt<noBuckets;bkt++){
			  int min = bkt*5;
			  int max = (bkt+1)*5;
			  Map<Integer, Integer> countMap = new HashMap<Integer, Integer>();
			  for(int i=0;i<queryTimeStamps.size();i++){
				  long ts =queryTimeStamps.get(i);
				  int m = (int) ((ts-startTS)/1000);
				  int chID = matchedChannels.get(i);
				  if ( (m > min) && (m <= max)){
					  if (countMap.containsKey(chID)) {
					        int currentCount = countMap.get(chID);
					        countMap.put(chID, currentCount + 1);
					    }
					    else {
					        countMap.put(chID, 1);
					    }
				  }//end if  max min
			  }//end i
			  System.out.print(""+bkt+"\t");
			  for (int i=101;i<108;i++) {
				    if(countMap.get(i)==null )
				    		System.out.print("0\t");
				    else
				    	System.out.print(countMap.get(i)+"\t");
				}
			  System.out.println();
				
		  }//end bkt
		  
		  
		  
		  //END print values for each 5 sec window
		  
		  /*
		  //remove outliers and non-matched regions
		  long startTS = queryTimeStamps.get(0);
		  long prevTS = queryTimeStamps.get(0);
		  for(int i=0;i<matchedChannels.size();i++){
			  long ts =queryTimeStamps.get(i);
			  Date q = new Date(queryTimeStamps.get(i));
			  int ch = matchedChannels.get(i);
			  //get all chIDs in the next 5 seconds
			  List<Integer> arrlist = new ArrayList();
			  Set<Integer> uniqCh = new HashSet<Integer>();
			  for(int j=i;j<matchedChannels.size();j++){
				  long ts1 = queryTimeStamps.get(j);
				  if((ts1-ts)/1000 < 5){	//5 sec window
					  arrlist.add(matchedChannels.get(j));
					  uniqCh.add(matchedChannels.get(j));
				  }//end if
			 }//end for j
			 
			  //find counts of each chId in the next 5 sec
			  HashMap hm = new HashMap();
			  int totalMatches =0; //totalMatches in next 5 sec
			  for(int chId : uniqCh){
				 int cnt = Collections.frequency(arrlist,chId);
				 totalMatches += cnt;
				  hm.put(chId,cnt);
			 }
			  
			  double time = (double)(ts-startTS)/1000.0;
			  System.out.println(time+"\t"+ ch+"\t"+totalMatches+"\t"+hm);
			  
			  //if there are < 10 matches in the next 5 seconds ignore the record
			 if (totalMatches <10){
				 //neglect this record
				 System.out.println(time+"\t"+ ch+"\t"+totalMatches+"\t"+hm);
			 }
			 else{
			  //else find if a chID is present in more than 70% of the time.
				 int maxChId = -1;
				 int maxChIdFreq=-1;
				 
				 Set set = hm.entrySet();
			     Iterator iter = set.iterator();
			     while(iter.hasNext()) {
			         Map.Entry me = (Map.Entry)iter.next();
			         if( ((Integer)me.getValue()) > maxChIdFreq){
			        	 	maxChId=((Integer)me.getKey());
			        	 	maxChIdFreq=((Integer)me.getValue());
			         }//end if
			         
			      }//end while
			     if(maxChIdFreq > 0.8*totalMatches){
			    	 	long diff = queryTimeStamps.get(i) - prevTS;
			    	 	
			    	 	if(diff >4000){
			    	 		System.out.println("********Transition*************************:"+diff);
			    	 	}
			    	 	prevTS = queryTimeStamps.get(i);
			    	 	System.out.println("**"+q+"\t"+time+"\t"+ maxChId+"\t"+hm );
			     }
			     else{
			    	 	//check if 1st/2nd >2 ?
			    	 	ArrayList<Integer> chFreqs = new ArrayList<Integer>();
			    	 	iter = set.iterator();
			    	 	while(iter.hasNext()) {
				         Map.Entry me = (Map.Entry)iter.next();
				         chFreqs.add((Integer)(me.getValue()));
				     }//end while
			    	 	Collections.sort(chFreqs);
			    	 	if(chFreqs.size()>1){
			    	 		int secondMax = chFreqs.get(chFreqs.size()-2);
			    	 		if((double)maxChIdFreq/(double)secondMax > 2.0){
			    	 			long diff = queryTimeStamps.get(i) - prevTS;
				    	 	
			    	 			if(diff >4000){
			    	 				System.out.println("********Transition*************************:"+diff);
			    	 			}
			    	 			prevTS = queryTimeStamps.get(i);
			    	 			System.out.println("**"+q+"\t"+time+"\t"+ maxChId+"\t"+hm );
			    	 		}
			    	 		else	 {
			    	 			System.out.println(time+"\t"+ch +"\t"+totalMatches+"\t"+hm);
			    	 		}
			    	 	}//
			    	 	else{System.out.println(time+"\t"+ch +"\t"+totalMatches+"\t"+hm);}
			     }
			    
			 }//end else
			 
			 
			  
		  }//end for i
		  */
		  
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
