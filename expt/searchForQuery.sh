#/bin/bash
#PATH=$PATH:/opt/local/bin:/opt/local/sbin:/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:/opt/X11/bin;
FILE=$1;
op="${FILE%%.*}".wav;
echo $op;

#compute fingerprint
/opt/local/bin/ffmpeg -i ./$FILE -ar 8000 -ac 2 ./$op
mat1="${FILE%%.*}".mat1;
mat="${FILE%%.*}".mat;

java -jar ../computeFPJava/computeFP.jar ./$op  ./$mat1 1002 2
cat ./$mat1 | sort -gk2 > ./$mat
len=`tail -1 ./$mat  | cut -f2 | awk '{printf "%d", 0.032*$1}'`;
mv ./$mat ./$mat.$len

#getmatches
txt="${FILE%%.*}".txt;
java -jar ../matchQueryToChannels/matchQueryToChannels.jar $mat.$len $txt

