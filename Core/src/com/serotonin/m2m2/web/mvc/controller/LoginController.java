/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.mvc.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.ILifecycle;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.module.AuthenticationDefinition;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.form.LoginForm;
import com.serotonin.provider.Providers;
import com.serotonin.util.ValidationUtils;


/**
 * 
 * TODO This class needs work for use with Spring 4, it is functioning but just barely.
 * 
 * @author Matthew Lohbihler and Terry Packer
 *
 */
@Controller
@RequestMapping("/login.htm")
public class LoginController {
    private static final Log LOG = LogFactory.getLog(LoginController.class);

    public LoginController(){
    	super();
    	this.setCommandName("login");
    	this.setCommandClass("com.serotonin.m2m2.web.mvc.form.LoginForm");
    	this.setFormView("/WEB-INF/jsp/login.jsp");
    	this.setBindOnNewForm(true);
    }
    
    private String commandName;
    private String commandClass;
    private String formView;
    private boolean bindOnNewForm;
    
    @RequestMapping(method=RequestMethod.GET)
    public String initForm(HttpServletRequest request, HttpServletResponse response, @ModelAttribute("login") LoginForm loginForm, BindingResult result){
    	//Edit TP Feb 2014 to show loading page until Mango is started.
    	ILifecycle lifecycle = Providers.get(ILifecycle.class);
    	if((lifecycle.getStartupProgress() < 100f && Common.envProps.getBoolean("showStartup", true)) || (lifecycle.getShutdownProgress() > 0)){
    		return "/startup.htm";
    	}
    	
    	BindException errors = new BindException(result);

        if (!errors.hasErrors()) {
            User user = null;
            for (AuthenticationDefinition def : ModuleRegistry.getDefinitions(AuthenticationDefinition.class)) {
                user = def.preLoginForm(request, response, loginForm, errors);
                if (user != null)
                    break;
            }

            if (user != null)
                return performLogin(request, response, user);
        }

        return "/WEB-INF/jsp/login.jsp";
    }
    
