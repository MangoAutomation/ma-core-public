/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao.logging;

import java.util.List;

import com.serotonin.m2m2.rt.console.LogEvent;

/**
 * @author Terry Packer
 *
 */
public interface LoggingDao {

	/**
	 * @param event
	 */
	public void log(LogEvent event);
	
	public List<LogEvent> getLogs(long from, long to, int limit);
	
	public List<LogEvent> getLogs(long from, long to, int level, int limit);
	
	public List<LogEvent> getLogs(long from, long to, List<Integer> levels, int limit);

}
