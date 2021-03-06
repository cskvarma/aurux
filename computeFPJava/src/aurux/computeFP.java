package aurux;
import com.musicg.dsp.LinearInterpolation;
import com.musicg.dsp.Resampler;
import com.musicg.dsp.WindowFunction;
import com.musicg.wave.Wave;
import com.musicg.wave.WaveHeader;
import com.musicg.wave.extension.Spectrogram;
import com.sun.media.sound.FFT;

import java.awt.LinearGradientPaint;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

public class computeFP {

	/**
	 * @param args
	 */
	private static int targetSR = 8000;
	private static int fft_ms = 64;
	private static int fft_hop = 32;
	private static int N = 7; 
	//private static int f_sd = 10;//10;inv prop to #features
	//private static double a_dec = 0.997;//0.997; inv prop to #features
	//private static int maxpksperframe = 10;//10;
	//private static double hpf_pole = 0.98;
	private static int targetdf = 31;
	private static int targetdt = 63;
	private static String audioFileName = "/Users/cvarma/TAM/code/tvStreams/audio/aajtak_0_300.mp3";
	private static String outFile = "/tmp/out.txt";
	private static int chId = -1;
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
	
	private static void sortDesc(double[] s, double[] v, int[] x ){

		final Integer[] idx = new Integer[s.length];
		final Double[] data = new Double[s.length];
		for(int i=0;i<s.length;i++){
			idx[i]=i;
			data[i]=s[i];
		}
		
		Arrays.sort(idx, new Comparator<Integer>() {
		    @Override public int compare(final Integer o1, final Integer o2) {
		        return Double.compare(data[o1], data[o2]);
		    }
		});
		
		int n=s.length;
		for(int i=0;i<s.length;i++){
			v[i] = s[idx[n-i-1]];
			x[i] = idx[n-i-1];
		}
		
			
	}
	
