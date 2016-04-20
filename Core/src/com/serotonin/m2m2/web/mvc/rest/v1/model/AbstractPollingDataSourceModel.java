/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.serotonin.m2m2.vo.dataSource.PollingDataSourceVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.time.TimePeriod;
import com.serotonin.m2m2.web.mvc.rest.v1.model.time.TimePeriodType;

/**
 * @author Terry Packer
 *
 */
public class AbstractPollingDataSourceModel<T extends PollingDataSourceVO<T>> extends AbstractDataSourceModel<PollingDataSourceVO<T>>{

	/**
	 * @param data
	 */
	public AbstractPollingDataSourceModel(PollingDataSourceVO<T> data) {
		super(data);
	}

	@JsonGetter(value="pollPeriod")
	public TimePeriod getPollPeriod(){
		return new TimePeriod(this.data.getUpdatePeriods(), 
				TimePeriodType.convertTo(this.data.getUpdatePeriodType()));
	}

	@JsonSetter(value="pollPeriod")
	public void setPollPeriod(TimePeriod pollPeriod){
		this.data.setUpdatePeriods(pollPeriod.getPeriods());
		this.data.setUpdatePeriodType(TimePeriodType.convertFrom(pollPeriod.getType()));
	}

	@JsonGetter(value="quantize")
	public boolean isQuantize(){
		return this.data.isQuantize();
	}
	@JsonSetter(value="quantize")
	public void setQuantize(boolean quantize){
		this.data.setQuantize(quantize);
	}
	@JsonGetter(value="cronPattern")
	public String getCronPattern(){
		return this.data.getCronPattern();
	}
	@JsonSetter(value="cronPattern")
	public void setCronPattern(String cronPattern){
		this.data.setCronPattern(cronPattern);
	}

	
}
