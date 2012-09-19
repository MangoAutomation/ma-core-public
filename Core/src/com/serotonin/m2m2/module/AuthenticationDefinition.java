package com.serotonin.m2m2.module;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;

import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.form.LoginForm;

/**
 * A definition allowing hooks into user authentication.
 * 
 * @author Matthew Lohbihler
 */
abstract public class AuthenticationDefinition extends ModuleElementDefinition {
    /**
     * This method provides a veto for the definition. If false is returned, the user will be logged out.
     * 
     * @param request
     *            the HTTP request
     * @param response
     *            the HTTP response
     * @param user
     *            the current user. Will not be null
     * @return the default return value should be true. False is only returned if the definition has determined that the
     *         user is no longer logged in.
     */
    abstract public boolean isAuthenticated(HttpServletRequest request, HttpServletResponse response, User user);

    /**
     * This method is called prior to the presentation of the login form to the user. It provides a means to
     * automatically log the user in, and prevent the login form from being shown.
     * 
     * @param request
     *            the HTTP request
     * @param response
     *            the HTTP response
     * @param loginForm
     *            the login form object. Default values can be set in it if the login form is to be shown.
     * @param errors
     *            the Spring errors object. Use ValidationUtils to add error messages
     * @return the User object to be logged in, or null if no automatic login is to occur. This user should already have
     *         been checked to ensure not disabled.
     */
    abstract public User preLoginForm(HttpServletRequest request, HttpServletResponse response, LoginForm loginForm,
            BindException errors);

    /**
     * Allows the definition to authenticate user information.
     * 
     * @param request
     *            the HTTP request
     * @param response
     *            the HTTP response
     * @param user
     *            the user object found by looking up in the database
     * @param password
     *            the password that the user entered in the login form. This may not be the same that the password in
     *            the user object
     * @param errors
     *            the Spring errors object. Use ValidationUtils to add error messages
     * @return true if the user should be logged in, false otherwise
     */
    abstract public boolean authenticate(HttpServletRequest request, HttpServletResponse response, User user,
            String password, BindException errors);

    /**
     * Called following a successful login of a user.
     * 
     * @param user
     *            the user that was logged in.
     */
    abstract public void postLogin(User user);

    /**
     * Called after a user has been logged out
     * 
     * @param request
     *            the HTTP request
     * @param response
     *            the HTTP response
     * @param user
     *            the user that was logged out
     */
    abstract public void logout(HttpServletRequest request, HttpServletResponse response, User user);
}
