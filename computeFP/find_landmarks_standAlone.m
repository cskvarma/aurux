#!/opt/local/bin/octave -qf
arg_list = argv ();
printf (" %s", arg_list{1});
D=arg_list{1}; # input file
f = arg_list{2}; # output file
s = arg_list{3}; # channelId
printf ("\n");
addpath("../computeFP");

pkg load all;
%function [L,S,T,maxes] = find_landmarks(D,SR,N)
[D,SR] = audioread(D);
N=7;
[L,S,T,maxes] = find_landmarks(D,SR,N);
%f = sprintf("%s_L.mat",arg_list{1});
H = landmark2hash(L,str2num(s));
%save(f,'H');
fid = fopen(f,"w");
for i=1:size(H,1)
	fprintf(fid,"%d\t%d\t%d\n",H(i,1),H(i,2),H(i,3));	
end
fclose(fid);






