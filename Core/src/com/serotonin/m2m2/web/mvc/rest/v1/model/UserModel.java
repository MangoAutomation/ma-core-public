/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.serotonin.m2m2.db.dao.DaoRegistry;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.permission.DataPointAccess;
import com.serotonin.m2m2.web.mvc.rest.v1.exception.RestValidationFailedException;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.AlarmLevel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.permissions.DataPointPermissionModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.permissions.PermissionEnum;
import com.serotonin.m2m2.web.mvc.rest.v1.model.permissions.PermissionsModel;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * @author Terry Packer
 * 
 */
@ApiModel(value="User", description="User Data Model", parent=AbstractRestModel.class)
@JsonPropertyOrder({"username", "email"})
public class UserModel extends AbstractRestModel<User> {
	
	public UserModel(){
		super(new User());
		this.data.setDataPointPermissions(new ArrayList<DataPointAccess>());
		this.data.setDataSourcePermissions(new ArrayList<Integer>());
	}
	
	public UserModel(User user) {
		super(user);
	}

	@ApiModelProperty(value = "Username of User", required = true)
	@JsonGetter("username")
	public String getUsername() {
		return data.getUsername();
	}

	@JsonSetter("username")
	public void setUsername(String username) {
		data.setUsername(username);
	}

	//TODO This is not working yet
	// the idea is that the password will only be
	// available in the Test View
	@JsonGetter("password")
	public String getPassword() {
			return ""; //data.getPassword(); 
	}
	@JsonSetter("password")
	public void setPassword(String password) {
		data.setPassword(password);
	}

	@JsonGetter("email")
	public String getEmail() {
		return data.getEmail();
	}

	@JsonSetter("email")
	public void setEmail(String email) {
		data.setEmail(email);
	}

	@JsonGetter("disabled")
	public Boolean getDisabled() {
		return data.isDisabled();
	}

	@JsonSetter("disabled")
	public void setDisabled(Boolean disabled) {
		data.setDisabled(disabled);
	}
	
	@JsonGetter("permissions")
	public PermissionsModel getPermissions() {
		
		List<DataPointPermissionModel> dataPointPermissions = new ArrayList<DataPointPermissionModel>();
		List<String> dataSourceXids = new ArrayList<String>();
		
		for(Integer dsId : data.getDataSourcePermissions()){
			DataSourceVO<?> vo = DaoRegistry.dataSourceDao.get(dsId);
			if(vo != null){
				dataSourceXids.add(vo.getXid());
			}
		}
		
		//Do For Data Points
		for(DataPointAccess access : data.getDataPointPermissions()){
			
			DataPointVO vo = DaoRegistry.dataPointDao.get(access.getDataPointId());
			if(vo != null){
				DataPointPermissionModel model = new DataPointPermissionModel();
				model.setDataPointXid(vo.getXid());
				model.setPermission(PermissionEnum.convertTo(access.getPermission()));
				dataPointPermissions.add(model);
			}
		}
		
		PermissionsModel model = new PermissionsModel(dataSourceXids, dataPointPermissions);
		
		return model;
	}

	@JsonSetter("permissions")
	public void setPermissions(PermissionsModel permissions) {
		
		for(String dsXid : permissions.getDataSourceXids()){
			DataSourceVO<?> vo = DaoRegistry.dataSourceDao.getByXid(dsXid);
			if(vo != null)
				this.data.getDataSourcePermissions().add(vo.getId());
		}
		
		for(DataPointPermissionModel model : permissions.getDataPointPermissions()){
			DataPointVO vo = DaoRegistry.dataPointDao.getByXid(model.getDataPointXid());
			if(vo != null){
				DataPointAccess access = new DataPointAccess();
				access.setDataPointId(vo.getId());
				access.setPermission(PermissionEnum.convertFrom(model.getPermission()));
				this.data.getDataPointPermissions().add(access);
			}
		}
		
	}
	
	@JsonGetter("homeUrl")
	public String getHomeUrl() {
		return data.getHomeUrl();
	}

	@JsonSetter("homeUrl")
	public void setHomeUrl(String homeUrl) {
		data.setHomeUrl(homeUrl);
	}
	
	@JsonGetter("receiveAlarmEmails")
	public AlarmLevel getReceiveAlarmEmails() {
		return AlarmLevel.convertTo(data.getReceiveAlarmEmails());
	}

	@JsonSetter("receiveAlarmEmails")
	public void setReceiveAlarmEmails(AlarmLevel level) {
		data.setReceiveAlarmEmails(AlarmLevel.convertFrom(level));
	}
	
	@JsonGetter("timezone")
	public String getTimezone() {
		return data.getTimezone();
	}

	@JsonSetter("timezone")
	public void setTimezone(String zone) {
		data.setTimezone(zone);
	}
	
	@JsonGetter("muted")
	public Boolean getMuted() {
		return data.isMuted();
	}

	@JsonSetter("muted")
	public void setMuted(Boolean muted) {
		data.setMuted(muted);
	}

	/*
	 * (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractRestModel#validate(com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult)
	 */
	@Override
	public void validate(RestProcessResult<?> result) throws RestValidationFailedException{
		ProcessResult validation = new ProcessResult();
		this.data.validate(validation);
		
		if(validation.getHasMessages()){
			result.addRestMessage(HttpStatus.BAD_REQUEST, new TranslatableMessage("common.default", "Validation failed"));
			result.addValidationMessages(validation);
			throw new RestValidationFailedException(this, result);
		}
	}


	
}
