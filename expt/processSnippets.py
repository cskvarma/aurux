
# this program reads the rtmpSources file and captures the stmp streams, chunks them into smaller snippets and computes fingerprints
import sys;
import os;
import thread
import time
import random;
from datetime import date, datetime, time, timedelta;

 
if __name__ == "__main__":
	d = dict();
	d['abn']=101;
	d['sakshi'] = 102;
	d['tv5']=103;
	d['etv2']=104;
	d['timesnow']=105;
	d['ntv']=106;
	d['hmtv']=107;
	d['svbc']=108;

	for f in os.listdir("./snippets"):
		name = f.split(".")[0];
		t = name.split("_")[2];
		time = datetime.strptime(t,"%Y-%m-%d-%H:%M:%S");
		time = time-timedelta(0,40);#since actual recording had a mistake of +20 instaed of -20 
		tS = time.strftime("%Y-%m-%d-%H:%M:%S");
		ch = f.split("_")[1];
		chID = d[ch];
		mat = "snippet_"+ch+"_"+tS+".mat";
		print f,mat;
		cmd = "java -jar ../computeFPJava/computeFP.jar ../expt/snippets/%s ../expt/fingerprints/%s %s 1" %(f,mat,chID);
		os.system(cmd);
