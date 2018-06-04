/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.User;

/**
 * @author Jared Wiltshire
 */
public class RateLimitingFilter extends OncePerRequestFilter {
    private final Log log = LogFactory.getLog(RateLimitingFilter.class);
    private final RateLimiter<String> ipRateLimiter;
    private final RateLimiter<Integer> userRateLimiter;
    private final int multiplier;

    public RateLimitingFilter(RateLimiter<String> ipRateLimiter, RateLimiter<Integer> userRateLimiter) {
        this(ipRateLimiter, userRateLimiter, 1);
    }

    public RateLimitingFilter(RateLimiter<String> ipRateLimiter, RateLimiter<Integer> userRateLimiter, int multiplier) {
        this.ipRateLimiter = ipRateLimiter;
        this.userRateLimiter = userRateLimiter;
        this.multiplier = multiplier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean rateExceeded = false;
        long secondsTillRetry = 0;
        boolean anonymous = authentication == null || authentication instanceof AnonymousAuthenticationToken;

        if (anonymous && this.ipRateLimiter != null) {
            // we dont check the X-FORWARDED-FOR header as this can be easily spoofed to bypass the rate limits
            String ip = request.getRemoteAddr();
            rateExceeded = this.ipRateLimiter.checkRateExceeded(ip, multiplier);
            secondsTillRetry = this.ipRateLimiter.secondsTillRetry(ip);

            if (rateExceeded && log.isDebugEnabled()) {
                log.debug("Non-authenticated IP address " + ip + " exceeded rate limit of " + this.ipRateLimiter.getRefillQuanitity() + " requests per " + this.ipRateLimiter.getRefillPeriod() + " " + this.ipRateLimiter.getRefillPeriodUnit());
            }
        } else if (!anonymous && this.userRateLimiter != null) {
            User user = (User) authentication.getPrincipal();
            rateExceeded = this.userRateLimiter.checkRateExceeded(user.getId(), multiplier);
            secondsTillRetry = this.userRateLimiter.secondsTillRetry(user.getId());

            if (rateExceeded && log.isDebugEnabled()) {
                log.debug("User " + user.getUsername() + " exceeded rate limit of " + this.userRateLimiter.getRefillQuanitity() + " requests per " + this.userRateLimiter.getRefillPeriod() + " " + this.userRateLimiter.getRefillPeriodUnit());
            }
        }

        if (rateExceeded) {
            TranslatableMessage message = new TranslatableMessage("rest.exception.rateLimited");

            response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(secondsTillRetry));
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), message.translate(Common.getTranslations()));
        } else {
            filterChain.doFilter(request, response);
        }
    }

}
