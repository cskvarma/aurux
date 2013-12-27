import com.musicg.dsp.LinearInterpolation;
import com.musicg.dsp.Resampler;
import com.musicg.wave.Wave;
import com.musicg.wave.WaveHeader;
import com.musicg.wave.extension.Spectrogram;
import com.sun.media.sound.FFT;

import java.awt.LinearGradientPaint;
import java.io.*;

public class computeFP {

	/**
	 * @param args
	 */
	private static int targetSR = 8000;
	private static int fft_ms = 64;
	private static int fft_hop = 32;
	private static int N = 7; 
	private static int f_sd = 10;
	private static double a_dec = 0.997;
	private static int maxpksperframe = 10;
	private static double hpf_pole = 0.98;
	private static int targetdf = 31;
	private static int targetdt = 63;
	private static String audioFileName = "/Users/cvarma/TAM/code/tvStreams/audio/aajtak_0_300.mp3";
	
	private static double [] locmax(double[] X){
		double[] Y = new double[X.length];
		for(int i=0;i<X.length;i++){
			if(i==0 ){
				if(X[i]>X[i+1]){
					Y[i]=X[i];
				}
				else{
					Y[i]=0;
				}
				
			}
			else if(i==X.length-1){
				/*if(X[i]>X[i-1]){
					Y[i]=X[i];
				}
				else{
					Y[i]=0;
				}*/
				Y[i]=0;
				
			}
			else{
				if((X[i-1] <= X[i]) && (X[i+1]<=X[i])){
					Y[i]=X[i];
				}
				else{
					Y[i]=0;
				}
					
			}
		}
		
		return Y;
	}
	
	private static double[] spread(double[] X, int e){
		double[] Y = new double[X.length];
		int W = 4*e;//e=10 ==> W=40
		//E = exp(-0.5*[(-W:W)/E].^2);
		double[] E= new double[2*W+1];
		for(int i=0;i<E.length;i++){
			E[i] = Math.exp(-0.5*(i-W)/e*(i-W)/e);
			//System.out.println("E"+i+":"+E[i]);
		}
		
		X = locmax(X);
		//Y = 0*X;
		for(int i=0;i<Y.length;i++){
			Y[i]=0*X[i];
		}
		
		//envolope-max
		/*
		for(int i=0;i<X.length;i++){
			if(X[i]==0)
				continue;
			
			for(int j=i-W;j<=i+W;j++){
				if(j<0) continue;
				if(j>=X.length) continue;
				if (X[i]*E[j] > Y[j])
					Y[j]=X[i]*E[j];
				
			}
		}
		*/
		int lenx = X.length;
		int maxi = X.length + E.length;
		int spos = Math.round((E.length)/2);
		
		
		//for i = find(X>0)
		for(int i=0;i<X.length;i++){
			if(X[i]<=0){
				continue;
			}
			//EE = [zeros(1,i),E];
			double[] EE = new double[maxi];
			
			for(int j=0;j<EE.length;j++){
				if(j<i){
					EE[j]=0.0;
				}
				else if (j<i+E.length){
					
					EE[j]=E[j-i];
				}
				else{
					EE[j] = 0;
				}
				
			}//end for j
			
			
			double[] EE_trunc = new double[X.length];
			//EE = EE(spos+(1:lenx));
			
			for(int j=0;j<lenx;j++){
				EE_trunc[j] = EE[spos+j];
				
			}
			
			//Y = max(Y,X(i)*EE);
			for(int k=0;k<Y.length;k++){
				if(Y[k] < X[i]*EE_trunc[k]){
					Y[k]=X[i]*EE_trunc[k];
					
				}
				//System.out.println("debug(Y):"+k+","+Y[k]);
			}//end for k
			
		}//end for i
			  
				
		return Y;
	}
	
	private static double[] filter(double[] x, double hpf_pole){
		
		double[] b = new double[]{1,-1};
        double[] a = new double[]{1, -hpf_pole};
        
        double[] y = new double[x.length];

        for (int n = 0; n < y.length; n++) {
            if(n-1 < 0){
                y[n] = b[0]*x[n];
            }else{
               y[n]= b[0]*x[n]+b[1]*x[n-1]-a[1]*y[n-1]; 
            }

        }
		return y;
	}
	
