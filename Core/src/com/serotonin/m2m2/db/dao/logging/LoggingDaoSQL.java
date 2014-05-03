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
public class LoggingDaoSQL implements LoggingDao{

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.logging.LoggingDao#log(com.serotonin.m2m2.vo.logging.LogEventVO)
	 */
	@Override
	public void log(LogEvent event) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.logging.LoggingDao#getLogs(long, long)
	 */
	@Override
	public List<LogEvent> getLogs(long from, long to, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.logging.LoggingDao#getLogs(long, long, int)
	 */
	@Override
	public List<LogEvent> getLogs(long from, long to, int level, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.logging.LoggingDao#getLogs(long, long, java.util.List)
	 */
	@Override
	public List<LogEvent> getLogs(long from, long to, List<Integer> levels, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.logging.LoggingDao#dateRangeCount(int, long, long)
	 */
	@Override
	public long dateRangeCount(long from, long to, int level) {
		// TODO Auto-generated method stub
		return 0;
	}

}