	public static int[][] computeFingerPrintFromWave(Wave w,int f_sd, double a_dec, int maxpksperframe,double hpf_pole, int maxpairsperpeak){

		//byte[] b = w.getBytes();
		WaveHeader wh = w.getWaveHeader();
		
		System.out.println("wave file params:"+w.getNormalizedAmplitudes().length +":" +w.size()+":"+wh.getChannels()+":"+wh.getSampleRate());
		
		
		double d[]=w.getNormalizedAmplitudes();
		short[] amplitudes=w.getSampleAmplitudes();
		
		
		//2a. average/smooth channels: D = mean(D);
		short[] amplitudesSmoothed=null;
		int idx=0;
		if (wh.getChannels()==2){
			amplitudesSmoothed= new short [(amplitudes.length/2)];
			
			for (int i=0;i<amplitudes.length-1;i+=2){
				amplitudesSmoothed[idx] = (short)((amplitudes[i]+amplitudes[i+1])/2);
				idx++;
			}//edn for
		}//end if
		if (wh.getChannels()==1){
			amplitudesSmoothed= new short [(amplitudes.length)];
			
			for (int i=0;i<amplitudes.length;i+=1){
				amplitudesSmoothed[i] = (short)((amplitudes[i]));
			
			}//end for
		}
		
	
		//build the new wave after avg-channels  
		WaveHeader wh_new=w.getWaveHeader();
		wh_new.setChannels(1);
		wh_new.setSampleRate(8000);
		Wave w_new = new Wave(wh_new);
		w_new.setSampleAmplitudes(amplitudesSmoothed);
		
		d=w_new.getNormalizedAmplitudes();
		System.out.println("*********");
		/*
		//debug normalized amplitudes
		for(int i=99;i<99+10;i++){
			System.out.println("NormalizedAmplitudes :"+i+":"+d[i]);
		}//end for
		System.out.println("*********");
		*/
		
		/*
		//Debug setNormalizedAmplitudes
		short [] x = {1,2,3,4,5,6,7,8,9,10};
	
		Wave w1 = new Wave(wh_new);
		w1.setSampleAmplitudes(x);
		double[] y=w1.getNormalizedAmplitudes();
		
		Wave w2 = new Wave(wh_new);
		w2.setNormalizedAmplitudes(y);
		double[] z=w2.getNormalizedAmplitudes();
		for(int i=0;i<10;i++){
			System.out.println("Debug setNormalizedAmplitudes:"+i+":"+z[i]);
		}
		*/
		
		
		//3a.Calc Spectrogram+LogNormalize
		Spectrogram s = new Spectrogram(w_new, 512, 2);
		double s_d[][] = s.getNormalizedSpectrogramData();
		double s_d_Abs[][] = s.getAbsoluteSpectrogramData();
		
		/*write spectrogram to file */
		
		/*
		System.out.println("Spectrogram :"+s_d_Abs.length+","+s_d_Abs[0].length);
		for(int i=0;i<10;i++){
			System.out.println("Spec :"+i+":"+s_d[0][i]);
		}//end for
		//s= new Spectrogram(x,10,2);
		*/
		
		/*
		//Spec Debug
		double[] x={1.000000,2.000000,3.000000,4.000000,5.000000,6.000000,7.000000,8.000000,9.000000,10.000000,11.000000,12.000000,13.000000,14.000000,15.000000,16.000000,17.000000,18.000000,19.000000,20.000000,21.000000,22.000000,23.000000,24.000000,25.000000,26.000000,27.000000,28.000000,29.000000,30.000000,31.000000,32.000000,33.000000,34.000000,35.000000,36.000000,37.000000,38.000000,39.000000,40.000000,41.000000,42.000000,43.000000,44.000000,45.000000,46.000000,47.000000,48.000000,49.000000,50.000000,51.000000,52.000000,53.000000,54.000000,55.000000,56.000000,57.000000,58.000000,59.000000,60.000000,61.000000,62.000000,63.000000,64.000000,65.000000,66.000000,67.000000,68.000000,69.000000,70.000000,71.000000,72.000000,73.000000,74.000000,75.000000,76.000000,77.000000,78.000000,79.000000,80.000000,81.000000,82.000000,83.000000,84.000000,85.000000,86.000000,87.000000,88.000000,89.000000,90.000000,91.000000,92.000000,93.000000,94.000000,95.000000,96.000000,97.000000,98.000000,99.000000,100.000000,101.000000,102.000000,103.000000,104.000000,105.000000,106.000000,107.000000,108.000000,109.000000,110.000000,111.000000,112.000000,113.000000,114.000000,115.000000,116.000000,117.000000,118.000000,119.000000,120.000000,121.000000,122.000000,123.000000,124.000000,125.000000,126.000000,127.000000,128.000000,129.000000,130.000000,131.000000,132.000000,133.000000,134.000000,135.000000,136.000000,137.000000,138.000000,139.000000,140.000000,141.000000,142.000000,143.000000,144.000000,145.000000,146.000000,147.000000,148.000000,149.000000,150.000000,151.000000,152.000000,153.000000,154.000000,155.000000,156.000000,157.000000,158.000000,159.000000,160.000000,161.000000,162.000000,163.000000,164.000000,165.000000,166.000000,167.000000,168.000000,169.000000,170.000000,171.000000,172.000000,173.000000,174.000000,175.000000,176.000000,177.000000,178.000000,179.000000,180.000000,181.000000,182.000000,183.000000,184.000000,185.000000,186.000000,187.000000,188.000000,189.000000,190.000000,191.000000,192.000000,193.000000,194.000000,195.000000,196.000000,197.000000,198.000000,199.000000,200.000000,201.000000,202.000000,203.000000,204.000000,205.000000,206.000000,207.000000,208.000000,209.000000,210.000000,211.000000,212.000000,213.000000,214.000000,215.000000,216.000000,217.000000,218.000000,219.000000,220.000000,221.000000,222.000000,223.000000,224.000000,225.000000,226.000000,227.000000,228.000000,229.000000,230.000000,231.000000,232.000000,233.000000,234.000000,235.000000,236.000000,237.000000,238.000000,239.000000,240.000000,241.000000,242.000000,243.000000,244.000000,245.000000,246.000000,247.000000,248.000000,249.000000,250.000000,251.000000,252.000000,253.000000,254.000000,255.000000,256.000000,257.000000,258.000000,259.000000,260.000000,261.000000,262.000000,263.000000,264.000000,265.000000,266.000000,267.000000,268.000000,269.000000,270.000000,271.000000,272.000000,273.000000,274.000000,275.000000,276.000000,277.000000,278.000000,279.000000,280.000000,281.000000,282.000000,283.000000,284.000000,285.000000,286.000000,287.000000,288.000000,289.000000,290.000000,291.000000,292.000000,293.000000,294.000000,295.000000,296.000000,297.000000,298.000000,299.000000,300.000000,301.000000,302.000000,303.000000,304.000000,305.000000,306.000000,307.000000,308.000000,309.000000,310.000000,311.000000,312.000000,313.000000,314.000000,315.000000,316.000000,317.000000,318.000000,319.000000,320.000000,321.000000,322.000000,323.000000,324.000000,325.000000,326.000000,327.000000,328.000000,329.000000,330.000000,331.000000,332.000000,333.000000,334.000000,335.000000,336.000000,337.000000,338.000000,339.000000,340.000000,341.000000,342.000000,343.000000,344.000000,345.000000,346.000000,347.000000,348.000000,349.000000,350.000000,351.000000,352.000000,353.000000,354.000000,355.000000,356.000000,357.000000,358.000000,359.000000,360.000000,361.000000,362.000000,363.000000,364.000000,365.000000,366.000000,367.000000,368.000000,369.000000,370.000000,371.000000,372.000000,373.000000,374.000000,375.000000,376.000000,377.000000,378.000000,379.000000,380.000000,381.000000,382.000000,383.000000,384.000000,385.000000,386.000000,387.000000,388.000000,389.000000,390.000000,391.000000,392.000000,393.000000,394.000000,395.000000,396.000000,397.000000,398.000000,399.000000,400.000000,401.000000,402.000000,403.000000,404.000000,405.000000,406.000000,407.000000,408.000000,409.000000,410.000000,411.000000,412.000000,413.000000,414.000000,415.000000,416.000000,417.000000,418.000000,419.000000,420.000000,421.000000,422.000000,423.000000,424.000000,425.000000,426.000000,427.000000,428.000000,429.000000,430.000000,431.000000,432.000000,433.000000,434.000000,435.000000,436.000000,437.000000,438.000000,439.000000,440.000000,441.000000,442.000000,443.000000,444.000000,445.000000,446.000000,447.000000,448.000000,449.000000,450.000000,451.000000,452.000000,453.000000,454.000000,455.000000,456.000000,457.000000,458.000000,459.000000,460.000000,461.000000,462.000000,463.000000,464.000000,465.000000,466.000000,467.000000,468.000000,469.000000,470.000000,471.000000,472.000000,473.000000,474.000000,475.000000,476.000000,477.000000,478.000000,479.000000,480.000000,481.000000,482.000000,483.000000,484.000000,485.000000,486.000000,487.000000,488.000000,489.000000,490.000000,491.000000,492.000000,493.000000,494.000000,495.000000,496.000000,497.000000,498.000000,499.000000,500.000000,501.000000,502.000000,503.000000,504.000000,505.000000,506.000000,507.000000,508.000000,509.000000,510.000000,511.000000,512.000000,513.000000,514.000000,515.000000,516.000000,517.000000,518.000000,519.000000,520.000000,521.000000,522.000000,523.000000,524.000000,525.000000,526.000000,527.000000,528.000000,529.000000,530.000000,531.000000,532.000000,533.000000,534.000000,535.000000,536.000000,537.000000,538.000000,539.000000,540.000000,541.000000,542.000000,543.000000,544.000000,545.000000,546.000000,547.000000,548.000000,549.000000,550.000000,551.000000,552.000000,553.000000,554.000000,555.000000,556.000000,557.000000,558.000000,559.000000,560.000000,561.000000,562.000000,563.000000,564.000000,565.000000,566.000000,567.000000,568.000000,569.000000,570.000000,571.000000,572.000000,573.000000,574.000000,575.000000,576.000000,577.000000,578.000000,579.000000,580.000000,581.000000,582.000000,583.000000,584.000000,585.000000,586.000000,587.000000,588.000000,589.000000,590.000000,591.000000,592.000000,593.000000,594.000000,595.000000,596.000000,597.000000,598.000000,599.000000,600.000000,601.000000,602.000000,603.000000,604.000000,605.000000,606.000000,607.000000,608.000000,609.000000,610.000000,611.000000,612.000000,613.000000,614.000000,615.000000,616.000000,617.000000,618.000000,619.000000,620.000000,621.000000,622.000000,623.000000,624.000000,625.000000,626.000000,627.000000,628.000000,629.000000,630.000000,631.000000,632.000000,633.000000,634.000000,635.000000,636.000000,637.000000,638.000000,639.000000,640.000000,641.000000,642.000000,643.000000,644.000000,645.000000,646.000000,647.000000,648.000000,649.000000,650.000000,651.000000,652.000000,653.000000,654.000000,655.000000,656.000000,657.000000,658.000000,659.000000,660.000000,661.000000,662.000000,663.000000,664.000000,665.000000,666.000000,667.000000,668.000000,669.000000,670.000000,671.000000,672.000000,673.000000,674.000000,675.000000,676.000000,677.000000,678.000000,679.000000,680.000000,681.000000,682.000000,683.000000,684.000000,685.000000,686.000000,687.000000,688.000000,689.000000,690.000000,691.000000,692.000000,693.000000,694.000000,695.000000,696.000000,697.000000,698.000000,699.000000,700.000000,701.000000,702.000000,703.000000,704.000000,705.000000,706.000000,707.000000,708.000000,709.000000,710.000000,711.000000,712.000000,713.000000,714.000000,715.000000,716.000000,717.000000,718.000000,719.000000,720.000000,721.000000,722.000000,723.000000,724.000000,725.000000,726.000000,727.000000,728.000000,729.000000,730.000000,731.000000,732.000000,733.000000,734.000000,735.000000,736.000000,737.000000,738.000000,739.000000,740.000000,741.000000,742.000000,743.000000,744.000000,745.000000,746.000000,747.000000,748.000000,749.000000,750.000000,751.000000,752.000000,753.000000,754.000000,755.000000,756.000000,757.000000,758.000000,759.000000,760.000000,761.000000,762.000000,763.000000,764.000000,765.000000,766.000000,767.000000,768.000000,769.000000,770.000000,771.000000,772.000000,773.000000,774.000000,775.000000,776.000000,777.000000,778.000000,779.000000,780.000000,781.000000,782.000000,783.000000,784.000000,785.000000,786.000000,787.000000,788.000000,789.000000,790.000000,791.000000,792.000000,793.000000,794.000000,795.000000,796.000000,797.000000,798.000000,799.000000,800.000000,801.000000,802.000000,803.000000,804.000000,805.000000,806.000000,807.000000,808.000000,809.000000,810.000000,811.000000,812.000000,813.000000,814.000000,815.000000,816.000000,817.000000,818.000000,819.000000,820.000000,821.000000,822.000000,823.000000,824.000000,825.000000,826.000000,827.000000,828.000000,829.000000,830.000000,831.000000,832.000000,833.000000,834.000000,835.000000,836.000000,837.000000,838.000000,839.000000,840.000000,841.000000,842.000000,843.000000,844.000000,845.000000,846.000000,847.000000,848.000000,849.000000,850.000000,851.000000,852.000000,853.000000,854.000000,855.000000,856.000000,857.000000,858.000000,859.000000,860.000000,861.000000,862.000000,863.000000,864.000000,865.000000,866.000000,867.000000,868.000000,869.000000,870.000000,871.000000,872.000000,873.000000,874.000000,875.000000,876.000000,877.000000,878.000000,879.000000,880.000000,881.000000,882.000000,883.000000,884.000000,885.000000,886.000000,887.000000,888.000000,889.000000,890.000000,891.000000,892.000000,893.000000,894.000000,895.000000,896.000000,897.000000,898.000000,899.000000,900.000000,901.000000,902.000000,903.000000,904.000000,905.000000,906.000000,907.000000,908.000000,909.000000,910.000000,911.000000,912.000000,913.000000,914.000000,915.000000,916.000000,917.000000,918.000000,919.000000,920.000000,921.000000,922.000000,923.000000,924.000000,925.000000,926.000000,927.000000,928.000000,929.000000,930.000000,931.000000,932.000000,933.000000,934.000000,935.000000,936.000000,937.000000,938.000000,939.000000,940.000000,941.000000,942.000000,943.000000,944.000000,945.000000,946.000000,947.000000,948.000000,949.000000,950.000000,951.000000,952.000000,953.000000,954.000000,955.000000,956.000000,957.000000,958.000000,959.000000,960.000000,961.000000,962.000000,963.000000,964.000000,965.000000,966.000000,967.000000,968.000000,969.000000,970.000000,971.000000,972.000000,973.000000,974.000000,975.000000,976.000000,977.000000,978.000000,979.000000,980.000000,981.000000,982.000000,983.000000,984.000000,985.000000,986.000000,987.000000,988.000000,989.000000,990.000000,991.000000,992.000000,993.000000,994.000000,995.000000,996.000000,997.000000,998.000000,999.000000,1000.000000,1001.000000,1002.000000,1003.000000,1004.000000,1005.000000,1006.000000,1007.000000,1008.000000,1009.000000,1010.000000,1011.000000,1012.000000,1013.000000,1014.000000,1015.000000,1016.000000,1017.000000,1018.000000,1019.000000,1020.000000,1021.000000,1022.000000,1023.000000,1024.000000};
		Wave w1 = new Wave(wh_new);
		w1.setNormalizedAmplitudes(x);
		Spectrogram s = new Spectrogram(w1, 512, 0);
		double s_d[][] = s.getAbsoluteSpectrogramData();//s.getNormalizedSpectrogramData();
		System.out.println("Spectrogram :"+s_d.length+","+s_d[0].length);
		for(int i=0;i<10;i++){
			System.out.println("Spec :"+i+":"+s_d[0][i]);
		}//end for
		*/
		
		
		
		/*
		//Debug Hanning window
		WindowFunction window = new WindowFunction();
		window.setWindowType("HANNING");
		double[] win=window.generate(10);
		for(int i=0;i<10;i++){
			System.out.println("Hanning:"+i+":"+win[i]);
		}
		*/
		
		
		 
	
		 
		
		//3b. high pass filter in time-domain: http://stackoverflow.com/questions/8504858/matlabs-filter-in-java
		
		double s_highPass[][] = new double [s_d.length][s_d[0].length];
		//double hpfPole = 0.98;
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
        		double[] y = filter(x,hpf_pole);
        		for (int n = 0; n < s_d.length; n++) {
        			s_highPass[n][idx]=y[n];
        		}//end for n
        }//end for idx
        /*
         // Debug highPass on the matrix
        System.out.println("After HighPass :"+s_highPass.length+","+s_highPass[0].length);
		for(int i=9360;i<s_highPass.length;i++){
			System.out.println("After HighPass  :"+i+":"+s_highPass[i][10]);
		}//end for
        if(1==1)
        		return;
        */
        
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
		
