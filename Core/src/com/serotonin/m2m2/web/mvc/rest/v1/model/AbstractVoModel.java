/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonView;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVColumnGetter;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVColumnSetter;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVEntity;
import com.serotonin.m2m2.web.mvc.rest.v1.mapping.JsonViews;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestMessageLevel;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestValidationMessage;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * @author Terry Packer
 *
 */
@CSVEntity
@JsonPropertyOrder({"xid", "name"})
public abstract class AbstractVoModel<T extends AbstractVO<T>> extends AbstractRestModel<T>{
	
	//TODO Make the JSON Views work, it currently does nothing
	@ApiModelProperty(value = "Messages for validation of data", required = false)
	@JsonProperty("validationMessages")
	@JsonView(JsonViews.Validation.class) //Only show in validation views (NOT WORKING YET)
	private List<RestValidationMessage> messages;
	
	/**
	 * @param data
	 */
	public AbstractVoModel(T data) {
		super(data);
		this.messages = new ArrayList<RestValidationMessage>();

	}
	
	
	//For CSV Models to define the type
	@CSVColumnGetter(order=0, header="modelType")
	public abstract String getModelType();
	
	@CSVColumnSetter(order=0, header="modelType")
	public void setModelType(String typeName){ }
	
	
	
	@CSVColumnGetter(order=1, header="xid")
	@JsonGetter("xid")
	public String getXid(){
		return this.data.getXid();
	}
	@CSVColumnSetter(order=1, header="xid")
	@JsonSetter("xid")
	public void setXid(String xid){
		this.data.setXid(xid);
	}
	
	@CSVColumnGetter(order=2, header="name")
	@JsonGetter("name")
	public String getName(){
		return this.data.getName();
	}
	@CSVColumnSetter(order=2, header="name")
	@JsonSetter("name")
	public void setName(String name){
		this.data.setName(name);
	}
	
	public void setMessages(List<RestValidationMessage> messages){
		this.messages = messages;
	}
	public List<RestValidationMessage> getMessages(){
		return this.messages;
	}	
	

	/*
	 * (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractRestModel#validate(com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult)
	 */
	@Override
	public boolean validate(){
		ProcessResult validation = new ProcessResult();
		this.data.validate(validation);
		
		if(validation.getHasMessages()){
			//Add our messages to the list
			for(ProcessMessage message : validation.getMessages()){
				this.messages.add(new RestValidationMessage(
						message.getContextualMessage().translate(Common.getTranslations()),
						RestMessageLevel.ERROR,
						message.getContextKey()
						));
			}
			return false;
		}else{
			return true; //Validated ok
		}
	}
	
	/**
	 * Helper to add Validation Message
	 * @param messageKey
	 * @param level
	 * @param property
	 */
	public void addValidationMessage(String messageKey, RestMessageLevel level, String property){
		this.messages.add(new RestValidationMessage(new TranslatableMessage(messageKey).translate(Common.getTranslations()), level, property));
	}

}
