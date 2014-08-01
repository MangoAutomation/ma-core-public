/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.serotonin.m2m2.module.AuthenticationDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
import com.serotonin.m2m2.web.mvc.rest.v1.model.UserModel;
import com.wordnik.swagger.annotations.Api;

/**
 * @author Terry Packer
 *
 */
@Api(value="Logout", description="Operations For Logout")
@RestController
@RequestMapping("/v1/logout")
public class LogoutRestController extends MangoRestController<UserModel>{


	/**
	 * PUT Logout action
	 * @param username
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(method = RequestMethod.PUT, value = "/{username}")
    public ResponseEntity<UserModel> logoutPut(
    		@PathVariable String username,
    		HttpServletRequest request,
    		HttpServletResponse response
    		) {
		return performLogout(username, request, response);
	}
	
	/**
	 * POST Logout action
	 * @param username
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/{username}")
    public ResponseEntity<UserModel> logoutPost(
    		@PathVariable String username,
    		HttpServletRequest request,
    		HttpServletResponse response
    		) {
		return performLogout(username, request, response);
	}

	/**
	 * Shared logout work
	 * 
	 * @param username
	 * @param request
	 * @param response
	 * @return
	 */
	private ResponseEntity<UserModel> performLogout(String username,
			HttpServletRequest request, HttpServletResponse response) {
		RestProcessResult<UserModel> result = new RestProcessResult<UserModel>(HttpStatus.OK);
		
		// Check if the user is logged in.
        User user = this.checkUser(request, result);
        if (result.isOk()) {
            // The user is in fact logged in. Invalidate the session.
            request.getSession().invalidate();

            // Notify any authentication modules of the logout.
            for (AuthenticationDefinition def : ModuleRegistry.getDefinitions(AuthenticationDefinition.class))
                def.logout(request, response, user);
        
            //Return an OK
            UserModel model = new UserModel(user);
            return result.createResponseEntity(model);
        
        }
      	return result.createResponseEntity();
	}
	
	
}
