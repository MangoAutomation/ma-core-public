/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Terry Packer
 *
 */
public class MangoWebSocketResponseModel {

	@JsonProperty("status")
	private MangoWebSocketResponseStatus status;
	
	@JsonProperty("payload")
	private Object payload;

	public MangoWebSocketResponseModel(MangoWebSocketResponseStatus status, Object payload){
		this.status = status;
		this.payload = payload;
	}
	
	public MangoWebSocketResponseModel(){
		
	}
	
	
	
	public MangoWebSocketResponseStatus getStatus() {
		return status;
	}

	public void setStatus(MangoWebSocketResponseStatus status) {
		this.status = status;
	}

	public Object getPayload() {
		return payload;
	}

	public void setPayload(Object payload) {
		this.payload = payload;
	}
	
	
}
