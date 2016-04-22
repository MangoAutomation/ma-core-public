/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import java.io.IOException;

import net.jazdw.rql.parser.ASTNode;

import com.fasterxml.jackson.core.JsonGenerator;
import com.serotonin.m2m2.db.dao.AbstractBasicDao;
import com.serotonin.m2m2.vo.AbstractBasicVO;
import com.serotonin.m2m2.web.mvc.rest.v1.MangoVoRestController;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPojoWriter;

/**
 * @author Terry Packer
 *
 */
public class PageQueryStream<VO extends AbstractBasicVO, MODEL, DAO extends AbstractBasicDao<VO>> extends QueryStream<VO,MODEL, DAO> implements QueryDataPageStream<VO>{

	protected QueryStreamCallback<Long> countCallback;

	/**
	 * 
	 * @param dao
	 * @param controller
	 * @param query
	 * @param queryCallback
	 */
	public PageQueryStream(DAO dao, MangoVoRestController<VO, MODEL, DAO> controller, ASTNode node, QueryStreamCallback<VO> queryCallback) {
		super(dao, controller, node, queryCallback);
		this.countCallback = new QueryStreamCallback<Long>();
	}


	/**
	 * Setup the Query
	 */
	@Override
	public void setupQuery(){
		this.results = this.dao.createQuery(root, queryCallback, countCallback, controller.getModelMap(), controller.getAppenders());
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.JsonDataPageStream#streamCount(com.fasterxml.jackson.core.JsonGenerator)
	 */
	@Override
	public void streamCount(JsonGenerator jgen) throws IOException {
		this.countCallback.setJsonGenerator(jgen);
		this.results.count();
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.QueryDataPageStream#streamCount(com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPojoWriter)
	 */
	@Override
	public void streamCount(CSVPojoWriter<Long> writer) throws IOException {		
		//Currently doing nothing as this would create a weird CSV
	}

	
}
