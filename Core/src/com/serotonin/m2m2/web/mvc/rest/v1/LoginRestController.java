/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.AuthenticationDefinition;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
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
@Api(value = "Login", description = "Operations For Login")
@RestController
@RequestMapping("/v1/login")
public class LoginRestController extends MangoRestController<UserModel> {

	private static final Log LOG = LogFactory.getLog(LoginRestController.class);
	public static final String LOGIN_DEFAULT_URI_HEADER = "user-home-uri";

	/**
	 * PUT login action
	 * @param username
	 * @param password
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(method = RequestMethod.PUT, value = "/{username}")
	public ResponseEntity<UserModel> loginPut(
			@PathVariable String username,
			@RequestParam(value = "password", required = true, defaultValue = "") String password,
			HttpServletRequest request, HttpServletResponse response) {
		return performLogin(username, password, request, response);
	}

	/**
	 * POST login action
	 * @param username
	 * @param password
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/{username}")
	public ResponseEntity<UserModel> loginPost(
			@PathVariable String username,
			@RequestParam(value = "password", required = true, defaultValue = "") String password,
			HttpServletRequest request, HttpServletResponse response) {
		return performLogin(username, password, request, response);
	}

	/**
	 * Shared work for the login process
	 * @param username
	 * @param password
	 * @param request
	 * @param response
	 * @return
	 */
	private ResponseEntity<UserModel> performLogin(String username, String password,
			HttpServletRequest request, HttpServletResponse response) {
		
		DataBinder binder = new DataBinder(User.class);
		RestProcessResult<UserModel> result = new RestProcessResult<UserModel>(HttpStatus.OK);
		boolean authenticated = false;
		User user = DaoRegistry.userDao.getUser(username);

		if (user == null)
			result.addRestMessage(this.getDoesNotExistMessage());
		else if (user.isDisabled())
			result.addRestMessage(HttpStatus.NOT_ACCEPTABLE, new TranslatableMessage(
					"login.validation.accountDisabled"));
		else {

			// Hack for now to get a BindException object so we can use the Auth
			// Defs to login.
			BindException errors = new BindException(binder.getBindingResult());

			for (AuthenticationDefinition def : ModuleRegistry
					.getDefinitions(AuthenticationDefinition.class)) {
				authenticated = def.authenticate(request, response, user,
						password, errors);
				if (authenticated)
					break;
			}

			if (!authenticated) {
				String passwordHash = Common.encrypt(password);

				// Validating the password against the database.
				if (!passwordHash.equals(user.getPassword())) {
					// Removed logging of failed password
					LOG.warn("Failed login attempt on user '"
							+ user.getUsername() + "' from IP +"
							+ request.getRemoteAddr());
					result.addRestMessage(HttpStatus.NOT_ACCEPTABLE, new TranslatableMessage(
							"login.validation.invalidLogin"));
				} else {
					authenticated = true;
				}

				if (errors.hasErrors()) {
					for (ObjectError error : errors.getAllErrors()) {
						result.addRestMessage(HttpStatus.NOT_ACCEPTABLE, new TranslatableMessage(
								"common.default", error.getDefaultMessage()));
					}
				}
			}
		}

		if (authenticated) {
			// Perform the Login and return the default login URI for the user
			// Update the last login time.
			DaoRegistry.userDao.recordLogin(user.getId());

			// Set the IP Address for the session
			user.setRemoteAddr(request.getRemoteAddr());

			// Add the user object to the session. This indicates to the rest of
			// the application whether the user is logged
			// in or not. Will replace any existing user object.
			Common.setUser(request, user);

			for (AuthenticationDefinition def : ModuleRegistry
					.getDefinitions(AuthenticationDefinition.class))
				def.postLogin(user);

			String uri = DefaultPagesDefinition.getDefaultUri(request,
					response, user);
			UserModel model = new UserModel(user);
			result.addHeader(LOGIN_DEFAULT_URI_HEADER, uri);
			return result.createResponseEntity(model);
		} else {
			return result.createResponseEntity();
		}
	}

}
