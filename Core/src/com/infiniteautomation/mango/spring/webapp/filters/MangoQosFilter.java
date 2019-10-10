/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.webapp.filters;

import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;

import org.eclipse.jetty.servlets.QoSFilter;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * https://www.eclipse.org/jetty/documentation/current/qos-filter.html
 * @author Jared Wiltshire
 */
@Lazy
@Component(MangoQosFilter.NAME)
@WebFilter(
        filterName = MangoQosFilter.NAME,
        asyncSupported = true,
        urlPatterns = {"/*"},
        initParams = {
                @WebInitParam(name = "managedAttr", value = "false"),
                @WebInitParam(name = "maxRequests", value = "${web.qos.maxRequests:10}"),
                @WebInitParam(name = "maxPriority", value = "3"),
                @WebInitParam(name = "waitMs", value = "${web.qos.waitMs:50}"),
                @WebInitParam(name = "suspendMs", value = "${web.qos.suspendMs:-1}")
        })
@Order(200)
public class MangoQosFilter extends QoSFilter {
    public static final String NAME = "mangoQosFilter";
}