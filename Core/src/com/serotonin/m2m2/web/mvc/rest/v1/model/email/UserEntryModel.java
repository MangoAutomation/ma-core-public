/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.email;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.infiniteautomation.mango.spring.dao.UserDao;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.mailingList.UserEntry;

/**
 * @author Terry Packer
 *
 */
public class UserEntryModel extends EmailRecipientModel<UserEntry>{

	public UserEntryModel(){
		super(new UserEntry());
	}
	
	/**
	 * @param data
	 */
	public UserEntryModel(UserEntry data) {
		super(data);
	}

	@JsonGetter("username")
	public String getUsername(){
		User u = UserDao.instance.get(this.data.getUserId());
		if(u == null)
			return "user missing";
		else
			return u.getUsername();
	}
	@JsonSetter("username")
	public void setUsername(String username){
		User u = UserDao.instance.getUser(username);
		if(u != null){
			this.data.setUserId(u.getId());
			this.data.setUser(u);
		}
	}
	
}
