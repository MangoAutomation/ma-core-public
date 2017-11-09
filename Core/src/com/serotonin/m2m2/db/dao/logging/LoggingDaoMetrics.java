/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao.logging;

import java.util.List;

import com.serotonin.log.LogStopWatch;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.console.LogEvent;

/**
 * @author Terry Packer
 *
 */
public class LoggingDaoMetrics implements LoggingDao{

	private final LoggingDao dao;
    private final long metricsThreshold;
	
	/**
	 * @param loggingDaoSQL
	 */
	public LoggingDaoMetrics(LoggingDao loggingDao) {
		this.dao = loggingDao;
        this.metricsThreshold = Common.envProps.getLong("db.metricsThreshold", 0L);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.logging.LoggingDao#log(com.serotonin.m2m2.vo.logging.LogEventVO)
	 */
	@Override
	public void log(LogEvent event) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		dao.log(event);
    	LogStopWatch.stop("log(event)", this.metricsThreshold);		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.logging.LoggingDao#getLogs(long, long)
	 */
	@Override
	public List<LogEvent> getLogs(long from, long to, int limit) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		List<LogEvent> values = dao.getLogs(from, to, limit);
    	LogStopWatch.stop("getLogs(from, to) (" + from + ", " +to + "){" + values.size() +"}", this.metricsThreshold);
    	return values;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.logging.LoggingDao#getLogs(long, long, int)
	 */
	@Override
	public List<LogEvent> getLogs(long from, long to, int level, int limit) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		List<LogEvent> values = dao.getLogs(from, to, level, limit);
    	LogStopWatch.stop("getLogs(from, to, level) (" + from + ", " +to + ", " + level + "){" + values.size() +"}", this.metricsThreshold);
    	return values;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.logging.LoggingDao#getLogs(long, long, int)
	 */
	@Override
	public List<LogEvent> getLogs(long from, long to, List<Integer> levels, int limit) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		
		String sqlIn = "[";
		for(int i=0; i<levels.size(); i++){
			sqlIn += levels.get(i);
			if(i < levels.size())
				sqlIn += ",";
		}
		sqlIn += "]";
		
		List<LogEvent> values = dao.getLogs(from, to, levels, limit);
    	LogStopWatch.stop("getLogs(from, to, levels) (" + from + ", " +to + ", " + sqlIn + "){" + values.size() +"}", this.metricsThreshold);
    	return values;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.logging.LoggingDao#dateRangeCount(int, long, long)
	 */
	@Override
	public long dateRangeCount(long from, long to, int level) {
		LogStopWatch LogStopWatch = new LogStopWatch();
		long count = dao.dateRangeCount(from, to, level);
    	LogStopWatch.stop("dateRangeCount(from, to, level) (" + from + ", " +to + ", " + level + "){" + count +"}", this.metricsThreshold);
    	return count;

	}
	
	
	
}
