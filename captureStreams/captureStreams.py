#!/usr/bin/python
# this program reads the rtmpSources file and captures the stmp streams, chunks them into smaller snippets and computes fingerprints
import sys;
import os;
import datetime; 
import thread
import time
from splitStream import splitStream;
import random;

def captureSingleStream(cmd):
	os.system(cmd);	
	return;
 
if __name__ == "__main__":
	f = open('./rtmpSources')
	lines = f.readlines()
	f.close()
	snippetLength = 30;
	timeStampsList=[];
	for line in lines:
		# neglect first line in the rtmpSources
		if (lines.index(line)== 0):
			continue;
		values = line.split('\t');
		timeStamp = (datetime.datetime.now()+datetime.timedelta(0,int(values[1]))).strftime("%Y-%m-%d-%H:%M:%S");
		timeStampsList.append(timeStamp);
		#capture-thread
		cmd = "ffmpeg -i %s -v 0 ../streams/%s_%s.ogg & " %(values[0],values[2],str(timeStamp));
		try:
   			thread.start_new_thread( captureSingleStream, (cmd, ) );
		except:
   			print "Error: unable to start capture thread";


	i=0;
	for line in lines:		
		#splitStream thread + fingerprint + Hash
		if (lines.index(line)== 0):
                        continue;
                values = line.split('\t');
                timeStamp = timeStampsList[i]; 
		streamName = "%s_%s.ogg" %(values[2],str(timeStamp));
		time.sleep(random.random()*30);
		try:
			print streamName,snippetLength,values[3];
                        thread.start_new_thread( splitStream, (streamName,snippetLength,int(values[3]),));
			#cmd = "./splitStream.py %s %s %s &" %(streamName,snippetLength,values[3]) ;
                	#thread.start_new_thread( splitStreamCmd, (cmd, ) );
		except:
                        print "Error: unable to start split thread";
		i = i+1;
	while 1:
   		pass

