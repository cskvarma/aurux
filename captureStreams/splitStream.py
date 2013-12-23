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
	print "\nInside split:%s" % (f);
        sys.stdout.flush();

	while 1:
		# get the length of the nisppet
		cmd1 = "ffmpeg -i ../streams/%s 2>&1 " %(f);
		cmd = cmd1 + " | grep 'Duration' | cut -f1 -d\",\" | cut -f2,3,4 -d\":\" | tr ':' '\t' | awk 'BEGIN{OFS=FS=\"\t\";}{printf \"%.0f\", $1*3600+$2*60+$3}'";
		status, output = commands.getstatusoutput(cmd);
		if(output==""):
			time.sleep(random.random()+snippetLength);
			continue;
		duration = int(output);
		# cut it now into snippetLength chunks if available
		if (duration-processed >= snippetLength):
			timeStamp = (startTime+datetime.timedelta(0,processed)).strftime("%Y-%m-%d-%H:%M:%S");
			snippetFileName = "snippet_%s_%s.mp3" %(channelName,timeStamp); 
			cmd = "ffmpeg -i ../streams/%s -v 0  -t %s -ss %s ../snippets/%s" %(f,str(snippetLength),str(processed),snippetFileName);
			os.system(cmd);
			# now compute fingerprint + hash for the snippet file 
			#os.chdir('../computeFP/');		
			fpFileName = "snippet_%s_%s.mat" %(channelName,timeStamp);
			cmd = "../computeFP/find_landmarks_standAlone.m ../snippets/%s ../fingerprints/%s %s" %(snippetFileName,fpFileName,str(channelId));
			os.system(cmd);	
			#os.chdir('../captureStreams/');


			processed = processed + snippetLength;
		else:
			time.sleep(random.random()+(duration-processed));	
	
	
	return;

if __name__ == "__main__":
	if (len(sys.argv)< 3):
		print "wrong arguments: <filename,snippetLength,channelID";
	else:
		splitStream(sys.argv[1],int(sys.argv[2]),int(sys.argv[3]));
