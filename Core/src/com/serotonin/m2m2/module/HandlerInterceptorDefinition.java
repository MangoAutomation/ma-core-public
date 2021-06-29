/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module;

import org.springframework.web.servlet.HandlerInterceptor;

/**
 * A handler interceptor definition intercepts the processing of a page at multiple points - pre-handle, post-handle,
 * and after completion - allowing arbitrary treatment of the request at those times.
 * 
 * @author Matthew Lohbihler
 */
abstract public class HandlerInterceptorDefinition extends ModuleElementDefinition {
    /**
     * An instance of the handler interceptor. Called once upon startup, so the instance must be reusable and thread
     * safe. Cannot be null.
     * 
     * @return an instance of the handler interceptor
     */
    abstract public HandlerInterceptor getInterceptor();
}
