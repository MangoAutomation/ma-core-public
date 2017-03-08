package com.serotonin.m2m2.module;

import org.springframework.security.authentication.AuthenticationProvider;

import com.serotonin.m2m2.vo.User;

/**
 * A definition allowing hooks into user authentication.
 * 
 * @author Matthew Lohbihler
 */
abstract public class AuthenticationDefinition extends ModuleElementDefinition {
    /**
     * @return a Spring Security AuthenticationProvider
     */
    abstract public AuthenticationProvider authenticationProvider();

    /**
     * Called following a successful login of a user.
     * 
     * @param user
     *            the user that was logged in.
     */
    abstract public void postLogin(User user);

    /**
     * Called after a user has been logged out
     * @param user
     *            the user that was logged out
     */
    abstract public void logout(User user);
}
