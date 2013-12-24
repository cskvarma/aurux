#!/bin/bash
rtmpdump -r "rtmp://cdn.m.yupptv.tv:80/liveorigin/" -a "liveorigin/" --live -y "ndtvhindi9002" -o ndtvhindi.flv &
rtmpdump -r rtmp://cdn.m.yupptv.tv/liveorigin/ndtvenglish9001 -o ndtv.flv &
rtmpdump -r rtmp://cdn.m.yupptv.tv/liveorigin/aajtak1 -o aajtak.flv &
rtmpdump -r rtsp://cdn.m.yupptv.tv/liveorigin/abn1 -o abn.flv &
wget http://bglive-a.bitgravity.com/ndtv/prolo/live/native -O ndtv_profit.flv &
rtmpdump -r "rtmp://cdn.m.yupptv.tv/liveorigin/sakshi1" -o sakshi1.flv &
rtmpdump -r "rtmp://cdn.m.yupptv.tv/liveorigin/tv51" -o tv5.flv &
rtmpdump -r rtmp://cdn.m.yupptv.tv/liveorigin/etv21 -o etv2.flv &
rtmpdump -r rtmp://cdn.m.yupptv.tv/liveorigin/timesnow1 -o timesnow.flv &
rtmpdump -r rtmp://cdn.m.yupptv.tv/liveorigin/ntv1 -o ntv.flv &
rtmpdump -r rtmp://cdn.m.yupptv.tv/liveorigin/hmtv1 -o hmtv.flv &
rtmpdump -r rtmp://cdn.m.yupptv.tv/liveorigin/svbc1 -o svbc.flv &


