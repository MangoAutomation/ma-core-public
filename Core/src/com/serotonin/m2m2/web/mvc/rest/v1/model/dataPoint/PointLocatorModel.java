/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.vo.dataSource.PointLocatorVO;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVEntity;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractRestModel;

/**
 * @author Terry Packer
 *
 */
@CSVEntity(derived=true)
public abstract class PointLocatorModel<T extends PointLocatorVO> extends AbstractRestModel<PointLocatorVO>{

	//TODO This can probably be removed
	@JsonIgnore
	protected T data;
	
	/**
	 * @param data
	 */
	@SuppressWarnings("unchecked")
	public PointLocatorModel(PointLocatorVO data) {
		super(data);
		this.data = (T)data;
	}

    @JsonGetter("type")
    public abstract String getTypeName();
   
    @JsonSetter("type")
    public void setTypeName(String typeName){ }
    
    
    @JsonGetter("dataType")
    public String getDataType(){
    	return DataTypes.CODES.getCode(this.data.getDataTypeId());
    }
    
    @JsonSetter("dataType")
    public void setDataType(String dataTypeCode){ }
    
    
    /**
     * Can the value be set in the data source?
     */
    @JsonGetter("settable")
    public boolean isSettable(){
    	return this.data.isSettable();
    }

    /**
     * Supplemental to being settable, can the set value be relinquished?
     */
    @JsonGetter("reqliquishable")
    public boolean isRelinquishable(){
    	return this.data.isRelinquishable();
    }
    
    @Override
    public T getData(){
    	return data;
    }
    
}
