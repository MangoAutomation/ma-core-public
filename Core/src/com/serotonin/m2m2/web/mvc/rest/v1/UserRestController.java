/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DaoRegistry;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
import com.serotonin.m2m2.web.mvc.rest.v1.model.UserModel;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * @author Terry Packer
 *
 */
@Api(value="Users", description="Operations on Users")
@RestController
@RequestMapping("/v1/users")
public class UserRestController extends MangoRestController<UserModel>{
	
	private static Logger LOG = Logger.getLogger(UserRestController.class);
	
	public UserRestController(){
	}

	@RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<UserModel>> getAll(HttpServletRequest request) {
		RestProcessResult<List<UserModel>> result = new RestProcessResult<List<UserModel>>(HttpStatus.OK);
    	
		User user = this.checkUser(request, result);
    	if(result.isOk()){
    		
    		if(user.isAdmin()){
    	    	List<UserModel> userModelList = new ArrayList<UserModel>();
    	    	List<User> users = DaoRegistry.userDao.getUsers();
    	    	for(User u : users){
    	    		userModelList.add(new UserModel(u));
    	    	}
    			return result.createResponseEntity(userModelList);
    		}else{
    			LOG.warn("Non admin user: " + user.getUsername() + " attempted to access all users");
    			result.addRestMessage(this.getUnauthorizedMessage());
    			return result.createResponseEntity();
    		}
    	}
    	
    	return result.createResponseEntity();
    }
	
	
	@RequestMapping(method = RequestMethod.GET, value = "/{username}")
    public ResponseEntity<UserModel> getUser(
    		@ApiParam(value = "Valid username", required = true, allowMultiple = false)
    		@PathVariable String username, HttpServletRequest request) {
		
		RestProcessResult<UserModel> result = new RestProcessResult<UserModel>(HttpStatus.OK);
    	User user = this.checkUser(request, result);
    	if(result.isOk()){
    		User u = DaoRegistry.userDao.getUser(username);
    		if(user.isAdmin()){
    			if (u == null) {
    				result.addRestMessage(getDoesNotExistMessage());
    	    		return result.createResponseEntity();
    	        }
    			UserModel model = new UserModel(u);
    			return result.createResponseEntity(model);
    		}else{
    			if(u.getId() != user.getId()){
	    			LOG.warn("Non admin user: " + user.getUsername() + " attempted to access user : " + u.getUsername());
	    			result.addRestMessage(this.getUnauthorizedMessage());
	    			return result.createResponseEntity();
    			}else{
    				//Allow users to access themselves
    				return result.createResponseEntity(new UserModel(u));
    			}
    		}
    	}
    	
    	return result.createResponseEntity();
	}

	
	@RequestMapping(method = RequestMethod.PUT, value = "/{username}")
    public ResponseEntity<UserModel> updateUser(
    		@PathVariable String username,
    		UserModel model,
    		HttpServletRequest request) {

		RestProcessResult<UserModel> result = new RestProcessResult<UserModel>(HttpStatus.OK);
    	User user = this.checkUser(request, result);
    	if(result.isOk()){
    		User u = DaoRegistry.userDao.getUser(username);
    		if(user.isAdmin()){
    			if (u == null) {
    				result.addRestMessage(getDoesNotExistMessage());
    	    		return result.createResponseEntity();
    	        }
    			model.getData().setId(u.getId());
    			ProcessResult validation = new ProcessResult();
				model.validate(validation);
				if(validation.getHasMessages()){
		        	result.addRestMessage(model.addValidationMessages(validation));
		        	return result.createResponseEntity(model); 
		        }else{
	    			DaoRegistry.userDao.saveUser(model.getData());
	    			return result.createResponseEntity(model);
		        }
    		}else{
    			if(u.getId() != user.getId()){
	    			LOG.warn("Non admin user: " + user.getUsername() + " attempted to access user : " + u.getUsername());
	    			result.addRestMessage(this.getUnauthorizedMessage());
	    			return result.createResponseEntity();
    			}else{
    				//Allow users to update themselves
    				model.getData().setId(u.getId());
    				ProcessResult validation = new ProcessResult();
    				model.validate(validation);
    				if(validation.getHasMessages()){
    		        	result.addRestMessage(model.addValidationMessages(validation));
    		        	return result.createResponseEntity(model); 
    		        }else{
	        			DaoRegistry.userDao.saveUser(model.getData());
	    				return result.createResponseEntity(model);
    		        }
    			}
    		}
    	}
    	
    	return result.createResponseEntity();
	}
	
	/**
	 * Create a new User
	 * @param model
	 * @param request
	 * @return
	 */
	@ApiOperation(
			value = "Create New User",
			notes = "Cannot save existing user"
			)
	@ApiResponses({
			@ApiResponse(code = 201, message = "User Created", response=UserModel.class),
			@ApiResponse(code = 401, message = "Unauthorized Access", response=ResponseEntity.class),
			@ApiResponse(code = 409, message = "User Already Exists")
			})
	@RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<UserModel> createNewUser(
    		@ApiParam( value = "User to save", required = true )
    		UserModel model,
    		UriComponentsBuilder builder,
    		HttpServletRequest request) {

		RestProcessResult<UserModel> result = new RestProcessResult<UserModel>(HttpStatus.OK);
    	User user = this.checkUser(request, result);
    	if(result.isOk()){
    		User u = DaoRegistry.userDao.getUser(model.getUsername());
    		if(user.isAdmin()){
    			if (u == null) {
    				//Create new user
    				model.getData().setId(Common.NEW_ID);
    				DaoRegistry.userDao.saveUser(model.getData());
    				ProcessResult validation = new ProcessResult();
    				model.validate(validation);
    				if(validation.getHasMessages()){
    		        	result.addRestMessage(model.addValidationMessages(validation));
    		        	return result.createResponseEntity(model); 
    		        }else{
	    		    	URI location = builder.path("/rest/v1/users/{username}").buildAndExpand(model.getUsername()).toUri();
	    		    	result.addRestMessage(getResourceCreatedMessage(location));
	    		        return result.createResponseEntity(model);
    		        }
    	        }else{
    	        	result.addRestMessage(getAlreadyExistsMessage());
    	        	return result.createResponseEntity();
    	        }
    		}else{
    			LOG.warn("Non admin user: " + user.getUsername() + " attempted to create user : " + model.getUsername());
    			result.addRestMessage(this.getUnauthorizedMessage());
    			return result.createResponseEntity();
    		}
    	}
    	
    	return result.createResponseEntity();
	}
	
}
