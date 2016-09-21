/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.db.dao.AbstractBasicDao;

/**
 * @author Terry Packer
 *
 */
public class BaseSqlQuery<T> {
	private static final Log LOG = LogFactory.getLog(BaseSqlQuery.class);

	protected AbstractBasicDao<T> dao;
	
	protected String selectSql;
	protected List<Object> selectArgs;
	
	protected String countSql;
	protected List<Object> countArgs;
	
	protected List<Object> limitOffsetArgs;

	public BaseSqlQuery(AbstractBasicDao<T> dao, 
			String selectSql, List<Object> selectArgs,
			String countSql, List<Object> countArgs){
		this.dao = dao;
		
		this.selectSql = selectSql;
		this.selectArgs = selectArgs;
		
		this.countSql = countSql;
		this.countArgs = countArgs;
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
	}
	

	/**
	 * Execute the quer
	 * @return
	 */
	public List<T> immediateQuery(){
		if(LOG.isDebugEnabled()){
        	LOG.debug("Query: " + selectSql + " \nArgs: " + selectArgs.toString());
        }
		return this.dao.query(selectSql, selectArgs.toArray(), this.dao.getRowMapper());
	}
	
	/**
	 * Execute the Count if there is one
	 */
	public long immediateCount(){
		if(countSql == null)
			return 0;
        //Report the query in the log
        if(LOG.isDebugEnabled()){
        	LOG.debug("Count: " + countSql + " \nArgs: " + countArgs.toString());
        }
 
        return this.dao.queryForObject(countSql, countArgs.toArray(), Long.class , new Long(0));
	}
	
	public List<Object> getLimitOffsetArgs(){
		return this.limitOffsetArgs;
	}
	
	public String toString(){
		String out = "Streamable Query: " + selectSql + " \nArgs: " + selectArgs.toString();
		return out += "\nCount: " + countSql + " \nArgs: " + countArgs.toString();
	}
}
