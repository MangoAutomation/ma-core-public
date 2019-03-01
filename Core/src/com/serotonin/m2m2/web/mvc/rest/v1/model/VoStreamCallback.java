/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import java.io.IOException;

import com.serotonin.m2m2.db.dao.AbstractBasicDao;
import com.serotonin.m2m2.vo.AbstractBasicVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.rest.IMangoVoRestController;

/**
 * @author Terry Packer
 *
 */
public class VoStreamCallback<VO extends AbstractBasicVO, MODEL, DAO extends AbstractBasicDao<VO>> extends QueryStreamCallback<VO> {

	protected IMangoVoRestController<VO, MODEL, DAO> controller;
	protected final User user;
	/**
	 * 
	 * @param controller
	 */
	public VoStreamCallback(IMangoVoRestController<VO, MODEL, DAO> controller, User user){
		this.controller = controller;
		this.user = user;
	}
	
	/**
	 * Do the work of writing the VO
	 * @param vo
	 * @throws IOException
	 */
	@Override
	protected void writeJson(VO vo) throws IOException{
		MODEL model = this.controller.createModel(vo, user);
		this.jgen.writeObject(model);
	}
	@Override
	protected void writeCsv(VO vo) throws IOException{
		MODEL model = this.controller.createModel(vo, user);
		this.csvWriter.writeNext(model);
	}
}
