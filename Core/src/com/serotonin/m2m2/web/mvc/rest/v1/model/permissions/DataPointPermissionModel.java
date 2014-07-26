/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.permissions;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Terry Packer
 *
 */
public class DataPointPermissionModel {

	public DataPointPermissionModel(){
		
	}
	
	@JsonProperty(value="dataPointXid", index=0, required=true)
	private String dataPointXid;
	
	@JsonProperty(value="permission", index=1, required=true)
	private PermissionEnum permission;

	public String getDataPointXid() {
		return dataPointXid;
	}

	public void setDataPointXid(String dataPointXid) {
		this.dataPointXid = dataPointXid;
	}

	public PermissionEnum getPermission() {
		return permission;
	}

	public void setPermission(PermissionEnum permission) {
		this.permission = permission;
	}
	
	
}
