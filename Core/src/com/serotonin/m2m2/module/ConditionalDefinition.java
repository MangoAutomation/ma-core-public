/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.module;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to conditionally load a ModuleElementDefinition only if a environment property is set to true
 *
 * @author Jared Wiltshire
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionalDefinition {
    /**
     * Defines the environment property to evaluate
     * @return env property name
     */
    String value() default "";

    String[] requireClasses() default {};

    /**
     * Determines if the ModuleElementDefinition is made available via the {@link ModuleRegistry}
     * @return true if enabled
     */
    boolean enabled() default true;
}