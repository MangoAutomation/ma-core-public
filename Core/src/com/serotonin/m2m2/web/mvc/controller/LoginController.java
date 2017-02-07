/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.mvc.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.serotonin.m2m2.web.mvc.form.LoginForm;

/**
 * 
 * @author Matthew Lohbihler and Terry Packer
 *
 */
@Controller
@RequestMapping("/login.htm")
public class LoginController {

    @RequestMapping(method=RequestMethod.GET)
    public String initForm(HttpServletRequest request, HttpServletResponse response, @ModelAttribute("login") LoginForm loginForm, BindingResult result) {
    	BindException errors = new BindException(result);

    	String errorParameter = request.getParameter("error");
    	if (errorParameter != null) {
    	    errors.reject(errorParameter);
    	    
    	    /* 
    	     * Alternative is to get the stored exception from SimpleUrlAuthenticationFailureHandler
    	     * 
            HttpSession session = request.getSession(false);
            if (session != null) {
                AuthenticationException ex = (AuthenticationException) session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
                if (ex != null) {
                    if (ex instanceof DisabledException) {
                        errors.reject("login.validation.accountDisabled", ex.getMessage());
                    } else {
                        errors.reject("login.validation.invalidLogin", ex.getMessage());
                    }
                }
            }
            */
            
            String username = request.getParameter("username");
            if (username != null && !username.isEmpty()) {
                loginForm.setUsername(username);
            }
            
            // display errors on the form or next to inputs like so
            // errors.reject("translation.key", "Fall back text");
            // errors.rejectValue("password", "translation.key", "Fall back text");
    	}
    	
        return "/WEB-INF/jsp/login.jsp";
    }

}
