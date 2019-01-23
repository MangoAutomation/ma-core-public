/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.dataSource;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.serotonin.m2m2.vo.dataSource.PollingDataSourceVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.time.TimePeriod;
import com.serotonin.m2m2.web.mvc.rest.v1.model.time.TimePeriodType;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractPollingDataSourceModel<T extends PollingDataSourceVO<T>> extends AbstractDataSourceModel<T> {

    public AbstractPollingDataSourceModel() {
        super();
    }
    
    public AbstractPollingDataSourceModel(T data) {
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
    
    @JsonGetter()
    public boolean isQuantize() {
        return this.data.isQuantize();
    }
    
    @JsonGetter()
    public void setQuantize(boolean quantize){
        this.data.setQuantize(quantize);
    }

}
