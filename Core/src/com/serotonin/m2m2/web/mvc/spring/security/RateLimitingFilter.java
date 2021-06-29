/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Jared Wiltshire
 */
public class RateLimitingFilter extends OncePerRequestFilter {
    private final Log log = LogFactory.getLog(RateLimitingFilter.class);
    private final RequestMatcher requestMatcher;
    private final RateLimiter<String> ipRateLimiter;
    private final RateLimiter<Integer> userRateLimiter;
    private final int multiplier;

    public RateLimitingFilter(RequestMatcher requestMatcher, RateLimiter<String> ipRateLimiter, RateLimiter<Integer> userRateLimiter) {
        this(requestMatcher, ipRateLimiter, userRateLimiter, 1);
    }

    public RateLimitingFilter(RequestMatcher requestMatcher, RateLimiter<String> ipRateLimiter, RateLimiter<Integer> userRateLimiter, int multiplier) {
        this.requestMatcher = requestMatcher;
        this.ipRateLimiter = ipRateLimiter;
        this.userRateLimiter = userRateLimiter;
        this.multiplier = multiplier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!requestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean rateExceeded = false;
        long secondsTillRetry = 0;

        User user = null;
        if (authentication != null && authentication.getPrincipal() instanceof PermissionHolder) {
            user = ((PermissionHolder) authentication.getPrincipal()).getUser();
        }

        if (user == null && this.ipRateLimiter != null) {
            // we dont check the X-FORWARDED-FOR header by default as this can be easily spoofed to bypass the rate limits
            String ip = request.getRemoteAddr();
            rateExceeded = this.ipRateLimiter.hit(ip, multiplier);
            secondsTillRetry = this.ipRateLimiter.secondsTillRetry(ip);

            if (rateExceeded && log.isDebugEnabled()) {
                log.debug("Non-authenticated IP address " + ip + " exceeded rate limit of " + this.ipRateLimiter.getRefillQuanitity() + " requests per " + this.ipRateLimiter.getRefillPeriod() + " " + this.ipRateLimiter.getRefillPeriodUnit());
            }
        } else if (user != null && this.userRateLimiter != null) {
            rateExceeded = this.userRateLimiter.hit(user.getId(), multiplier);
            secondsTillRetry = this.userRateLimiter.secondsTillRetry(user.getId());

            if (rateExceeded && log.isDebugEnabled()) {
                log.debug("User " + user.getUsername() + " exceeded rate limit of " + this.userRateLimiter.getRefillQuanitity() + " requests per " + this.userRateLimiter.getRefillPeriod() + " " + this.userRateLimiter.getRefillPeriodUnit());
            }
        }

        if (rateExceeded) {
            TranslatableMessage message = new TranslatableMessage("rest.exception.rateLimited");

            response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(secondsTillRetry));

            // TODO Mango 4.0 escapes from Spring and is not caught by exception handler
            // throw new RateLimitedRestException(MangoRestErrorCode.IP_RATE_LIMITED, message);

            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), message.translate(Common.getTranslations()));
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