        int maxespersec = 500;//30
        double ddur = (w_new.getSampleAmplitudes().length)/targetSR;
        System.out.println("duration  :"+ddur);
        int nmaxkeep = (int)Math.round(maxespersec * ddur);
        double [][] maxes = new double[3][nmaxkeep];
        int nmaxes = -1;
        int maxix = 0;
        double s_sup=1.0;
        double[] y = locmax(s_highPass[0]);
        //int f_sd = 10;
        
        //4a:sthresh = s_sup*spread(max(S(:,1:min(10,size(S,2))),[],2),f_sd)';
        
        //x = max(S(:,1:10));
        double x[]=new double[s_highPass[0].length];
        for(int i=0;i<s_highPass[0].length;i++){
        		x[i]=Double.MIN_VALUE;
        		for(int j=0;j<10;j++){// hardcoded 10 here
        			if(s_highPass[j][i] > x[i])
        				x[i] = s_highPass[j][i];
        		}
        }
        /*
         //Debug X
        for(int i=0;i<x.length;i++){
			System.out.println("x  :"+(i+1)+":"+x[i]);
		}//end for
        */
        y = spread(x,f_sd);
        double[] sthresh = new double[s_highPass[0].length];
        for(int i=0; i<sthresh.length;i++){
        		sthresh[i] = s_sup*y[i];
        }
        /*
        //debug
        for(int i=0;i<sthresh.length;i++){
			System.out.println("sthresh  :"+(i+1)+":"+sthresh[i]);
		}//end for
        */
        
        
        /*
        //debug sortDesc
        double[] s123 = {3.1,4.3,5.3,6.5,7.2};
        double[] v =new double[s123.length];
        int[] xx = new int[s123.length];
        sortDesc(s123,v,xx);
        for(int i=0;i<s123.length;i++)
        		System.out.println(i+"|"+s123[i]+"|"+v[i]+"|"+xx[i]);
        		
        */		
        
