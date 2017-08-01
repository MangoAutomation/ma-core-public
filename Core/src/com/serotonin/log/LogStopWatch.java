/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.log;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;

/**
 * Helper class to generate timing log output
 * 
 * @author Terry Packer
 */
public class LogStopWatch {
	static final String start = "start[";
	static final String time = "time[";
	static final String message = "message[";
	static final String close = "] ";
	
	final transient Log logger;
	final long startTime;
	
	public LogStopWatch(){
		logger = LogFactory.getLog(LogStopWatch.class);
		startTime = Common.timer.currentTimeMillis();
	}
	
	public void stop(String text){
		long end = Common.timer.currentTimeMillis();
		long duration = end - startTime;
		StringBuilder builder = new StringBuilder();
		builder.append(start);
		builder.append(startTime);
		builder.append(close);
		builder.append(time);
		builder.append(duration);
		builder.append(close);
		builder.append(message);
		builder.append(text);
		builder.append(close);
		logger.info(builder.toString());
	}
	
	public long getElapsedTime(){
		return Common.timer.currentTimeMillis() - startTime;
	}
}
