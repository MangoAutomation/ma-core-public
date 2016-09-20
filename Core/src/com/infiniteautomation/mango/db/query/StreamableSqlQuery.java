/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.SingleColumnRowMapper;

import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.db.dao.AbstractBasicDao;

/**
 * @author Terry Packer
 *
 */
public class StreamableSqlQuery<T> extends BaseSqlQuery<T>{
	
	private static final Log LOG = LogFactory.getLog(StreamableSqlQuery.class);
	
	protected MappedRowCallback<T> selectCallback;
	protected MappedRowCallback<Long> countCallback;
	
	

	/**
	 * @param dao
	 * @param selectSql
	 * @param selectCallback
	 * @param countSql
	 * @param countCallback
	 * @param selectArgs
	 */
	public StreamableSqlQuery(AbstractBasicDao<T> dao, 
			SQLStatement statement, MappedRowCallback<T> selectCallback, MappedRowCallback<Long> countCallback) {
		super(dao, statement);
		
		this.selectCallback = selectCallback;
		this.countCallback = countCallback;
	}
	
	
	/**
	 * @param dao
	 * @param selectSql
	 * @param selectCallback
	 * @param countSql
	 * @param countCallback
	 * @param selectArgs
	 */
	public StreamableSqlQuery(AbstractBasicDao<T> dao, 
			String selectSql, MappedRowCallback<T> selectCallback, List<Object> selectArgs,
			String countSql, MappedRowCallback<Long> countCallback, List<Object> countArgs) {
		super(dao, selectSql, selectArgs, countSql, countArgs);
		
		this.selectCallback = selectCallback;
		this.countCallback = countCallback;
	}

	/**
	 * Execute the Query
	 */
	public void query(){
        //Report the query in the log
        if(LOG.isDebugEnabled()){
        	LOG.debug("Streamable Query: " + selectSql + " \nArgs: " + selectArgs.toString());
        }
		this.dao.query(selectSql, selectArgs.toArray(), this.dao.getRowMapper(), selectCallback);
	}
	
	/**
	 * Execute the Count if there is one
	 */
	public void count(){
		if(countSql == null)
			return;
        //Report the query in the log
        if(LOG.isDebugEnabled()){
        	LOG.debug("Count: " + countSql + " \nArgs: " + countArgs.toString());
        }
 
		this.dao.query(countSql, countArgs.toArray(), SingleColumnRowMapper.newInstance(Long.class), countCallback);
	}	
}
