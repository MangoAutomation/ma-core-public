/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.jsondata;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.serotonin.m2m2.vo.json.JsonDataVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractRestModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractVoModel;
import com.wordnik.swagger.annotations.ApiModel;

/**
 * @author Terry Packer
 *
 */
@ApiModel(value = "JsonData", description = "Json Data Model", parent = AbstractRestModel.class)
public class JsonDataModel extends AbstractVoModel<JsonDataVO>{
	
	/**
	 * @param data
	 */
	public JsonDataModel(JsonDataVO data) {
		super(data);
	}

	public JsonDataModel() {
		super(new JsonDataVO());
	}
	
	@JsonGetter("dataPath")
    public String getDataPath() {
        return data.getDataPath();
    }
	
	@JsonSetter("dataPath")
    public void setDataPath(String dataPath) {
	    this.data.setDataPath(dataPath);
    }

	@JsonGetter("readPermission")
	public String getReadPermission(){
		return this.data.getReadPermission();
	}

	@JsonSetter("readPermission")
	public void setReadPermission(String permission){
		this.data.setReadPermission(permission);
	}
	
	@JsonGetter("editPermission")
	public String getSetPermission(){
		return this.data.getEditPermission();
	}

	@JsonSetter("editPermission")
	public void setSetPermission(String permission){
		this.data.setEditPermission(permission);
	}
	
	@JsonGetter("jsonData")
	public Object getJsonData(){
		return this.data.getJsonData();
	}

	@JsonSetter("jsonData")
	public void setJsonData(Object data){
		this.data.setJsonData(data);
	}
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractVoModel#getModelType()
	 */
	@Override
	public String getModelType() {
		return JsonDataModelDefinition.TYPE_NAME;
	}

}
