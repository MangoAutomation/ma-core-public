/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.permissions;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * 
 * @author Terry Packer
 *
 */
public class PermissionsModel {
	
	@JsonProperty
	private List<String> dataSourceXids;
	
	@JsonProperty
	private List<DataPointPermissionModel> dataPointPermissions;

	/**
	 * @param dataSourceXids
	 * @param dataPointPermissions
	 */
	public PermissionsModel(List<String> dataSourceXids,
			List<DataPointPermissionModel> dataPointPermissions) {
		this.dataSourceXids = dataSourceXids;
		this.dataPointPermissions = dataPointPermissions;
	}

	/**
	 * 
	 */
	public PermissionsModel(){
		this.dataSourceXids = new ArrayList<String>();
		this.dataPointPermissions = new ArrayList<DataPointPermissionModel>();
	}
	
	public List<String> getDataSourceXids() {
		return dataSourceXids;
	}

	public void setDataSourceXids(List<String> dataSourceXids) {
		this.dataSourceXids = dataSourceXids;
	}

	public List<DataPointPermissionModel> getDataPointPermissions() {
		return dataPointPermissions;
	}

	public void setDataPointPermissions(
			List<DataPointPermissionModel> dataPointPermissions) {
		this.dataPointPermissions = dataPointPermissions;
	}
	
}
