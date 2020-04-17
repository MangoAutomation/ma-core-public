/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.module;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to conditionally load a ModuleElementDefinition only if a environment propery is set to true
 *
 * @author Jared Wiltshire
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionalDefinition {
    /**
     * Defines the environment property to evaluate
     * @return
     */
    String value() default "";

    /**
     * If set to false the module defintion will never be autoloaded
     * @return
     */
    boolean autoLoad() default true;
}