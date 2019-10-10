/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.webapp.filters;

import javax.servlet.annotation.WebFilter;

import org.springframework.core.annotation.Order;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.DelegatingFilterProxy;


/**
 * Delegates to an instance of FilterChainProxy with bean name "springSecurityFilterChain" created by Spring Security
 * @author Jared Wiltshire
 */
@Component
@WebFilter(
        filterName = AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME,
        asyncSupported = true,
        urlPatterns = {"/*"})
@Order(0)
public class MangoSpringSecurityDelegate extends DelegatingFilterProxy {

    private MangoSpringSecurityDelegate() {
        super(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME);
    }
}
