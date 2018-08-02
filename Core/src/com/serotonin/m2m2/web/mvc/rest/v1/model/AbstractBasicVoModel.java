/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import com.serotonin.m2m2.vo.AbstractBasicVO;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVColumnGetter;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVColumnSetter;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVEntity;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Terry Packer
 *
 */
@CSVEntity
public abstract class AbstractBasicVoModel<T extends AbstractBasicVO> extends AbstractRestModel<T>{

	/**
	 * @param data
	 */
	public AbstractBasicVoModel(T data) {
		super(data);
	}

	//For CSV Models to define the type
	@ApiModelProperty(value = "Model Type Definition", required = false)
	@CSVColumnGetter(order=0, header="modelType")
	public abstract String getModelType();
	
	@CSVColumnSetter(order=0, header="modelType")
	public void setModelType(String typeName){ }

	
}
