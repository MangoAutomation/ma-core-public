/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.module;

import javax.servlet.ServletContext;

import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * @author Terry Packer
 *
 */
public abstract class ApplicationContextDefinition extends ModuleElementDefinition {

    /**
     * 
     * Allow a module to define an application context
     * 
     * @param context
     * @param rootWebContext
     */
    abstract public void configure(ServletContext context,
            AnnotationConfigWebApplicationContext rootWebContext);
    
}
