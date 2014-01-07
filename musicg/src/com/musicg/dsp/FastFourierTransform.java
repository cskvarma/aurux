/*
 * Copyright (C) 2011 Jacquet Wong
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.musicg.dsp;

import com.sun.media.sound.FFT;

/**
 * FFT object, transform amplitudes to frequency intensities
 * 
 * @author Jacquet Wong
 * 
 */
public class FastFourierTransform {

	/**
	 * Get the frequency intensities
	 * 
	 * @param amplitudes
	 *            amplitudes of the signal
	 * @return intensities of each frequency unit: mag[frequency_unit]=intensity
	 */
	public double[] getMagnitudes(double[] amplitudes) {

		int sampleSize = amplitudes.length;
		// call the fft and transform the complex numbers
		FFT fft = new FFT(sampleSize , -1);
		double [] amplitudesInterlaced = new double[sampleSize*2];
		for(int i=0;i<sampleSize;i++){
			amplitudesInterlaced[2*i]=amplitudes[i];
			amplitudesInterlaced[2*i+1]=0.0;
		}
		fft.transform(amplitudesInterlaced);
		// end call the fft and transform the complex numbers

		double[] complexNumbers = amplitudesInterlaced;

		// even indexes (0,2,4,6,...) are real parts
		// odd indexes (1,3,5,7,...) are img parts
		int indexSize = sampleSize ;

		// FFT produces a transformed pair of arrays where the first half of the
		// values represent positive frequency components and the second half
		// represents negative frequency components.
		// we obtain the abs value of the complex numbers
		int positiveSize = indexSize / 2;

		double[] mag = new double[positiveSize];
		for (int i = 0; i < indexSize; i += 2) {
			mag[i / 2] = Math.sqrt(complexNumbers[i] * complexNumbers[i] + complexNumbers[i + 1] * complexNumbers[i + 1]);
		}
		
		return mag;
		

	}

}
