/*
 * Copyright (C) 2018 Infinite Automation Systems Inc. All rights reserved.
 */
package com.serotonin.m2m2.module;

import org.eclipse.jetty.servlet.ServletContextHandler;

/**
 * @author Jared Wiltshire
 */
abstract public class ContextConfigurerDefinition extends ModuleElementDefinition {
    public abstract void configureContext(ServletContextHandler context);
}