        //4b
        double[][] T= new double[s_highPass.length][s_highPass[0].length];
        //for i = 1:size(S,2)-1
        for(int i=0;i<s_highPass.length-1;i++){
        		  //s_this = S(:,i);
        		  double[] s_this = new double[s_highPass[0].length];
        		  for(int j=0;j<s_highPass[0].length;j++)
        			  s_this[j] = s_highPass[i][j];
        		  
        		  //sdiff = max(0,(s_this - sthresh))';
        		  double[] sdiff = new double[s_highPass[0].length];
        		  for(int j=0;j<sdiff.length;j++){
        			  if(s_this[j]-sthresh[j] > 0)
        				  sdiff[j] = s_this[j]-sthresh[j];
        			  else
        				  sdiff[j] = 0.0;
        		  }
        			
        		  //sdiff = locmax(sdiff);
        		  double[] sdiff_1 = new double[s_highPass[0].length];
        		  sdiff_1 = locmax(sdiff);
        		  //sdiff(end) = 0;  % i.e. bin 257 from the sgram
        		 
        		 // [vv,xx] = sort(sdiff, 'descend');
        		  double []vv= new double [sdiff_1.length];
        		  int[] xx= new int[sdiff_1.length];
        		  sortDesc(sdiff_1, vv,xx);
        		 // xx = xx(vv>0);
        		  int cnt=0;
        		  int [] xx_1 = new int[xx.length];
        		  for(int j=0;j<xx.length;j++){
        			  if(vv[j] > 0){
        				  xx_1[cnt]=xx[j];
        				 cnt++; 
        			  }//end if
        		  }//end for j
        		 
        		  int nmaxthistime = 0;
        		  //for j = 1:length(xx)
        		  for(int j=0;j<cnt;j++){
        		    int p = xx_1[j];
        		    if (nmaxthistime < maxpksperframe){
        		    		if (s_this[p] > sthresh[p]){
        		    			nmaxthistime = nmaxthistime + 1;
        		    			nmaxes = nmaxes + 1;
        		    			//System.out.println(p+":"+s_this[p]+":"+nmaxkeep+":"+nmaxes);
        		    			maxes[1][nmaxes] = p;
        		    			maxes[0][nmaxes] = i;
        		    			maxes[2][nmaxes] = s_this[p];
        		    			//eww = exp(-0.5*(([1:length(sthresh)]'- p)/f_sd).^2);
        		    			double[] eww= new double[sthresh.length];
        		    			for(int k=0;k<eww.length;k++){
        		    				eww[k] = Math.exp(-0.5*(k-p)/f_sd*(k-p)/f_sd);
        		    			}//end k
        		    			//sthresh = max(sthresh, s_this(p)*s_sup*eww);
        		    			for(int k=0;k<sthresh.length;k++){
        		    				if(sthresh[k] < s_this[p]*s_sup*eww[k])
        		    					sthresh[k] = s_this[p]*s_sup*eww[k];
        		    			}//end for k
        		    		}//end if
        		    }//end if
        		  }//end for j
        		  
        		  //T(:,i) = sthresh;
        		  for(int j=0;j<sthresh.length;j++){
        			  T[i][j] = sthresh[j];
        			  //System.out.println(sthresh.length+" T_"+i+"_"+j+":"+T[i][j]);
        		  }
        		  
        		  //sthresh = a_dec*sthresh;
        		  for(int k=0;k<sthresh.length;k++)
        			  sthresh[k]=sthresh[k]*a_dec;
        }//end for i
       /* 
       //Debug
        System.out.println("nmaxes:"+nmaxes);
        for(int i=0;i<nmaxes+1;i++){
        		System.out.println("maxes_3:"+i+":"+maxes[2][i]);
        }
       if(1==1)
    	   	return;
        */
        //4c
        //Backwards pruning of maxes
        double [][] maxes2 = new double[2][nmaxkeep];
        int nmaxes2 = -1;
        int whichmax = nmaxes;
        
