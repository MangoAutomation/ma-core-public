/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;

import com.serotonin.m2m2.db.dao.AbstractBasicDao;
import com.serotonin.m2m2.vo.AbstractBasicVO;

/**
 * @author Terry Packer
 *
 */
public class StreamableSqlQuery<T  extends AbstractBasicVO> extends BaseSqlQuery<T>{
	
	private static final Log LOG = LogFactory.getLog(StreamableSqlQuery.class);
	
	//Stream the results (MySQL)
	protected boolean stream;
	protected StreamableRowCallback<T> selectCallback;
	protected StreamableRowCallback<Long> countCallback;
	
	
	/**
	 * 
	 * @param dao
	 * @param stream - Stream results (MySQL)
	 * @param statement
	 * @param selectCallback
	 * @param countCallback
	 */
	public StreamableSqlQuery(AbstractBasicDao<T> dao,
			boolean stream,
			SQLStatement statement,
			StreamableRowCallback<T> selectCallback, 
			StreamableRowCallback<Long> countCallback) {
		super(dao, statement);
		this.stream = stream;
		this.selectCallback = selectCallback;
		this.countCallback = countCallback;
	}
	
	
	/**
	 * 
	 * @param dao
	 * @param stream - Stream the results (MySQL)
	 * @param selectSql
	 * @param selectCallback
	 * @param selectArgs
	 * @param countSql
	 * @param countCallback
	 * @param countArgs
	 */
	public StreamableSqlQuery(AbstractBasicDao<T> dao, boolean stream,
			String selectSql, StreamableRowCallback<T> selectCallback, List<Object> selectArgs,
			String countSql, StreamableRowCallback<Long> countCallback, List<Object> countArgs) {
		super(dao, selectSql, selectArgs, countSql, countArgs);
		this.stream = stream;
		this.selectCallback = selectCallback;
		this.countCallback = countCallback;
	}

	/**
	 * Execute the Query
	 */
	public void query() throws IOException{
        StopWatch stopWatch = null;
        if(this.useMetrics)
        	 stopWatch = new Log4JStopWatch();
        try{
	        PreparedStatement statement = this.dao.createPreparedStatement(selectSql, selectArgs, stream);
	        ResultSet rs = statement.executeQuery();
	        RowMapper<T> mapper = this.dao.getRowMapper();
	        int index = 0;
        	try{
		        while(rs.next()){
	        		this.selectCallback.row(mapper.mapRow(rs, index), index);
	        		index++;
		        }
	        }catch(Exception e){
	        	LOG.error(e.getMessage(), e);
	        	statement.cancel();
	        	throw e;
	        }finally{
	        	statement.getConnection().close();
	        	statement.close();
	        }
        }catch(Exception e){
        	LOG.error(e.getMessage() + " For Query: " + selectSql, e);
        	throw new IOException(e);
        }
        if(this.useMetrics)
        	stopWatch.stop("Streamable Query: " + selectSql + " \nArgs: " + selectArgs.toString());
	}
	
	/**
	 * Execute the Count if there is one
	 */
	public void count() throws IOException{
		if(countSql == null)
			return;
        StopWatch stopWatch = null;
        if(this.useMetrics)
        	 stopWatch = new Log4JStopWatch();
        try{
	        PreparedStatement statement = this.dao.createPreparedStatement(countSql, countArgs, stream);
	        try{
		        ResultSet rs = statement.executeQuery();
		        RowMapper<Long> mapper = SingleColumnRowMapper.newInstance(Long.class);
		        int index = 0;
		        while(rs.next()){
	        		this.countCallback.row(mapper.mapRow(rs, index), index);
	        		index++;
		        }
	        }catch(Exception e){
	        	LOG.error(e.getMessage(), e);
	        	statement.cancel();
	        	throw e;
	        }finally{
	        	statement.getConnection().close();
	        	statement.close();
	        }
        }catch(Exception e){
        	LOG.error(e.getMessage() + " For Query: " + countSql, e);
        	throw new IOException(e);
        }
        if(this.useMetrics)
        	stopWatch.stop("Count: " + countSql + " \nArgs: " + countArgs.toString());
	}	
}
