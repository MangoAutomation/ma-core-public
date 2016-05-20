/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao.logging;

import java.util.List;

import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;

import com.serotonin.m2m2.rt.console.LogEvent;

/**
 * @author Terry Packer
 *
 */
public class LoggingDaoMetrics implements LoggingDao{

	private final LoggingDao dao;
	
	/**
	 * @param loggingDaoSQL
	 */
	public LoggingDaoMetrics(LoggingDao loggingDao) {
		this.dao = loggingDao;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.logging.LoggingDao#log(com.serotonin.m2m2.vo.logging.LogEventVO)
	 */
	@Override
	public void log(LogEvent event) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		dao.log(event);
    	stopWatch.stop("log(event)");		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.logging.LoggingDao#getLogs(long, long)
	 */
	@Override
	public List<LogEvent> getLogs(long from, long to, int limit) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		List<LogEvent> values = dao.getLogs(from, to, limit);
    	stopWatch.stop("getLogs(from, to) (" + from + ", " +to + "){" + values.size() +"}");
    	return values;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.logging.LoggingDao#getLogs(long, long, int)
	 */
	@Override
	public List<LogEvent> getLogs(long from, long to, int level, int limit) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		List<LogEvent> values = dao.getLogs(from, to, level, limit);
    	stopWatch.stop("getLogs(from, to, level) (" + from + ", " +to + ", " + level + "){" + values.size() +"}");
    	return values;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.logging.LoggingDao#getLogs(long, long, int)
	 */
	@Override
	public List<LogEvent> getLogs(long from, long to, List<Integer> levels, int limit) {
		StopWatch stopWatch = new Log4JStopWatch();
		
		String sqlIn = "[";
		for(int i=0; i<levels.size(); i++){
			sqlIn += levels.get(i);
			if(i < levels.size())
				sqlIn += ",";
		}
		sqlIn += "]";
		
		stopWatch.start();
		List<LogEvent> values = dao.getLogs(from, to, levels, limit);
    	stopWatch.stop("getLogs(from, to, levels) (" + from + ", " +to + ", " + sqlIn + "){" + values.size() +"}");
    	return values;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.logging.LoggingDao#dateRangeCount(int, long, long)
	 */
	@Override
	public long dateRangeCount(long from, long to, int level) {
		StopWatch stopWatch = new Log4JStopWatch();
		stopWatch.start();
		long count = dao.dateRangeCount(from, to, level);
    	stopWatch.stop("dateRangeCount(from, to, level) (" + from + ", " +to + ", " + level + "){" + count +"}");
    	return count;

	}
	
	
	
}
