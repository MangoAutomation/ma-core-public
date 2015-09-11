/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.mvc.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.ILifecycle;
import com.serotonin.m2m2.i18n.TranslatableException;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.form.LoginForm;
import com.serotonin.provider.Providers;


/**
 * 
 * @author Matthew Lohbihler and Terry Packer
 *
 */
@Controller
@RequestMapping("/login.htm")
public class LoginController {

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
			try {
				user = Common.loginManager.performLogin(null, null, request, response, loginForm, errors, false, false);
			} catch (TranslatableException e) {
				//Munch, we don't care
			}

            if (user != null)
                return "redirect:" + DefaultPagesDefinition.getDefaultUri(request, response, user);        
        }

        return "/WEB-INF/jsp/login.jsp";
    }
    
    @RequestMapping(method=RequestMethod.POST)
    public String onSubmit(@ModelAttribute("login") LoginForm login, HttpServletRequest request, HttpServletResponse response,
    		Model model, BindingResult result){

    	BindException errors = new BindException(result);
    	
    	try{
    		User user = Common.loginManager.performLogin(login.getUsername(), login.getPassword(), request, response, null, errors, false, false);
    		if ((user == null)||errors.hasErrors())
    			return initForm(request, response, login, errors);
    		else
    			return "redirect:" + DefaultPagesDefinition.getDefaultUri(request, response, user);      
    	}catch(TranslatableException e){
    		
    		//Add the error
    		errors.reject(e.getTranslatableMessage().getKey(), e.getLocalizedMessage());
    		
           return initForm(request, response, login, errors);
    	}
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
