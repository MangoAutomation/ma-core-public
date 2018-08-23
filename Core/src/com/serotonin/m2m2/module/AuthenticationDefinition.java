package com.serotonin.m2m2.module;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;

import com.serotonin.m2m2.vo.User;

/**
 * A definition allowing hooks into user authentication.
 *
 * @author Matthew Lohbihler
 */
abstract public class AuthenticationDefinition extends ModuleElementDefinition {
    /**
     * @return a Spring Security AuthenticationProvider or null
     */
    abstract public AuthenticationProvider authenticationProvider();

    /**
     * Called following a successful authentication of a user.
     * @param authentication Spring authentication object
     * @param user Mango user for the successful authentication (may be null)
     */
    abstract public void authenticationSuccess(Authentication authentication, User user);

    /**
     * Called after a user has been logged out
     * @param user
     *            the user that was logged out
     */
    abstract public void logout(User user);
}