        //sthresh = s_sup*spread(S(:,end),f_sd)';
        for(int i=0;i<x.length;i++)
        		x[i]=s_highPass[s_highPass.length-1][i];
        	y = spread(x,f_sd);
        for(int i=0; i<sthresh.length;i++){
    			sthresh[i] = s_sup*y[i];
        }
        
        
        //for i = (size(S,2)-1):-1:1
        for(int i=s_highPass.length-2;i>=0;i--){
        		//while whichmax > 0 && maxes(1,whichmax) == i
        		while(whichmax > 0 && maxes[0][whichmax] == i){
        			double p = maxes[1][whichmax];
                double v = maxes[2][whichmax];
                if  (v >= sthresh[(int)p]){
                      //% keep this one
                      nmaxes2 = nmaxes2 + 1;
                      maxes2[0][nmaxes2] = i;
                      maxes2[1][nmaxes2] = p;
                      double[] eww= new double[sthresh.length];
                      //eww = exp(-0.5*(([1:length(sthresh)]'- p)/f_sd).^2);
		    			  for(int k=0;k<eww.length;k++){
		    				eww[k] = Math.exp(-0.5*(k-p)/f_sd*(k-p)/f_sd);
		    			   }//end k
                      
                      //sthresh = max(sthresh, v*s_sup*eww);
		    			  for(int k=0;k<sthresh.length;k++){
  		    				if(sthresh[k] < v*s_sup*eww[k])
  		    					sthresh[k] = v*s_sup*eww[k];
  		    			}//end for k
                }//end if
                    whichmax = whichmax - 1;
                
        		}//end while
        	// sthresh = a_dec*sthresh;
        	for(int j=0;j<sthresh.length;j++)
        		sthresh[j]=a_dec*sthresh[j];
        }//end for i
        /*
        //Debug
         for(int i=0;i<100;i++){
         	System.out.println("maxes2_3:"+i+":"+maxes2[0][i]+"|"+maxes2[1][i]+"|"+nmaxes2);
         }
       */
       
