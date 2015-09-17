/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.view.quantize2;

import org.jtransforms.fft.DoubleFFT_1D;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;

/**
 * 
 * Generate an FFT into values
 * 
 * @author Terry Packer
 *
 */
public class FftGenerator {

	private DoubleFFT_1D fft;
	private double[] values;
	private double averageSamplePeriodMs;
	private int position;
	private long lastTimestamp;
	
	public FftGenerator(long size){
		this.fft = new DoubleFFT_1D(size);
		
		//If this happens we can convert this to use a LargeDoubleArray instead of normal arrays
		if(size > Integer.MAX_VALUE)
			throw new ShouldNeverHappenException("Too much data to fft!");
		
		this.values = new double[(int)size];
		this.position = 0;
		this.averageSamplePeriodMs = 0;
		this.lastTimestamp = 0;
	}
	
	public void data(PointValueTime value){
		this.values[this.position] = value.getDoubleValue();
		this.computeMovingAverage(value.getTime());
		this.position++;
	}
	
	public void done(PointValueTime value){
		if(value != null){
			this.values[this.position] = value.getDoubleValue();
			this.computeMovingAverage(value.getTime());
		}
		
		this.fft();
	}
	
	/**
	 * Compute the FFT of the values
	 */
	public void fft(){
		this.fft.realForward(values);
	}
	
	/**
	 * Compute the IFFT of the values
	 */
	public void ifft(){
		this.fft.complexInverse(values, true);
	}
	
	/**
	 * Return the pre FFT values if not done, else return FFT values
	 * @return
	 */
	public double[] getValues(){
		return values;
	}

	/**
	 * @return
	 */
	public double getAverageSamplePeriodMs() {
		return averageSamplePeriodMs;
	}
	
	
	private void computeMovingAverage(long currentTimestamp){
		if(this.lastTimestamp != 0){
			//Compute this period
			long period = currentTimestamp - this.lastTimestamp;
			double count = (double)this.position;
			this.averageSamplePeriodMs -= this.averageSamplePeriodMs / count;
			this.averageSamplePeriodMs += (double)period / count;
			
		}
		this.lastTimestamp = currentTimestamp;
	}
}
