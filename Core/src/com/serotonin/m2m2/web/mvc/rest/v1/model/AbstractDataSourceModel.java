/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.web.mvc.rest.v1.exception.RestValidationFailedException;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
import com.serotonin.m2m2.web.mvc.rest.v1.model.dataSource.PurgeSettings;
import com.serotonin.m2m2.web.mvc.rest.v1.model.time.TimePeriodType;

/**
 * @author Terry Packer
 *
 */
@JsonPropertyOrder({"xid", "name", "enabled"})
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class AbstractDataSourceModel<T extends DataSourceVO<?>> extends AbstractActionVoModel<DataSourceVO<?>>{
	
	protected DataSourceVO<T> data;
	
	
	/**
	 * @param data
	 */
	public AbstractDataSourceModel(DataSourceVO<T> data) {
		super(data);
		this.data = data;
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
	
	
	@JsonGetter(value="purgeSettings")
	public PurgeSettings getPurgeSettings(){
		return new PurgeSettings(this.data.isPurgeOverride(), this.data.getPurgePeriod(), this.data.getPurgeType());
	}
	
	@JsonSetter("purgeSettings")
	public void setPurgeSettings(PurgeSettings settings){
		this.data.setPurgeOverride(settings.isOverride());
		this.data.setPurgePeriod(settings.getFrequency().getPeriods());
		this.data.setPurgeType(TimePeriodType.convertFrom(settings.getFrequency().getType()));
	}
	
	@JsonIgnore
	public DataSourceVO<T> getData(){
		return this.data;
	}
}