        //maxes2 = fliplr(maxes2);
        int [][] maxes2_fl = new int[2][nmaxes2+1];
        int ntmp = nmaxes2;
        for(int i=0;i<nmaxes2+1;i++){
        		
        		maxes2_fl[0][i]=(int) maxes2[0][ntmp-i];
        		maxes2_fl[1][i]=(int) maxes2[1][ntmp-i];
        }
       /* 
       //Debug
        for(int i=0;i<100;i++){
        		System.out.println("maxes2fl_3:"+i+":"+maxes2_fl[0][i]+"|"+maxes2_fl[1][i]+"|"+nmaxes2);
        }
        */
        
        //4c
        
        //%% Pack the maxes into nearby pairs = landmarks

        		//% Limit the number of pairs that we'll accept from each peak
        		//int maxpairsperpeak=3;

        		//% Landmark is <starttime F1 endtime F2>
        		//L = zeros(nmaxes2*maxpairsperpeak,4);
        		double[][] L= new double[nmaxes2*maxpairsperpeak][4];
        		int nlmarks = -1;

        		//for i =1:nmaxes2
        		for(int i=0;i<nmaxes2+1;i++){
        			/*if(nlmarks==276){
          			  System.out.println("Descrepancy");
          		  }*/	
        		  int startt = maxes2_fl[0][i];
        		  int F1 = maxes2_fl[1][i];
        		  int maxt = startt + targetdt;
        		  int  minf = F1 - targetdf;
        		  int  maxf = F1 + targetdf;
        		  ArrayList <Integer> matchmaxs = new ArrayList<Integer>();
        		  //matchmaxs = find((maxes2(1,:)>startt)&(maxes2(1,:)<maxt)&(maxes2(2,:)>minf)&(maxes2(2,:)<maxf));
        		  for(int j=0;j<nmaxes2+1;j++){
        			  if( (maxes2_fl[0][j] > startt) && (maxes2_fl[0][j] < maxt) && (maxes2_fl[1][j] > minf) && (maxes2_fl[1][j] < maxf) ){
        				  matchmaxs.add(j);
        				  //System.out.println(j);
        			  }
        			  		
        		  }
        		  //System.out.println(i+":"+ matchmaxs.size());
        		  
        		  if (matchmaxs.size() > maxpairsperpeak){
        		    //% limit the number of pairs we make; take first ones, as they
        		    //% will be closest in time
        		    //matchmaxs = matchmaxs(1:maxpairsperpeak);
        			  matchmaxs = new ArrayList <Integer> (matchmaxs.subList(0, maxpairsperpeak));
        		  }//end if
        		  //System.out.println(i+":"+ matchmaxs.size());
        		  
        		  
        		  //for match = matchmaxs
        		  for(int match :matchmaxs){
        		    nlmarks = nlmarks+1;
        		    L[nlmarks][0] = startt+1;
        		    L[nlmarks][1] = F1+1;
        		    L[nlmarks][2] = maxes2_fl[1][match]+1;  //% frequency row
        		    L[nlmarks][3] = maxes2_fl[0][match]-startt; // % time column difference
        		  }//end for match
        		}//end for i
        		//L = L(1:nlmarks,:);
        		System.out.println("nlmarks:"+nlmarks);
        		
