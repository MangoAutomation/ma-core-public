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
	ALL(200, "ALL", "common.logging.all");
	
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
		case 5000:
			return TRACE;
		case 10000:
			return DEBUG;
		case 20000:
			return INFO;
		case 30000:
			return WARN;
		case 40000:
			return ERROR;
		case 50000: 
			return FATAL;
		case -2147483648:
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
		case TRACE:
			return 5000;
		case DEBUG:
			return 10000;
		case INFO:
			return 20000;
		case WARN:
			return 30000;
		case ERROR:
			return 40000;
		case FATAL: 
			return 50000;
		case ALL:
			return -2147483648;
		default:
			return 10000;
		}
	}
	
	
}
