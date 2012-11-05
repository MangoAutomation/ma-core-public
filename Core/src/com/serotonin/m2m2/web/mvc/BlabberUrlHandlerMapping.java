/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.mvc;

import org.springframework.beans.BeansException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

public class BlabberUrlHandlerMapping extends SimpleUrlHandlerMapping {
    @Override
    public void registerHandler(String urlPath, Object handler) throws BeansException, IllegalStateException {
        super.registerHandler(urlPath, handler);
    }

    public void addInterceptor(HandlerInterceptor interceptor) {
        setInterceptors(new Object[] { interceptor });
    }

    @Override
    public void initInterceptors() {
        super.initInterceptors();
    }
}
