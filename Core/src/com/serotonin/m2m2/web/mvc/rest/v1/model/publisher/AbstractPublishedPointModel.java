/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.publisher;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVColumnGetter;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVColumnSetter;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractRestModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractPublishedPointModel<T extends PublishedPointVO> extends AbstractRestModel<T> {

	
	/**
	 * @param data
	 */
	public AbstractPublishedPointModel(T data) {
		super(data);
	}
	
	//For CSV Models to define the type
	@ApiModelProperty(value = "Model Type Definition", required = false)
	@CSVColumnGetter(order=0, header="modelType")
	public String getModelType(){
		return "PUB-POINT-" + getPublisherTypeName();
	}
	
	@CSVColumnSetter(order=0, header="modelType")
	public void setModelType(String typeName){ }
	
	@CSVColumnGetter(order=1, header="dataPointId")
	public int getDataPointId(){
		return this.data.getDataPointId();
	}
	@CSVColumnSetter(order=1, header="dataPointId")
	public void setDataPointId(int dataPointId){
		this.data.setDataPointId(dataPointId);
	}
	
	/**
	 * Get the sub type name for the model.  
	 * Published Points are based on the model type name
	 * PUB-POINT-{Publisher Type Name}
	 * @return
	 */
	@JsonIgnore
	public abstract String getPublisherTypeName();
}
