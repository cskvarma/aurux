#/bin/bash
FILE=$1;
op="${FILE%%.*}".wav;
echo $op;

#compute fingerprint
ffmpeg -i ./uploads/$FILE -ar 8000 -ac 2 ./uploads/$op
mat1="${FILE%%.*}".mat1;
mat="${FILE%%.*}".mat;

java -jar ../computeFPJava/computeFP.jar ./uploads/$op  ../buildQueryForExpt/$mat1 1002 2
cat ../buildQueryForExpt/$mat1 | sort -gk2 > ../buildQueryForExpt/$mat

#getmatches
txt="${FILE%%.*}".txt;
java -jar ../matchQueryToChannels/matchQueryToChannels.jar $mat $txt

