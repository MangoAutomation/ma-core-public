/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.webapp.filters;

import javax.servlet.DispatcherType;
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
        urlPatterns = {"/*"},
        dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.ERROR, DispatcherType.ASYNC})
@Order(FilterOrder.SPRING_SECURITY)
public class MangoSpringSecurityDelegate extends DelegatingFilterProxy {
    private MangoSpringSecurityDelegate() {
        super(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME);
    }
}
