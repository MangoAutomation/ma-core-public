/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.util.List;

import com.serotonin.log.LogStopWatch;
import com.serotonin.m2m2.db.dao.AbstractBasicDao;
import com.serotonin.m2m2.vo.AbstractBasicVO;

/**
 * @author Terry Packer
 *
 */
public class BaseSqlQuery<T  extends AbstractBasicVO> {
	protected AbstractBasicDao<T> dao;
	
	protected String selectSql;
	protected List<Object> selectArgs;
	
	protected String countSql;
	protected List<Object> countArgs;
	
	protected List<Object> limitOffsetArgs;
	
	protected boolean useMetrics;
    protected long metricsThreshold;

	public BaseSqlQuery(AbstractBasicDao<T> dao, 
			String selectSql, List<Object> selectArgs,
			String countSql, List<Object> countArgs){
		this.dao = dao;
		
		this.selectSql = selectSql;
		this.selectArgs = selectArgs;
		
		this.countSql = countSql;
		this.countArgs = countArgs;
		
		this.useMetrics = this.dao.isUseMetrics();
        this.metricsThreshold = this.dao.getMetricsThreshold();
	}
	
	/**
	 * 
	 * @param dao
	 * @param statement
	 * @param applyLimitToSelectSql - Should we apply the limit
	 */
	public BaseSqlQuery(AbstractBasicDao<T> dao, 
			SQLStatement statement){
		this.dao = dao;
		
		this.selectSql = statement.getSelectSql();
		this.selectArgs = statement.getSelectArgs();
		
		this.countSql = statement.getCountSql();
		this.countArgs = statement.getCountArgs();
		
		this.limitOffsetArgs = statement.getLimitOffsetArgs();
		
		this.useMetrics = this.dao.isUseMetrics();
        this.metricsThreshold = this.dao.getMetricsThreshold();
	}
	

	/**
	 * Execute the quer
	 * @return
	 */
	public List<T> immediateQuery(){
		LogStopWatch stopWatch = null;
        if(this.useMetrics)
        	 stopWatch = new LogStopWatch();
		
		List<T> list = this.dao.query(selectSql, selectArgs.toArray(), this.dao.getRowMapper());
		if(this.useMetrics)
			stopWatch.stop("Query: " + selectSql + " \nArgs: " + selectArgs.toString(), this.metricsThreshold);
		return list;
	}
	
	/**
	 * Execute the Count if there is one
	 */
	public long immediateCount(){
		if(countSql == null)
			return 0;

		LogStopWatch stopWatch = null;
        if(this.useMetrics)
        	 stopWatch = new LogStopWatch();

        long count = this.dao.queryForObject(countSql, countArgs.toArray(), Long.class , new Long(0));
        if(this.useMetrics)
        	stopWatch.stop("Count: " + countSql + " \nArgs: " + countArgs.toString(), this.metricsThreshold);
        return count;
	}
	
	public List<Object> getLimitOffsetArgs(){
		return this.limitOffsetArgs;
	}
	
	public String toString(){
		String out = "Streamable Query: " + selectSql + " \nArgs: " + selectArgs.toString();
		return out += "\nCount: " + countSql + " \nArgs: " + countArgs.toString();
	}
}
