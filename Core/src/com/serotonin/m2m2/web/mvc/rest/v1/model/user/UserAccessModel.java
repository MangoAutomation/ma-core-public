/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.user;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Terry Packer
 *
 */
public class UserAccessModel {

	@JsonProperty
	public String accessType;
	@JsonProperty
	public UserModel user;

	public UserAccessModel(String accessType, UserModel user){
		this.accessType = accessType;
		this.user = user;
	}
	public UserAccessModel(){ }
	public String getAccessType() {
		return accessType;
	}
	public void setAccessType(String accessType) {
		this.accessType = accessType;
	}
	public UserModel getUser() {
		return user;
	}
	public void setUser(UserModel user) {
		this.user = user;
	}
	
}
