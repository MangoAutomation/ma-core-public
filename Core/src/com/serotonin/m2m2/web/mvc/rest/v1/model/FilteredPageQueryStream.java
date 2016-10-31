/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.serotonin.m2m2.db.dao.AbstractBasicDao;
import com.serotonin.m2m2.vo.AbstractBasicVO;
import com.serotonin.m2m2.web.mvc.rest.v1.MangoVoRestController;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPojoWriter;

import net.jazdw.rql.parser.ASTNode;

/**
 * @author Terry Packer
 *
 */
public class FilteredPageQueryStream<VO extends AbstractBasicVO, MODEL, DAO extends AbstractBasicDao<VO>> extends QueryStream<VO, MODEL, DAO> implements QueryDataPageStream<VO>{

	protected CountQueryStreamCallback countCallback;
	
	/**
	 * @param dao
	 * @param controller
	 * @param node
	 * @param queryCallback
	 */
	public FilteredPageQueryStream(DAO dao, MangoVoRestController<VO, MODEL, DAO> controller, ASTNode node,
			FilteredQueryStreamCallback<VO> queryCallback) {
		super(dao, controller, node, queryCallback);
		this.countCallback = new CountQueryStreamCallback();
	}

	/**
	 * Setup the Query without limit or count
	 */
	@Override
	public void setupQuery(){
		this.results = this.dao.createQuery(root, queryCallback, countCallback, controller.getModelMap(), controller.getAppenders(), false);
		List<Object> args = this.results.getLimitOffsetArgs();
		if((args != null)&&(args.size() > 0)){
			FilteredQueryStreamCallback<VO> callback = (FilteredQueryStreamCallback<VO>)this.queryCallback;
			callback.setLimit((Integer)args.get(0));
			if(args.size() == 2)
				callback.setOffset((Integer)args.get(1));
		}
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.JsonDataPageStream#streamCount(com.fasterxml.jackson.core.JsonGenerator)
	 */
	@Override
	public void streamCount(JsonGenerator jgen) throws IOException {
		this.countCallback.setJsonGenerator(jgen);
		//Ensure the count callback knows how many were filtered from the request
		// outside of the query
		this.countCallback.setFilteredCount(((FilteredQueryStreamCallback<VO>)this.queryCallback).getFilteredCount());
		this.results.count();
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.QueryDataPageStream#streamCount(com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPojoWriter)
	 */
	@Override
	public void streamCount(CSVPojoWriter<Long> writer) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
}
