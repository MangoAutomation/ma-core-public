/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.publisher;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.infiniteautomation.mango.spring.dao.DataPointDao;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVColumnGetter;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVColumnSetter;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractRestModel;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractPublishedPointModel<T extends PublishedPointVO> extends AbstractRestModel<T> {

    @JsonIgnore
    private String missingXid; 
	
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
		return DataPointDao.instance.getXidById(this.data.getDataPointId());
	}
	@CSVColumnSetter(order=1, header="dataPointXid")
	public void setDataPointXid(String dataPointXid){
	    Integer dpid = DataPointDao.instance.getIdByXid(dataPointXid);
	    if(dpid == null) { //also the case if dataPointId is null
	        this.data.setDataPointId(Common.NEW_ID);
	        missingXid = dataPointXid;
	    } else
	        this.data.setDataPointId(dpid.intValue());
	}
	
	//Used to provide a meaningful validation message
	@JsonIgnore
	public String getMissingXid() {
	    return missingXid;
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
