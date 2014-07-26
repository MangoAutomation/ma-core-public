/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.test.data;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.DataPointAccess;

/**
 * @author Terry Packer
 *
 */
public class UserTestData {
	
	
	/**
	 * Create a standard user
	 * @return
	 */
	public static User standardUser(){
		User user = new User();
		user.setId(1);
		user.setUsername("standard");
		user.setPassword("standard");
		user.setEmail("email@address.com");
		user.setPhone("808-000-0000");
		user.setAdmin(false);
		user.setDisabled(false);
		user.setHomeUrl("/home.shtm");
		user.setLastLogin(100000000);
		user.setReceiveAlarmEmails(1);
		user.setReceiveOwnAuditEvents(true);
		user.setTimezone("UTC");
		user.setMuted(false);
		
		//Setup Permissions
		List<Integer> dataSourcePermissions = new ArrayList<Integer>();
		dataSourcePermissions.add(1);
		user.setDataSourcePermissions(dataSourcePermissions);
		
		List<DataPointAccess> dataPointPermissions = new ArrayList<DataPointAccess>();
		DataPointAccess access1 = new DataPointAccess();
		access1.setDataPointId(1);
		access1.setPermission(DataPointAccess.READ);
		dataPointPermissions.add(access1);
		user.setDataPointPermissions(dataPointPermissions);
		
		return user;
	}

}
