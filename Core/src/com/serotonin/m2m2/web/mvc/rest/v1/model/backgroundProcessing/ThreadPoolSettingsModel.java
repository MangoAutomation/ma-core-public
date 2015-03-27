/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.backgroundProcessing;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.serotonin.m2m2.web.mvc.rest.v1.mapping.JsonViews;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestValidationMessage;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * @author Terry Packer
 *
 */
public class ThreadPoolSettingsModel {

	@ApiModelProperty(value = "Messages for validation of data", required = false)
	@JsonProperty("validationMessages")
	@JsonView(JsonViews.Validation.class) //Only show in validation views (NOT WORKING YET)
	private List<RestValidationMessage> messages;
	
	@JsonProperty
	private Integer corePoolSize;
	
	@JsonProperty
	private Integer maximumPoolSize;

	@JsonProperty
	private Integer activeCount; //Number of active Threads
	
	@JsonProperty
	private Integer largestPoolSize; //Largest the pool has been
	
	public ThreadPoolSettingsModel(){ }

	
	/**
	 * @param corePoolSize
	 * @param maximumPoolSize
	 * @param activeCount
	 * @param largestPoolSize
	 */
	public ThreadPoolSettingsModel(Integer corePoolSize, Integer maximumPoolSize,
			Integer activeCount, Integer largestPoolSize) {
		super();
		this.corePoolSize = corePoolSize;
		this.maximumPoolSize = maximumPoolSize;
		this.activeCount = activeCount;
		this.largestPoolSize = largestPoolSize;
	}


	public Integer getActiveCount() {
		return activeCount;
	}

	public void setActiveCount(Integer activeCount) {
		this.activeCount = activeCount;
	}

	public Integer getLargestPoolSize() {
		return largestPoolSize;
	}

	public void setLargestPoolSize(Integer largestPoolSize) {
		this.largestPoolSize = largestPoolSize;
	}

	public Integer getCorePoolSize() {
		return corePoolSize;
	}

	public void setCorePoolSize(Integer corePoolSize) {
		this.corePoolSize = corePoolSize;
	}

	public Integer getMaximumPoolSize() {
		return maximumPoolSize;
	}

	public void setMaximumPoolSize(Integer maximumPoolSize) {
		this.maximumPoolSize = maximumPoolSize;
	}

	public List<RestValidationMessage> getMessages() {
		return messages;
	}

	public void setMessages(List<RestValidationMessage> messages) {
		this.messages = messages;
	}
}
