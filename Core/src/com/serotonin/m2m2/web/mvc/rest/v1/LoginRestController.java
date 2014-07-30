/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.DataBinder;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DaoRegistry;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.AuthenticationDefinition;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.rest.v1.model.UserModel;
import com.wordnik.swagger.annotations.Api;

/**
 * Rest Login endpoint
 * 
 * 
 * 
 * 
 * 
 * @author Terry Packer
 *
 */
@Api(value="Login", description="Operations For Login")
@RestController
@RequestMapping("/v1/login")
public class LoginRestController extends MangoRestController<UserModel>{
	
	private static final Log LOG = LogFactory.getLog(LoginRestController.class);
	
	@RequestMapping(method = RequestMethod.POST, value = "/{username}")
    public ResponseEntity<UserModel> login(
    		@PathVariable String username,
    		@RequestParam(value="password", required=true, defaultValue="") String password,
    		HttpServletRequest request,
    		HttpServletResponse response
    		) {
		DataBinder binder = new DataBinder(User.class);
		ProcessResult result = new ProcessResult();
		boolean authenticated = false;
		User user = DaoRegistry.userDao.getUser(username);
		
        if (user == null)
        	result.addMessage(new TranslatableMessage("login.validation.noSuchUser"));
        else if (user.isDisabled())
        	result.addMessage(new TranslatableMessage( "login.validation.accountDisabled"));
        else {
        	
        	//Hack for now to get a BindException object so we can use the Auth Defs to login.
            BindException errors = new BindException(binder.getBindingResult());
            
            for (AuthenticationDefinition def : ModuleRegistry.getDefinitions(AuthenticationDefinition.class)) {
                authenticated = def.authenticate(request, response, user, password, errors);
                if (authenticated)
                    break;
            }

            if (!authenticated) {
                String passwordHash = Common.encrypt(password);

                // Validating the password against the database.
                if (!passwordHash.equals(user.getPassword())) {
                	//Removed logging of failed password
                    LOG.warn("Failed login attempt on user '" + user.getUsername() + "' from IP +" + request.getRemoteAddr());
                    result.addMessage(new TranslatableMessage("login.validation.invalidLogin"));
                }else{
                	authenticated = true;
                }
                
                if (errors.hasErrors()){
                	for(ObjectError error : errors.getAllErrors()){
                		result.addMessage(new TranslatableMessage("common.default", error.getDefaultMessage()));
                	}
                }
            }
        }
        ResponseEntity<UserModel> responseEntity;
        
        if(authenticated){
	        //Perform the Login and return the default login URI for the user
	        String uri = performLogin(request, response, user);
	        UserModel model = new UserModel(user);
	        HttpHeaders headers = new HttpHeaders();
	        headers.add("user-default-login-uri", uri);
			responseEntity = this.createResponseEntity(result, model, headers, HttpStatus.OK);
        }else{
        	responseEntity = createResponseEntity(result);
        }
        
		return responseEntity;
	}

	/**
	 * Login action
	 * @param request
	 * @param response
	 * @param user
	 * @return
	 */
    private String performLogin(HttpServletRequest request, HttpServletResponse response, User user) {
        if (user.isDisabled())
            return "disabled";

        // Update the last login time.
       DaoRegistry.userDao.recordLogin(user.getId());

        // Set the IP Address for the session
        user.setRemoteAddr(request.getRemoteAddr());
        
        // Add the user object to the session. This indicates to the rest of the application whether the user is logged 
        // in or not. Will replace any existing user object.
        Common.setUser(request, user);

        for (AuthenticationDefinition def : ModuleRegistry.getDefinitions(AuthenticationDefinition.class))
            def.postLogin(user);

        String uri = DefaultPagesDefinition.getDefaultUri(request, response, user);        
        
        return uri;
    }

}