        		/*
        		//Debug
        		for(int i=nlmarks-100;i< nlmarks+1;i++){
        			System.out.println("FP:"+i+"|"+L[i][0] + "|"+ L[i][1]+"|" + L[i][2]+"|" +L[i][3] );
        		}
        		*/
        		
        		//Compute Landmarks out of FP
        		
        		int[][] H = new int[nlmarks+1][3];
        		for(int i=0; i<nlmarks+1; i++){
        			//F1 = rem(round(L(:,2)-1),2^8);
        			int F1 = (int)L[i][1]-1;
        			F1= F1%256;
        			//System.out.println("F1="+F1);
        			
        			//DF = round(L(:,3)-L(:,2));
        			int DF = (int) (L[i][2]-L[i][1]);
        			//System.out.println("DF="+DF);
        			//if DF < 0
        			//  DF = DF + 2^8;
        			//  end
        			//if (DF < 0)
        			//  DF = DF + 256;
        			
        			//DF = rem(DF,2^6);
        			DF = DF%64;
        			//System.out.println("DF="+DF);
        			
        			//DT = rem(abs(round(L(:,4))), 2^6);
        			int DT = (int) L[i][3];
        			DT = DT%64;
        			//System.out.println("DT="+DT);
        			
        			//H = [S,H,uint32(F1*(2^12)+DF*(2^6)+DT)];
        			H[i][0] = chId;
        			H[i][1] = (int) L[i][0];
        			H[i][2] = F1*4096+DF*64+DT;
        			
        			//Debug
        			//System.out.println("LM:"+i+"|"+H[i][0]+"|"+H[i][1]+"|" + H[i][2]+"|"+L[i][0] + "|"+ L[i][1]+"|" + L[i][2]+"|" +L[i][3]);
        		}//end for i LP
        		
