/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.vo.AbstractVO;

/**
 * @author Terry Packer
 *
 */
public class BaseSqlQuery<VO extends AbstractVO<VO>> {
	private static final Log LOG = LogFactory.getLog(BaseSqlQuery.class);

	protected AbstractDao<VO> dao;
	
	protected String selectSql;
	protected List<Object> selectArgs;
	
	protected String countSql;
	protected List<Object> countArgs;

	public BaseSqlQuery(AbstractDao<VO> dao, 
			String selectSql, List<Object> selectArgs,
			String countSql, List<Object> countArgs){
		this.dao = dao;
		
		this.selectSql = selectSql;
		this.selectArgs = selectArgs;
		
		this.countSql = countSql;
		this.countArgs = countArgs;
	}
	
	public BaseSqlQuery(AbstractDao<VO> dao, 
			SQLStatement statement){
		this.dao = dao;
		
		this.selectSql = statement.getSelectSql();
		this.selectArgs = statement.getSelectArgs();
		
		this.countSql = statement.getCountSql();
		this.countArgs = statement.getCountArgs();
	}
	

	/**
	 * Execute the quer
	 * @return
	 */
	public List<VO> immediateQuery(){
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
	
}
