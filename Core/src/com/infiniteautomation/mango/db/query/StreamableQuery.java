/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.simple.ParameterizedSingleColumnRowMapper;

import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.vo.AbstractVO;

/**
 * @author Terry Packer
 *
 */
public class StreamableQuery<VO extends AbstractVO<VO>> {
	
	private static final Log LOG = LogFactory.getLog(StreamableQuery.class);
	
	AbstractDao<VO> dao;
	String selectSql;
	MappedRowCallback<VO> selectCallback;
	List<Object> selectArgs;
	
	String countSql;
	MappedRowCallback<Long> countCallback;
	List<Object> countArgs;
	
	

	/**
	 * @param dao
	 * @param selectSql
	 * @param selectCallback
	 * @param countSql
	 * @param countCallback
	 * @param selectArgs
	 */
	public StreamableQuery(AbstractDao<VO> dao, 
			String selectSql, MappedRowCallback<VO> selectCallback, List<Object> selectArgs,
			String countSql, MappedRowCallback<Long> countCallback, List<Object> countArgs) {
		super();
		this.dao = dao;
		
		this.selectSql = selectSql;
		this.selectCallback = selectCallback;
		this.selectArgs = selectArgs;
		
		this.countSql = countSql;
		this.countCallback = countCallback;
		this.countArgs = countArgs;
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
        	LOG.debug("Count: " + countSql + " \nArgs: " + selectArgs.toString());
        }
 
		this.dao.query(countSql, countArgs.toArray(), ParameterizedSingleColumnRowMapper.newInstance(Long.class), countCallback);
		
	}
}