        		return H;
		
	}
	
	public static void computeFingerPrintFromFileToFile(String audFileName, String outF, int Ch) {
		audioFileName=audFileName;
		outFile=outF;
		chId = Ch;
		System.out.println("input-file:"+audioFileName);
		System.out.println("output-file:"+outFile);
			
		//1a.convert mp3 to wav
		
		/*
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
		*/	
		
		//1b.read wav file into Wave object
		Wave w = new Wave(audioFileName) ;
		int [][] H = computeFingerPrintFromWave(w,10,0.997,10,0.98,3);
        	int nlmarks = H.length;	
		//Write output to file
        	Writer writer = null;

        	try {
        		   writer = new BufferedWriter(new OutputStreamWriter(
        		          new FileOutputStream(outFile), "utf-8"));
        		    for(int i=0; i<nlmarks; i++){
        		    		writer.write(H[i][0]+"\t"+H[i][1]+"\t"+H[i][2]+"\n");
        		    }
        		} catch (IOException ex) {
        		  // report
        		} finally {
        		   try {writer.close();} catch (Exception ex) {}
        		}
        		
        System.out.println("Done");
        
        
	}//end 

	
	public static void computeFingerPrintFromFileToFileQuery(String audFileName, String outF, int Ch) {
		audioFileName=audFileName;
		outFile=outF;
		chId = Ch;
		System.out.println("input-file:"+audioFileName);
		System.out.println("output-file:"+outFile);
			
		int f_sd = 10;//10;
		double a_dec=0.97;//0.97;
		int maxpksperframe=30;//30;
		double hpf_pole=0.98;
		int noNeighborsPerPeak=50;
		
		
		//1b.read wav file into Wave object
		Wave w = new Wave(audioFileName) ;
		
		//WaveHeader wh = w.getWaveHeader();
		
		//System.out.println("wave file params:"+w.getNormalizedAmplitudes().length +":" +w.size()+":"+wh.getChannels()+":"+wh.getSampleRate());
		
		
		/*
		% Augment with landmarks calculated half-a-window advanced too
		Lq = find_landmarks(D,SR,N);
		landmarks_hopt = 0.032;
		Lq = [Lq;find_landmarks(D(round(landmarks_hopt/4*SR):end),SR, N)];
		Lq = [Lq;find_landmarks(D(round(landmarks_hopt/2*SR):end),SR, N)];
		Lq = [Lq;find_landmarks(D(round(3*landmarks_hopt/4*SR):end),SR, N)];
		% add in quarter-hop offsets too for even better recall
		*/
		WaveHeader wh = w.getWaveHeader();
		
		System.out.println("0 wave file params:"+w.getNormalizedAmplitudes().length +":" +w.size()+":"+wh.getChannels()+":"+wh.getSampleRate());
		
		int [][] H1 = computeFingerPrintFromWave(w,f_sd,a_dec,maxpksperframe,hpf_pole,noNeighborsPerPeak);
		//***************************************************//
		
		w = new Wave(audioFileName) ;
		wh = w.getWaveHeader();
		//System.out.println("1 wave file params:"+w.getNormalizedAmplitudes().length +":" +w.size()+":"+wh.getChannels()+":"+wh.getSampleRate());
		
		short[] amplitudes=w.getSampleAmplitudes();
		//truncate 64 
		short[] amplitudesTrunc=null;
		int idx=0;
		if (wh.getChannels()==2){
			amplitudesTrunc= new short [(amplitudes.length-64*2)];
			
			for (int i=64*2;i<amplitudes.length;i++){
				amplitudesTrunc[idx] = (short)((amplitudes[i]));
				idx++;
			}//edn for
		}//end if
		
		w.setSampleAmplitudes(amplitudesTrunc);
		
		int [][] H2 = computeFingerPrintFromWave(w,f_sd,a_dec,maxpksperframe,hpf_pole,noNeighborsPerPeak);
        
		//System.out.println("2 wave file params:"+w.getNormalizedAmplitudes().length +":" +w.size()+":"+wh.getChannels()+":"+wh.getSampleRate());
		
		//***************************************************//
		
		w = new Wave(audioFileName) ;
		wh = w.getWaveHeader();
		//System.out.println("1 wave file params:"+w.getNormalizedAmplitudes().length +":" +w.size()+":"+wh.getChannels()+":"+wh.getSampleRate());
		
		amplitudes=w.getSampleAmplitudes();
		//truncate 128 
		amplitudesTrunc=null;
		
		if (wh.getChannels()==2){
			amplitudesTrunc= new short [(amplitudes.length-128*2)];
			idx=0;
			for (int i=128*2;i<amplitudes.length;i++){
				amplitudesTrunc[idx] = (short)((amplitudes[i]));
				idx++;
			}//edn for
		}//end if
		
		w.setSampleAmplitudes(amplitudesTrunc);
		//w.leftTrim(0.008);
		int [][] H3 = computeFingerPrintFromWave(w,f_sd,a_dec,maxpksperframe,hpf_pole,noNeighborsPerPeak);
		
		//***************************************************//
		
		w = new Wave(audioFileName) ;
		wh = w.getWaveHeader();
		//System.out.println("1 wave file params:"+w.getNormalizedAmplitudes().length +":" +w.size()+":"+wh.getChannels()+":"+wh.getSampleRate());
		
		amplitudes=w.getSampleAmplitudes();
		//truncate 192 
		amplitudesTrunc=null;
		
		if (wh.getChannels()==2){
			amplitudesTrunc= new short [(amplitudes.length-192*2)];
			idx=0;
			for (int i=192*2;i<amplitudes.length;i++){
				amplitudesTrunc[idx] = (short)((amplitudes[i]));
				idx++;
			}//edn for
		}//end if
		
		w.setSampleAmplitudes(amplitudesTrunc);
		
		
		//w.leftTrim(0.008);
		int [][] H4 = computeFingerPrintFromWave(w,f_sd,a_dec,maxpksperframe,hpf_pole,noNeighborsPerPeak);
		
		//find unique landmarks
		HashSet<String> set = new HashSet<String>();
		for(int i=0;i<H1.length;i++){
			StringBuffer sb = new StringBuffer();
			sb.append(H1[i][0]+"-"+H1[i][1]+"-"+H1[i][2]);
			set.add(sb.toString());
		}
		for(int i=0;i<H2.length;i++){
			StringBuffer sb = new StringBuffer();
			sb.append(H2[i][0]+"-"+H2[i][1]+"-"+H2[i][2]);
			set.add(sb.toString());
		}
		for(int i=0;i<H3.length;i++){
			StringBuffer sb = new StringBuffer();
			sb.append(H3[i][0]+"-"+H3[i][1]+"-"+H3[i][2]);
			set.add(sb.toString());
		}
		for(int i=0;i<H4.length;i++){
			StringBuffer sb = new StringBuffer();
			sb.append(H4[i][0]+"-"+H4[i][1]+"-"+H4[i][2]);
			set.add(sb.toString());
		}
		
		int [][] H = new int[set.size()][3];
		Iterator<String> iter = set.iterator();
		int i=0;
		   while (iter.hasNext()) {
		     String [] temp = ((String)iter.next()).split("-");
		     H[i][0] = Integer.parseInt(temp[0]);
		     H[i][1] = Integer.parseInt(temp[1]);
		     H[i][2] = Integer.parseInt(temp[2]);
		     ++i;
		   }
		
		
		int nlmarks = H.length;
		System.out.println("# query landmarks:"+nlmarks);
		//Write output to file
        	Writer writer = null;

        	try {
        		   writer = new BufferedWriter(new OutputStreamWriter(
        		          new FileOutputStream(outFile), "utf-8"));
        		    for( i=0; i<nlmarks; i++){
        		    		writer.write(H[i][0]+"\t"+H[i][1]+"\t"+H[i][2]+"\n");
        		    }
        		} catch (IOException ex) {
        		  // report
        		} finally {
        		   try {writer.close();} catch (Exception ex) {}
        		}
        		
        System.out.println("Done");
        
        
	}//end main
	public static void main(String args[]){
		if (Integer.parseInt(args[3])==1)
			computeFingerPrintFromFileToFile(args[0],args[1],Integer.parseInt(args[2]));
		else
			computeFingerPrintFromFileToFileQuery(args[0],args[1],Integer.parseInt(args[2]));
	}
}
