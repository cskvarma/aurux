#/bin/bash
#PATH=$PATH:/opt/local/bin:/opt/local/sbin:/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:/opt/X11/bin;
FILE=$1;
op="${FILE%%.*}".wav;
echo $op;

#compute fingerprint
/opt/local/bin/ffmpeg -i ./uploads/$FILE -ar 8000 -ac 2 ./uploads/$op
mat1="${FILE%%.*}".mat1;
mat="${FILE%%.*}".mat;

java -jar ../computeFPJava/computeFP.jar ./uploads/$op  ../buildQueryForExpt/$mat1 1002 2
cat ../buildQueryForExpt/$mat1 | sort -gk2 > ../buildQueryForExpt/$mat
len=`tail -1 ../buildQueryForExpt/$mat  | cut -f2 | awk '{printf "%d", 0.032*$1}'`;
mv ../buildQueryForExpt/$mat ../buildQueryForExpt/$mat.$len

#getmatches
txt="${FILE%%.*}".txt;
java -jar ../matchQueryToChannels/matchQueryToChannels.jar $mat.$len $txt

