/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.console;

import com.serotonin.m2m2.Common;

/**
 * 
 * Class to gracefully map logging levels 
 * across logging implementations
 * 
 * @author Terry Packer
 *
 */
public enum LogLevel {

	TRACE(10, "TRACE", "common.logging.trace"),
	DEBUG(20, "DEBUG", "common.logging.debug"),
	INFO(30, "INFO", "common.logging.info"),
	WARN(40, "WARN", "common.logging.warn"),
	ERROR(50, "ERROR", "common.logging.error"),
	FATAL(60, "FATAL", "common.logging.fatal"),
	UNKNOWN(100, "UNKNOWN", "common.logging.unknown"),
	ALL(200, "ALL", "common.logging.all"),
	OFF(0, "OFF", "common.logging.off");
	
	private int level;
	private String code;
	private String key;
	
	private LogLevel(int level, String code, String key){
		this.level = level;
		this.code = code;
		this.key = key;
	}
	
	/**
	 * Get the translated text
	 * @return
	 */
	public String getDescription(){
		return Common.translate(key);
	}
	
	/**
	 * Get the translation key
	 * @return
	 */
	public String getKey(){
		return this.key;
	}
	
	/**
	 * Get the log code (TRACE, DEBUG, INFO, WARN, ERROR, FATAL, UNKNOWN, ALL)
	 * @return
	 */
	public String getCode(){
		return this.code;
	}
	/**
	 * Basic mapping for use in Log4j
	 * @param level
	 * @return
	 */
	public static LogLevel createFromLog4jLevel(int level){
		switch(level){
		case 0:
			return OFF;
		case 600:
			return TRACE;
		case 500:
			return DEBUG;
		case 400:
			return INFO;
		case 300:
			return WARN;
		case 200:
			return ERROR;
		case 100: 
			return FATAL;
		case Integer.MAX_VALUE:
			return ALL;
		default:
			return UNKNOWN;
		}
	}
	
	/**
	 * Create a LogLevel using a known integer level
	 * @param level
	 * @return
	 */
	public static LogLevel createFromLevel(int level){
		
		for(LogLevel logLevel : LogLevel.values()){
			if(logLevel.getLevel() == level)
				return logLevel;
		}
		
		return UNKNOWN;
	}
	
	public int getLevel(){
		return level;
	}

	/**
	 * @return
	 */
	public int getLog4jLevel() {
		switch(this){
		case OFF:
			return 0;
		case TRACE:
			return 600;
		case DEBUG:
			return 500;
		case INFO:
			return 400;
		case WARN:
			return 300;
		case ERROR:
			return 200;
		case FATAL: 
			return 100;
		case ALL:
			return Integer.MAX_VALUE;
		default:
			return 100;
		}
	}
	
	
}
