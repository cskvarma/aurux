%!/opt/local/bin/octave -qf
D = '../buildQueryForExpt/etv2_ss61_t80.mp3';
q = '../buildQueryForExpt/qMobile_2013-12-23-21:50:00.mp3';
pkg load all;
[D,D_SR] = audioread(D);
[q,q_SR] = audioread(q);

% Convert D to a mono row-vector
[nr,nc] = size(D);
if nr > nc
  D = D';
  [nr,nc] = size(D);
end
if nr > 1
  D = mean(D);
  nr = 1;
end
% Resample to target sampling rate
targetSR = 8000;
if (D_SR ~= targetSR)
  D = resample(D,targetSR,D_SR);
end
D_SR=targetSR;


% convert q into mono row vector
[nr,nc] = size(q);
if nr > nc
  q = q';
  [nr,nc] = size(q);
end
if nr > 1
  q = mean(q);
  nr = 1;
end
% Resample to target sampling rate
targetSR = 8000;
if (q_SR ~= targetSR)
  q = resample(q,targetSR,q_SR);
end
q_SR=targetSR;


% spec
fft_ms = 64;
fft_hop = 32;
nfft = round(targetSR/1000*fft_ms);
D_S = abs(specgram(D,nfft,targetSR,nfft,nfft-round(targetSR/1000*fft_hop)));

q_S = abs(specgram(q,nfft,targetSR,nfft,nfft-round(targetSR/1000*fft_hop)));
