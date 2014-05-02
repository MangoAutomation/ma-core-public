/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.console;

import org.apache.log4j.AsyncAppender;
import org.apache.log4j.spi.LoggingEvent;

import com.serotonin.m2m2.db.dao.logging.LoggingDao;

/**
 * TODO This is NOT running Asynchronously!!!!!
 * 
 * @author Terry Packer
 *
 */
public class DatabaseLogAppender extends AsyncAppender{
	
	private final LoggingDao dao;
	
	public DatabaseLogAppender(LoggingDao dao){
		this.dao = dao;
	}

	/* (non-Javadoc)
	 * @see org.apache.log4j.Appender#close()
	 */
	@Override
	public void close() {
		//Don't need to close the dao down
	}

	/* (non-Javadoc)
	 * @see org.apache.log4j.Appender#requiresLayout()
	 */
	@Override
	public boolean requiresLayout() {
		return false; //Don't need it
	}

	/* (non-Javadoc)
	 * @see org.apache.log4j.AppenderSkeleton#append(org.apache.log4j.spi.LoggingEvent)
	 */
	@Override
	public void append(LoggingEvent event) {
		
		String message = event.getMessage().toString();
		String locationInfo = null;
		if(event.locationInformationExists()){
			StringBuilder locationBuilder = new StringBuilder();
			locationBuilder.append(event.getLocationInformation().getClassName());
			locationBuilder.append(".");
			locationBuilder.append(event.getLocationInformation().getMethodName());
			locationBuilder.append(":");
			locationBuilder.append(event.getLocationInformation().getLineNumber());
			locationInfo = locationBuilder.toString();
		}
		
		this.dao.log(new LogEvent(event.getTimeStamp(), event.getLevel().toInt(), message, locationInfo));
		
	}

}
