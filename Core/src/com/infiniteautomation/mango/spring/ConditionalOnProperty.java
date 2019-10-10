/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;

import com.infiniteautomation.mango.spring.ConditionalOnProperty.ConditionalOnPropertyCondition;

/**
 * @author Jared Wiltshire
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(ConditionalOnPropertyCondition.class)
public @interface ConditionalOnProperty {
    String value();

    public static class ConditionalOnPropertyCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            try {
                Map<String, Object> annotationProperties = metadata.getAnnotationAttributes(ConditionalOnProperty.class.getName());
                String value = (String) annotationProperties.get("value");
                String result = context.getEnvironment().resolveRequiredPlaceholders(value);
                return Boolean.parseBoolean(result);
            } catch (Exception e) {
                return false;
            }
        }
    }
}
