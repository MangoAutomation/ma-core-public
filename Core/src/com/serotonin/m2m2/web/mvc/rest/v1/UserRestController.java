/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.serotonin.m2m2.db.dao.DaoRegistry;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.rest.v1.model.UserModel;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * @author Terry Packer
 *
 */
@Api(value="Users", description="Operations on Users", position=3)
@RestController
@RequestMapping("/v1/users")
public class UserRestController extends MangoRestController<UserModel>{
	
	private static Logger LOG = Logger.getLogger(UserRestController.class);
	
	public UserRestController(){
	}

	@RequestMapping(method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
	@ResponseBody
    public List<UserModel> getAll(HttpServletRequest request) {
    	if(LOG.isDebugEnabled())
    		LOG.debug("Getting all Users");

    	List<UserModel> userModelList = new ArrayList<UserModel>();
    	List<User> users = DaoRegistry.userDao.getUsers();
    	for(User user : users){
    		userModelList.add(new UserModel(user));
    	}
    	
		return userModelList;

    }
	
	
	@RequestMapping(method = RequestMethod.GET, value = "/{username}")
    public ResponseEntity<UserModel> getUser(
    		@ApiParam(value = "Valid username", required = true, allowMultiple = false)
    		@PathVariable String username, HttpServletRequest request) {
		
		ProcessResult response = new ProcessResult();
		User user = DaoRegistry.userDao.getUser(username);
		
		if (user == null) {
    		//TODO Add to messages or extract to superclass
    		response.addMessage(new TranslatableMessage("common.default", "User Does not exist"));
    		return this.createResponseEntity(response, HttpStatus.NOT_FOUND);
        }
		
		UserModel model = new UserModel(user);
		
		return this.createResponseEntity(response, model, HttpStatus.OK);
		
		
	}

	
	@RequestMapping(method = RequestMethod.PUT, value = "/{username}")
    public ResponseEntity<UserModel> updateUser(
    		@PathVariable String username,
    		UserModel model,
    		HttpServletRequest request) {
		
		ProcessResult response = new ProcessResult();
		LOG.info("Updating user with name " + model.getUsername());
		return this.createResponseEntity(response, model, HttpStatus.OK);
		
		
	}
	
}
