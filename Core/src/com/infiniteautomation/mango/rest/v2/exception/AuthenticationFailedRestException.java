/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.rest.v2.exception;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoPasswordAuthenticationProvider.PasswordChangeException;

/**
 * @author Jared Wiltshire
 */
public class AuthenticationFailedRestException extends AbstractRestV2Exception {
    private static final long serialVersionUID = 1L;

    private AuthenticationFailedRestException(MangoRestErrorCode code, TranslatableMessage message, AuthenticationException cause) {
        super(HttpStatus.UNAUTHORIZED, code, message, cause);
    }

    public static AuthenticationFailedRestException restExceptionFor(AuthenticationException cause) {
        if (cause instanceof BadCredentialsException || cause instanceof UsernameNotFoundException) {
            return new AuthenticationFailedRestException(MangoRestErrorCode.BAD_CREDENTIALS,
                    new TranslatableMessage("rest.exception.badCredentials"),
                    cause);
        } else if (cause instanceof CredentialsExpiredException) {
            return new AuthenticationFailedRestException(MangoRestErrorCode.CREDENTIALS_EXPIRED,
                    new TranslatableMessage("rest.exception.credentialsExpired"),
                    cause);
        } else if (cause instanceof DisabledException) {
            return new AuthenticationFailedRestException(MangoRestErrorCode.ACCOUNT_DISABLED,
                    new TranslatableMessage("rest.exception.accountDisabled"),
                    cause);
        } else if (cause instanceof PasswordChangeException) {
            return new AuthenticationFailedRestException(MangoRestErrorCode.PASSWORD_CHANGE_FAILED,
                    ((PasswordChangeException) cause).getTranslatableMessage(),
                    cause);
        } else {
            return new AuthenticationFailedRestException(MangoRestErrorCode.GENERIC_AUTHENTICATION_FAILED,
                    new TranslatableMessage("rest.exception.genericAuthenticationFailed", cause.getClass().getSimpleName() + " " + cause.getMessage()),
                    cause);
        }
    }
}
