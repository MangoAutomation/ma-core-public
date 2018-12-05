/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.mvc.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.web.mvc.form.LoginForm;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoPasswordAuthenticationProvider.AuthenticationRateException;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoPasswordAuthenticationProvider.IpAddressAuthenticationRateException;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoPasswordAuthenticationProvider.PasswordChangeException;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoPasswordAuthenticationProvider.UsernameAuthenticationRateException;

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
        HttpSession session = request.getSession(false);
        if (session != null) {
            AuthenticationException ex = (AuthenticationException) session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            if (ex != null) {
                TranslatableMessage message;

                if (ex instanceof AuthenticationRateException) {
                    message = messageForAuthenticationRateException((AuthenticationRateException) ex);
                } else {
                    message = messageForAuthenticationFailedException(ex);
                }

                errors.reject(message.getKey(), message.getArgs(), ex.getMessage());
            }

            String username = (String)session.getAttribute("username");
            if (username != null && !username.isEmpty()) {
                loginForm.setUsername(username);
            }
        }

        //TODO What if this is a forwarded request?  There shan't be a session....

        // display errors on the form or next to inputs like so
        // errors.reject("translation.key", "Fall back text");
        // errors.rejectValue("password", "translation.key", "Fall back text");


        return "/WEB-INF/jsp/login.jsp";
    }

    /**
     * This logic was lifted from the various v2 REST exception classes
     * 
     * @param ex
     * @return
     */
    private TranslatableMessage messageForAuthenticationFailedException(
            AuthenticationException cause) {
        if (cause instanceof BadCredentialsException || cause instanceof UsernameNotFoundException) {
            return new TranslatableMessage("rest.exception.badCredentials");
        } else if (cause instanceof CredentialsExpiredException) {
            return new TranslatableMessage("rest.exception.credentialsExpired");
        } else if (cause instanceof DisabledException) {
            return new TranslatableMessage("rest.exception.accountDisabled");
        } else if (cause instanceof PasswordChangeException) {
            return ((PasswordChangeException) cause).getTranslatableMessage();
        } else {
            return new TranslatableMessage("rest.exception.genericAuthenticationFailed", cause.getClass().getSimpleName() + " " + cause.getMessage());
        }
    }

    private TranslatableMessage messageForAuthenticationRateException(AuthenticationRateException cause) {
        if (cause instanceof IpAddressAuthenticationRateException) {
            return new TranslatableMessage("rest.exception.authenticationIpRateLimited", ((IpAddressAuthenticationRateException) cause).getIp());
        } else if (cause instanceof UsernameAuthenticationRateException) {
            return new TranslatableMessage("rest.exception.authenticationUsernameRateLimited", ((UsernameAuthenticationRateException) cause).getUsername());
        } else {
            throw new IllegalArgumentException("Unknown AuthenticationRateException");
        }
    }
    
}
