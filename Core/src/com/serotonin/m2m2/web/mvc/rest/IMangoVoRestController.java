/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest;

import java.util.Map;

import com.infiniteautomation.mango.db.query.appender.SQLColumnQueryAppender;
import com.serotonin.m2m2.db.dao.AbstractBasicDao;
import com.serotonin.m2m2.vo.AbstractBasicVO;

/**
 * Common Interface for all VO Rest Controllers
 * 
 * @author Terry Packer
 */
public interface IMangoVoRestController<VO extends AbstractBasicVO, MODEL, DAO extends AbstractBasicDao<VO>> {

	/**
	 * Create the model
	 * @param vo
	 * @return
	 */
	public MODEL createModel(VO vo);

	/**
	 * Map any Model members to VO Properties
	 * @param list
	 */
	public Map<String,String> getModelMap();
	
	/**
	 * Get any query appenders mapped to properties
	 * @return
	 */
	public Map<String, SQLColumnQueryAppender> getAppenders();
	
}
