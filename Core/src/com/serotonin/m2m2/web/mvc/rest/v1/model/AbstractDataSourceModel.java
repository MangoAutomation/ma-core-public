/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.web.mvc.rest.v1.exception.RestValidationFailedException;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractDataSourceModel<T extends DataSourceVO<?>> extends AbstractRestModel<DataSourceVO<T>>{

	/**
	 * @param data
	 */
	public AbstractDataSourceModel(DataSourceVO<T> data) {
		super(data);
	}

	/*
	 * (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractRestModel#validate(com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult)
	 */
	@Override
	public void validate(RestProcessResult<?> result) throws RestValidationFailedException {
		ProcessResult validation = new ProcessResult();
		this.data.validate(validation);
		
		if(validation.getHasMessages()){
			result.addValidationMessages(validation);
			throw new RestValidationFailedException(this, result);
		}
	}
	
	
	@JsonGetter(value="purgeOverride")
	public boolean getPurgeOverride(){
		return this.data.isPurgeOverride();
	}
	@JsonSetter(value="purgeOverride")
	public void setPurgeOverride(boolean override){
		this.data.setPurgeOverride(override);
	}
	
	@JsonGetter(value="xid")
	public String getXid(){
		return this.data.getXid();
	}
	@JsonSetter(value="xid")
	public void setXid(String xid){
		this.data.setXid(xid);
	}

}
