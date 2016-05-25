/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.publisher;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;
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
	
	@CSVColumnGetter(order=1, header="dataPointXid")
	public String getDataPointXid(){
		//TODO Performance is poor here for a large list of points...
		DataPointVO vo = DataPointDao.instance.get(this.data.getDataPointId());
		if(vo != null)
			return vo.getXid();
		else
			return null;
	}
	@CSVColumnSetter(order=1, header="dataPointXid")
	public void setDataPointXid(String xid){
		DataPointVO vo = DataPointDao.instance.getByXid(xid);
		if(vo != null)
			this.data.setDataPointId(vo.getId());
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
