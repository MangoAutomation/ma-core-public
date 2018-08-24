/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.rest.v2.exception;

import org.springframework.http.HttpStatus;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoPasswordAuthenticationProvider.AuthenticationRateException;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoPasswordAuthenticationProvider.IpAddressAuthenticationRateException;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoPasswordAuthenticationProvider.UsernameAuthenticationRateException;

/**
 * @author Jared Wiltshire
 */
public class RateLimitedRestException extends AbstractRestV2Exception {
    private static final long serialVersionUID = 1L;

    public RateLimitedRestException(MangoRestErrorCode code, TranslatableMessage message, Throwable cause) {
        super(HttpStatus.TOO_MANY_REQUESTS, code, message, cause);
    }

    public RateLimitedRestException(MangoRestErrorCode code, TranslatableMessage message) {
        super(HttpStatus.TOO_MANY_REQUESTS, code, message);
    }

    public static RateLimitedRestException restExceptionFor(AuthenticationRateException cause) {
        if (cause instanceof IpAddressAuthenticationRateException) {
            return new RateLimitedRestException(MangoRestErrorCode.IP_RATE_LIMITED,
                    new TranslatableMessage("rest.exception.authenticationIpRateLimited", ((IpAddressAuthenticationRateException) cause).getIp()),
                    cause);
        } else if (cause instanceof UsernameAuthenticationRateException) {
            return new RateLimitedRestException(MangoRestErrorCode.USER_RATE_LIMITED,
                    new TranslatableMessage("rest.exception.authenticationUsernameRateLimited", ((UsernameAuthenticationRateException) cause).getUsername()),
                    cause);
        } else {
            throw new IllegalArgumentException("Unknown AuthenticationRateException");
        }
    }
}
