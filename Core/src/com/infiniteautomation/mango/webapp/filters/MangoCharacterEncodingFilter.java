/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.webapp.filters;

import javax.servlet.DispatcherType;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.CharacterEncodingFilter;

/**
 * @author Jared Wiltshire
 */
@Component
@WebFilter(
        asyncSupported = true,
        urlPatterns = {"/rest/*"},
        initParams = {
                @WebInitParam(name = "encoding", value = "UTF-8"),
                @WebInitParam(name = "forceEncoding", value = "true")
        },
        dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.ASYNC})
public class MangoCharacterEncodingFilter extends CharacterEncodingFilter {

}