	public static void main(String[] args) {
		//1a.convert mp3 to wav
		Runtime r = Runtime.getRuntime();
		try{
			String cmd = "/opt/local/bin/mpg123 -q -r 8000 -w  " + " /tmp/resample.wav "+ audioFileName; 
			System.out.println(cmd);
			Process p = r.exec(cmd);
			p.waitFor();
		}
		catch(Exception ex){
			System.out.println("Exception:"+ex);
		}
			
		//1b.read wav file into Wave object
		Wave w = new Wave("/tmp/resample.wav") ;
		//byte[] b = w.getBytes();
		WaveHeader wh = w.getWaveHeader();
		
		double d[]=w.getNormalizedAmplitudes();
		short[] amplitudes=w.getSampleAmplitudes();
		
		
		//2a. average/smooth channels: D = mean(D);
		short[] amplitudesSmoothed = new short [(amplitudes.length/2)];
		int idx=0;
		for (int i=0;i<amplitudes.length-1;i+=2){
			amplitudesSmoothed[idx] = (short)((amplitudes[i]+amplitudes[i+1])/2);
			idx++;
		}
		
	
		//build the new wave after avg-channels  
		WaveHeader wh_new=w.getWaveHeader();
		wh_new.setChannels(1);
		wh_new.setSampleRate(8000);
		Wave w_new = new Wave(wh_new);
		w_new.setSampleAmplitudes(amplitudesSmoothed);
		
		d=w_new.getNormalizedAmplitudes();
		System.out.println("*********");
		/*for(int i=0;i<10;i++){
			System.out.println("d :"+i+":"+d[i]);
		}//end for
		System.out.println("*********");
		*/
		
		//3a.Calc Spectrogram+LogNormalize
		Spectrogram s = new Spectrogram(w_new, 512, 2);
		double s_d[][] = s.getNormalizedSpectrogramData();
		/*
		System.out.println("Spec :"+s_d.length+","+s_d[0].length);
		for(int i=0;i<10;i++){
			System.out.println("Spec :"+i+":"+s_d[i][0]);
		}//end for
		*/
		
		
		//3b. high pass filter in time-domain: http://stackoverflow.com/questions/8504858/matlabs-filter-in-java
		
		double s_highPass[][] = new double [s_d.length][s_d[0].length];
		double hpfPole = 0.98;
       // double[] b = new double[]{1,-1};
       // double[] a = new double[]{1, -hpfPole};
        //double[] x = new double[s_d.length];
        //double[] y = new double[x.length];
		//high pass  on time-dim vector for a given freq
        for(idx=0;idx<s_d[0].length;idx++){
        		double[] x = new double[s_d.length];
        		for (int n = 0; n < s_d.length; n++) {
        			x[n]=s_d[n][idx];
        		}//end for n
        		double[] y = filter(x,hpfPole);
        		for (int n = 0; n < s_d.length; n++) {
        			s_highPass[n][idx]=y[n];
        		}//end for n
        }//edn for idx
        
        /* 
         * Unit test for filter
        double[] X={-1.821511,-1.046346,-2.204245,-0.901026,0.392599,-0.648957,-1.645967,-0.382813,-0.401311,1.122489,1.594587,2.071632,0.872544,0.289720,-1.782406,-0.369161,0.483059,0.646636,-0.301052,1.182476};
        double[] Y = filter(X,0.098);
        System.out.println("*********");
        for(int i=0;i<20;i++){
			System.out.println("Filter :"+i+"|"+X[i]+"|"+Y[i]);
		}//end for
		*/
        
        //4.
        int maxespersec = 30;
        double ddur = (w_new.getSampleAmplitudes().length)/targetSR;
        int nmaxkeep = (int)Math.round(maxespersec * ddur);
        double [][] maxes = new double[3][nmaxkeep];
        int nmaxes = 0;
        int maxix = 0;
        double s_sup=1.0;
        double[] y = locmax(s_highPass[0]);
        int f_sd = 10;
        
        //4a:sthresh = s_sup*spread(max(S(:,1:min(10,size(S,2))),[],2),f_sd)';
        double x[]=new double[s_d[0].length];
        for(int i=0;i<s_d[0].length;i++){
        		x[i]=Double.MIN_VALUE;
        		for(int j=0;j<10;j++){
        			if(s_d[j][i] > x[i])
        				x[i] = s_d[j][i];
        		}
        }
        
        y = spread(x,f_sd);
        double[] sthresh = new double[s_d[0].length];
        for(int i=0; i<sthresh.length;i++){
        		sthresh[i] = s_sup*f_sd*y[i];
        }
        
        
        
        
        
        
        
	}//end main

}
