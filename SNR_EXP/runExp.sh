#!/bin/bash
vol=$1;
sox -r 8000 -b 16 -n whiteNoise.wav synth 30  pinknoise pinknoise
sox -v -$vol snippet.wav snippetVol.wav
sox -m whiteNoise.wav snippetVol.wav snippetNoise.wav
java -jar ../computeFPJava/computeFP.jar ./snippet.wav snippet_FP 2 1
java -jar ../computeFPJava/computeFP.jar ./snippetNoise.wav snippetNoise_FP 2 1
join -1 3 -2 3 <(sort -k3 snippet_FP) <(sort -k3 snippetNoise_FP ) | tr " " "\t" | cut -f1,3,5 | sort -gk2 | awk 'BEGIN{OFS=FS="\t";}{if($2==$3) print $1,$2*0.032,$3*0.032}' 
join -1 3 -2 3 <(sort -k3 snippet_FP) <(sort -k3 snippetNoise_FP ) | tr " " "\t" | cut -f1,3,5 | awk 'BEGIN{OFS=FS="\t";}{if($2==$3) print}' | wc -l
