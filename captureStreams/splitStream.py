#!/usr/bin/python
# this program splits the availabel streams into snippetLength audio files
import sys;
import os;
import datetime;
import thread
import time
import commands
import random

def splitStream(f,snippetLength,channelId):
       	startTime = datetime.datetime.strptime((f.split(".")[0]).split("_")[1],"%Y-%m-%d-%H:%M:%S");
	channelName = (f.split(".")[0]).split("_")[0];
	processed = 0;	
	print "OutsideWhileLoop:%s" %(f);
        sys.stdout.flush();

	while 1:
		print "InsideWhileLoop:%s" %(f);
                sys.stdout.flush();
		# get the length of the nisppet
		cmd1 = "ffmpeg -i ../streams/%s 2>&1 " %(f);
		cmd = cmd1 + " | grep 'Duration' | cut -f1 -d\",\" | cut -f2,3,4 -d\":\" | tr ':' '\t' | awk 'BEGIN{OFS=FS=\"\t\";}{printf \"%.0f\", $1*3600+$2*60+$3}'";
		status, output = commands.getstatusoutput(cmd);
		if(output==""):
			print "Waiting:%d:%s" %(snippetLength,f);
			sys.stdout.flush();
			time.sleep(random.random()+snippetLength);
			continue;
		duration = int(output);
		# cut it now into snippetLength chunks if available
		if (duration-processed >= snippetLength):
			timeStamp = (startTime+datetime.timedelta(0,processed)).strftime("%Y-%m-%d-%H:%M:%S");
			snippetFileName = "snippet_%s_%s.wav" %(channelName,timeStamp); 
			cmd = "ffmpeg -i ../streams/%s -ar 8000 -v 0  -t %s -ss %s ../snippets/%s" %(f,str(snippetLength),str(processed),snippetFileName);
			os.system(cmd);
			# now compute fingerprint + hash for the snippet file 
			#os.chdir('../computeFP/');		
			fpFileName = "snippet_%s_%s.mat" %(channelName,timeStamp);
			#cmd = "../computeFP/find_landmarks_standAlone.m ../snippets/%s ../fingerprints/%s %s" %(snippetFileName,fpFileName,str(channelId));
			#java -jar computeFP.jar /tmp/resample.wav out_H 1002
            		cmd = "java -jar ../computeFPJava/computeFP.jar ../snippets/%s ../fingerprints/%s %s 1" %(snippetFileName,fpFileName,str(channelId));
	          	os.system(cmd);	
			#os.chdir('../captureStreams/');


			processed = processed + snippetLength;
		else:
			time.sleep(random.random()+(duration-processed));
			print "Waiting:%d:%s" %((duration-processed),f);
                        sys.stdout.flush();	
	
	
	return;

if __name__ == "__main__":
	if (len(sys.argv)< 3):
		print "wrong arguments: <filename,snippetLength,channelID";
	else:
		splitStream(sys.argv[1],int(sys.argv[2]),int(sys.argv[3]));
