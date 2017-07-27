/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.script;

import java.io.Serializable;
import java.util.Set;

import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * Container for all scripts that holds the 
 * permissions during runtime.
 * 
 * @author Terry Packer
 *
 */
public class ScriptPermissions implements Serializable{
	
	public static final String DATA_SOURCE = "scriptDataSourcePermission";
	public static final String DATA_POINT_READ = "scriptDataPointReadPermission";
	public static final String DATA_POINT_SET = "scriptDataPointSetPermission";
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@JsonProperty
	private String dataSourcePermissions = new String();
	@JsonProperty
	private String dataPointSetPermissions = new String();
	@JsonProperty
	private String dataPointReadPermissions = new String();
	@JsonProperty
	private String customPermissions = new String();
	
	public ScriptPermissions(){	
		dataSourcePermissions = new String();
		dataPointSetPermissions = new String();
		dataPointReadPermissions = new String();
		customPermissions = new String();
	}
	
	public ScriptPermissions(User user){
		if(user == null){
			dataSourcePermissions = new String();
			dataPointSetPermissions = new String();
			dataPointReadPermissions = new String();
			customPermissions = new String();
		
		}else{
			dataPointReadPermissions = user.getPermissions();
			dataPointSetPermissions = user.getPermissions();
			dataSourcePermissions = user.getPermissions();
			customPermissions = user.getPermissions();
		}
	}
	
	public String getDataSourcePermissions() {
		return dataSourcePermissions;
	}
	public void setDataSourcePermissions(String dataSourcePermissions) {
		this.dataSourcePermissions = dataSourcePermissions;
	}
	public String getDataPointSetPermissions() {
		return dataPointSetPermissions;
	}
	public void setDataPointSetPermissions(String dataPointSetPermissions) {
		this.dataPointSetPermissions = dataPointSetPermissions;
	}
	public String getDataPointReadPermissions() {
		return dataPointReadPermissions;
	}
	public void setDataPointReadPermissions(String dataPointReadPermissions) {
		this.dataPointReadPermissions = dataPointReadPermissions;
	}
	public String getCustomPermissions() {
		return customPermissions;
	}
	public void setCustomPermissions(String customPermissions) {
		this.customPermissions = customPermissions;
	}
	
	public void validate(ProcessResult response, User user){
		if(user == null){
			response.addContextualMessage(DATA_SOURCE, "validate.invalidPermission","No User Found");
			response.addContextualMessage(DATA_POINT_SET, "validate.invalidPermission","No User Found");
			response.addContextualMessage(DATA_POINT_READ, "validate.invalidPermission","No User Found");
			return;
		}
		
		if(user.isAdmin())
			return;
			
		//If superadmin then fine or if not then only allow my groups
		if((!this.dataSourcePermissions.isEmpty())&&(!Permissions.hasPermission(this.dataSourcePermissions, user.getPermissions()))){
			Set<String> invalid = Permissions.findInvalidPermissions(this.dataSourcePermissions, user.getPermissions());
			String notGranted = Permissions.implodePermissionGroups(invalid);
			response.addContextualMessage(DATA_SOURCE, "validate.invalidPermission", notGranted);
		}
		if((!this.dataPointSetPermissions.isEmpty())&&(!Permissions.hasPermission(this.dataPointSetPermissions, user.getPermissions()))){
			Set<String> invalid = Permissions.findInvalidPermissions(this.dataPointSetPermissions, user.getPermissions());
			String notGranted = Permissions.implodePermissionGroups(invalid);
			response.addContextualMessage(DATA_POINT_SET, "validate.invalidPermission", notGranted);
		}
		if((!this.dataPointReadPermissions.isEmpty())&&(!Permissions.hasPermission(this.dataPointReadPermissions, user.getPermissions()))){
			Set<String> invalid = Permissions.findInvalidPermissions(this.dataPointReadPermissions, user.getPermissions());
			String notGranted = Permissions.implodePermissionGroups(invalid);
			response.addContextualMessage(DATA_POINT_READ, "validate.invalidPermission", notGranted);
		}
	}
	
	public void validate(ProcessResult response, User user, ScriptPermissions oldPermissions) {
		if(user.isAdmin())
			return;
		
		Set<String> nonUserPre = Permissions.findInvalidPermissions(oldPermissions.getDataSourcePermissions(), user.getPermissions());
		Set<String> nonUserPost = Permissions.findInvalidPermissions(this.dataSourcePermissions, user.getPermissions());
		if(nonUserPre.size() != nonUserPost.size())
			response.addContextualMessage(DATA_SOURCE, "validate.invalidPermissionModification", user.getPermissions());
		else {
			for(String s : nonUserPre)
				if(!nonUserPost.contains(s))
					response.addContextualMessage(DATA_SOURCE, "validate.invalidPermissionModification", user.getPermissions());
		}
		
		nonUserPre = Permissions.findInvalidPermissions(oldPermissions.getDataPointSetPermissions(), user.getPermissions());
		nonUserPost = Permissions.findInvalidPermissions(this.dataPointSetPermissions, user.getPermissions());
		if(nonUserPre.size() != nonUserPost.size())
			response.addContextualMessage(DATA_POINT_SET, "validate.invalidPermissionModification", user.getPermissions());
		else {
			for(String s : nonUserPre)
				if(!nonUserPost.contains(s))
					response.addContextualMessage(DATA_POINT_SET, "validate.invalidPermissionModification", user.getPermissions());
		}
		
		nonUserPre = Permissions.findInvalidPermissions(oldPermissions.getDataPointReadPermissions(), user.getPermissions());
		nonUserPost = Permissions.findInvalidPermissions(this.dataPointReadPermissions, user.getPermissions());
		if(nonUserPre.size() != nonUserPost.size())
			response.addContextualMessage(DATA_POINT_READ, "validate.invalidPermissionModification", user.getPermissions());
		else {
			for(String s : nonUserPre)
				if(!nonUserPost.contains(s))
					response.addContextualMessage(DATA_POINT_READ, "validate.invalidPermissionModification", user.getPermissions());
		}
	}
}
