/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.script;

import com.serotonin.m2m2.view.stats.AnalogStatistics;

/**
 * Wrapper to allow use of Java Primitives from Statistics
 * in JavaScript.  This is Required for the Nashorn Javascript Engine
 * because it doesn't automatically convert java.lang.xxx to Javascript Number
 * 
 * @author Terry Packer
 *
 */
public class AnalogStatisticsWrapper {
	
	private AnalogStatistics statistics;
	public AnalogStatisticsWrapper(AnalogStatistics statistics){
		this.statistics = statistics;
	}
	

    public long getPeriodStartTime() {
        return statistics.getPeriodStartTime();
    }

    public long getPeriodEndTime() {
    	return statistics.getPeriodEndTime();
    }

    public double getMinimumValue() {
    	if(statistics.getMinimumValue() == null)
    		return 0;
    	else
    		return (double)statistics.getMinimumValue();
    }

    public long getMinimumTime() {
    	if(statistics.getMinimumTime() == null)
    		return -1;
    	else 
    		return (long)statistics.getMinimumTime();
    }

    public double getMaximumValue() {
    	if(statistics.getMaximumValue() == null)
    		return 0;
    	else
    		return (double)statistics.getMaximumValue();
    }

    public long getMaximumTime() {
    	if(statistics.getMaximumTime() == null)
    		return -1;
    	else 
    		return (long)statistics.getMaximumTime();
    }

    public double getAverage() {
    	if(statistics.getAverage() == null)
    		return 0;
    	else
    		return (double)statistics.getAverage();
    }
    
    public double getIntegral() {
    	if(statistics.getIntegral() == null)
    		return 0;
    	else
    		return (double)statistics.getIntegral();
    }

    public double getSum() {
        return statistics.getSum();
    }

    public double getFirstValue() {
    	if(statistics.getFirstValue() == null)
    		return 0;
    	else
    		return (double)statistics.getFirstValue();
    }

    public long getFirstTime() {
    	if(statistics.getFirstTime() == null)
    		return -1;
    	else 
    		return (long)statistics.getFirstTime();
    }

    public double getLastValue() {
    	if(statistics.getLastValue() == null)
    		return 0;
    	else
    		return (double)statistics.getLastValue();
    }

    public long getLastTime() {
    	if(statistics.getLastTime() == null)
    		return -1;
    	else 
    		return (long)statistics.getLastTime();
    }

    public int getCount() {
        return statistics.getCount();
    }

    public double getDelta(){
    	return statistics.getDelta();
    }
    
    public String getHelp() {
        return statistics.toString();
    }

    @Override
    public String toString() {
    	return statistics.toString();
    }
}
