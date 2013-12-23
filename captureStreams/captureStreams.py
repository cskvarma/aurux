#!/usr/bin/python
# this program reads the rtmpSources file and captures the stmp streams, chunks them into smaller snippets and computes fingerprints
import sys;
import os;
import datetime; 
import thread
import time
from splitStream import splitStream;

def captureSingleStream(cmd):
	os.system(cmd);	
	return;
def splitStreamCmd(cmd):
	os.system(cmd);
	return;
 
if __name__ == "__main__":
	f = open('./rtmpSources')
	lines = f.readlines()
	f.close()
	snippetLength = 30;
	for line in lines:
		# neglect first line in the rtmpSources
		if (lines.index(line)== 0):
			continue;
		values = line.split('\t');
		timeStamp = (datetime.datetime.now()+datetime.timedelta(0,int(values[1]))).strftime("%Y-%m-%d-%H:%M:%S");
		#capture-thread
		cmd = "ffmpeg -i %s -v 0 ../streams/%s_%s.mp3 & " %(values[0],values[2],str(timeStamp));
		try:
   			thread.start_new_thread( captureSingleStream, (cmd, ) );
		except:
   			print "Error: unable to start capture thread";
		
		#splitStream thread + fingerprint + Hash
		streamName = "%s_%s.mp3" %(values[2],str(timeStamp));
		try:
			print streamName,snippetLength,values[3];
                        thread.start_new_thread( splitStream, (streamName,snippetLength,int(values[3]),));
			#cmd = "./splitStream.py %s %s %s &" %(streamName,snippetLength,values[3]) ;
                	#thread.start_new_thread( splitStreamCmd, (cmd, ) );
		except:
                        print "Error: unable to start split thread";
	while 1:
   		pass

