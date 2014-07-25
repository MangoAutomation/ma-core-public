/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.permission.DataPointAccess;
import com.serotonin.m2m2.web.mvc.rest.v1.model.permissions.DataPointPermissionModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.permissions.PermissionEnum;
import com.serotonin.m2m2.web.mvc.rest.v1.model.permissions.PermissionsModel;

/**
 * @author Terry Packer
 * 
 */
public class UserModel extends AbstractRestModel<User> {

	public UserModel(){
		super(new User());
	}
	
	public UserModel(User user) {
		super(user);
	}

	@JsonGetter(value = "username")
	public String getUsername() {
		return data.getUsername();
	}

	@JsonSetter(value = "username")
	public void setUsername(String username) {
		data.setUsername(username);
	}

	@JsonSetter(value = "password")
	public void setPassword(String password) {
		data.setPassword(password);
	}

	@JsonGetter(value = "email")
	public String getEmail() {
		return data.getEmail();
	}

	@JsonSetter(value = "email")
	public void setEmail(String email) {
		data.setEmail(email);
	}

	// Missing several properties

	@JsonGetter(value = "permissions")
	public PermissionsModel getPermissions() {
		
		List<DataPointPermissionModel> dataPointPermissions = new ArrayList<DataPointPermissionModel>();
		List<String> dataSourceXids = new ArrayList<String>();
		
		for(Integer dsId : data.getDataSourcePermissions()){
			DataSourceVO<?> vo = DataSourceDao.instance.get(dsId);
			if(vo != null){
				dataSourceXids.add(vo.getXid());
			}
		}
		
		//Do For Data Points
		for(DataPointAccess access : data.getDataPointPermissions()){
			
			DataPointVO vo = DataPointDao.instance.get(access.getDataPointId());
			if(vo != null){
				DataPointPermissionModel model = new DataPointPermissionModel();
				model.setDataPointXid(vo.getXid());
				switch(access.getPermission()){
					case DataPointAccess.READ:
						model.setPermission(PermissionEnum.READ);
					break;
					case DataPointAccess.SET:
						model.setPermission(PermissionEnum.SET);
					break;
				}
				dataPointPermissions.add(model);
			}
		}
		
		PermissionsModel model = new PermissionsModel(dataSourceXids, dataPointPermissions);
		
		return model;
	}

	@JsonSetter(value = "permissions")
	public void setPermissions(List<PermissionsModel> permissions) {
		
		

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.serotonin.m2m2.web.mvc.controller.rest.model.BaseRestModel#validate
	 * (com.serotonin.m2m2.i18n.ProcessResult)
	 */
	@Override
	public void validate(ProcessResult response) {
		this.data.validate(response);
	}

}
