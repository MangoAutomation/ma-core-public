/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import java.util.List;

import com.serotonin.m2m2.db.dao.AbstractBasicDao;
import com.serotonin.m2m2.vo.AbstractBasicVO;
import com.serotonin.m2m2.web.mvc.rest.v1.MangoVoRestController;

import net.jazdw.rql.parser.ASTNode;

/**
 * @author Terry Packer
 *
 */
public class FilteredQueryStream<VO extends AbstractBasicVO, MODEL, DAO extends AbstractBasicDao<VO>> extends QueryStream<VO, MODEL, DAO>{

	/**
	 * @param dao
	 * @param controller
	 * @param root
	 * @param queryCallback
	 */
	public FilteredQueryStream(DAO dao, MangoVoRestController<VO, MODEL, DAO> controller, ASTNode root,
			FilteredQueryStreamCallback<VO> queryCallback) {
		super(dao, controller, root, queryCallback);
	}

	/**
	 * Setup the Query without limit or count
	 */
	@Override
	public void setupQuery(){
		this.results = this.dao.createQuery(root, queryCallback, null, controller.getModelMap(), controller.getAppenders(), false);
		List<Object> args = this.results.getLimitOffsetArgs();
		if((args != null)&&(args.size() > 0)){
			FilteredQueryStreamCallback<VO> callback = (FilteredQueryStreamCallback<VO>)this.queryCallback;
			callback.setLimit((Integer)args.get(0));
			if(args.size() == 2)
				callback.setOffset((Integer)args.get(1));
		}
	}
}