    @RequestMapping(method=RequestMethod.POST)
    public String onSubmit(@ModelAttribute("login") LoginForm login, HttpServletRequest request, HttpServletResponse response,
    		Model model, BindingResult result){

    	BindException errors = new BindException(result);
        // Check if the user exists
        User user = new UserDao().getUser(login.getUsername());
        if (user == null)
            ValidationUtils.rejectValue(errors, "username", "login.validation.noSuchUser");
        else if (user.isDisabled())
            ValidationUtils.reject(errors, "login.validation.accountDisabled");
        else {
            boolean authenticated = false;

            for (AuthenticationDefinition def : ModuleRegistry.getDefinitions(AuthenticationDefinition.class)) {
                authenticated = def.authenticate(request, response, user, login.getPassword(), errors);
                if (authenticated)
                    break;
            }

            if (!authenticated) {
                String passwordHash = Common.encrypt(login.getPassword());

                // Validating the password against the database.
                if (!passwordHash.equals(user.getPassword())) {
                	//Removed logging of failed password
                    LOG.warn("Failed login attempt on user '" + user.getUsername() + "' from IP +" + request.getRemoteAddr());
                    ValidationUtils.reject(errors, "login.validation.invalidLogin");
                }
            }
        }

        if (errors.hasErrors())
            return initForm(request, response, login, errors);

        return performLogin(request, response, user);
    }
//    public String showForm(HttpServletRequest request, HttpServletResponse response, BindException errors,
//            @SuppressWarnings("rawtypes") Map controlModel) throws Exception {
//
//    	//Edit TP Feb 2014 to show loading page until Mango is started.
//    	ILifecycle lifecycle = Providers.get(ILifecycle.class);
//    	if((lifecycle.getStartupProgress() < 100f && Common.envProps.getBoolean("showStartup", true)) || (lifecycle.getShutdownProgress() > 0)){
//    		return new ModelAndView(new RedirectView("/startup.htm"));
//    	}
//    	
//    	
//    	LoginForm loginForm = (LoginForm) errors.getTarget();
//
//        if (!errors.hasErrors()) {
//            User user = null;
//            for (AuthenticationDefinition def : ModuleRegistry.getDefinitions(AuthenticationDefinition.class)) {
//                user = def.preLoginForm(request, response, loginForm, errors);
//                if (user != null)
//                    break;
//            }
//
//            if (user != null)
//                return performLogin(request, response, user);
//        }
//
//        return super.showForm(request, response, errors, controlModel);
//    }

//    @Override
//    protected void onBindAndValidate(HttpServletRequest request, Object command, BindException errors) {
//        LoginForm login = (LoginForm) command;
//
//        // Make sure there is a username
//        if (StringUtils.isBlank(login.getUsername()))
//            ValidationUtils.rejectValue(errors, "username", "login.validation.noUsername");
//
//        // Make sure there is a password
//        if (StringUtils.isBlank(login.getPassword()))
//            ValidationUtils.rejectValue(errors, "password", "login.validation.noPassword");
//    }

//    @Override
//    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command,
//            BindException errors) throws Exception {
//        LoginForm login = (LoginForm) command;
//
//        // Check if the user exists
//        User user = new UserDao().getUser(login.getUsername());
//        if (user == null)
//            ValidationUtils.rejectValue(errors, "username", "login.validation.noSuchUser");
//        else if (user.isDisabled())
//            ValidationUtils.reject(errors, "login.validation.accountDisabled");
//        else {
//            boolean authenticated = false;
//
//            for (AuthenticationDefinition def : ModuleRegistry.getDefinitions(AuthenticationDefinition.class)) {
//                authenticated = def.authenticate(request, response, user, login.getPassword(), errors);
//                if (authenticated)
//                    break;
//            }
//
//            if (!authenticated) {
//                String passwordHash = Common.encrypt(login.getPassword());
//
//                // Validating the password against the database.
//                if (!passwordHash.equals(user.getPassword())) {
//                	//Removed logging of failed password
//                    LOG.warn("Failed login attempt on user '" + user.getUsername() + "' from IP +" + request.getRemoteAddr());
//                    ValidationUtils.reject(errors, "login.validation.invalidLogin");
//                }
//            }
//        }
//
//        if (errors.hasErrors())
//            return showForm(request, response, errors);
//
//        return performLogin(request, response, user);
//    }

    
    
    private String performLogin(HttpServletRequest request, HttpServletResponse response, User user) {
        if (user.isDisabled())
            throw new RuntimeException("User " + user.getUsername() + " is disabled. Aborting login");

        // Update the last login time.
        new UserDao().recordLogin(user.getId());

        // Set the IP Address for the session
        user.setRemoteAddr(request.getRemoteAddr());
        
        // Add the user object to the session. This indicates to the rest of the application whether the user is logged 
        // in or not. Will replace any existing user object.
        Common.setUser(request, user);

        for (AuthenticationDefinition def : ModuleRegistry.getDefinitions(AuthenticationDefinition.class))
            def.postLogin(user);

        String uri = DefaultPagesDefinition.getDefaultUri(request, response, user);        
        
        return"redirect:" + uri;
    }

	public String getCommandName() {
		return commandName;
	}

	public void setCommandName(String form) {
		this.commandName = form;
	}

	public String getCommandClass() {
		return commandClass;
	}

	public void setCommandClass(String commandClass) {
		this.commandClass = commandClass;
	}

	public String getFormView() {
		return formView;
	}

	public void setFormView(String formView) {
		this.formView = formView;
	}

	public boolean isBindOnNewForm() {
		return bindOnNewForm;
	}

	public void setBindOnNewForm(boolean bindOnNewForm) {
		this.bindOnNewForm = bindOnNewForm;
	}
    
    
}
