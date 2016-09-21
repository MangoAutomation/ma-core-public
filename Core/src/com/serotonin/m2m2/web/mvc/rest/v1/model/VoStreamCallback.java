/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import java.io.IOException;

import com.serotonin.m2m2.db.dao.AbstractBasicDao;
import com.serotonin.m2m2.web.mvc.rest.v1.MangoVoRestController;

/**
 * @author Terry Packer
 *
 */
public class VoStreamCallback<VO, MODEL, DAO extends AbstractBasicDao<VO>> extends QueryStreamCallback<VO> {

	protected MangoVoRestController<VO, MODEL, DAO> controller;
	protected long count;
	
	/**
	 * 
	 * @param controller
	 */
	public VoStreamCallback(MangoVoRestController<VO, MODEL, DAO> controller){
		this.controller = controller;
	}
	
	/**
	 * Do the work of writing the VO
	 * @param vo
	 * @throws IOException
	 */
	@Override
	protected void writeJson(VO vo) throws IOException{
		if(canWrite(vo)){
			MODEL model = this.controller.createModel(vo);
			this.jgen.writeObject(model);
			this.count++;
		}
	}
	@Override
	protected void writeCsv(VO vo) throws IOException{
		if(canWrite(vo)){
			MODEL model = this.controller.createModel(vo);
			this.csvWriter.writeNext(model);
			this.count++;
		}
	}

	/**
	 * Can we write this VO (permissions lacking etc.)
	 * @param vo
	 * @return
	 */
	protected boolean canWrite(VO vo){
		return true;
	}
	
	public long getWrittenCount(){
		return count;
	}
}
