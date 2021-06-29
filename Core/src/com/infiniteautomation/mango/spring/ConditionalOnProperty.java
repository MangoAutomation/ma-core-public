/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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
 * Provides a way to conditionally enable a Spring component/service based on a property (env.properties, environment variable, Java system property)
 *
 * @author Jared Wiltshire
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(ConditionalOnPropertyCondition.class)
public @interface ConditionalOnProperty {
    /**
     * @return Values are resolved via the Spring {@link org.springframework.core.env.PropertyResolver#resolveRequiredPlaceholders(String) PropertyResolver}
     * and parsed as boolean values. If multiple values are specified they must all evaluate to true in order to enable the component.<br>
     * e.g. <code>${testing.enabled:false}</code>
     */
    String[] value();

    public static class ConditionalOnPropertyCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            try {
                Map<String, Object> annotationProperties = metadata.getAnnotationAttributes(ConditionalOnProperty.class.getName());
                String[] values = (String[]) annotationProperties.get("value");

                for (String value : values) {
                    String result = context.getEnvironment().resolveRequiredPlaceholders(value);
                    if (!Boolean.parseBoolean(result)) {
                        return false;
                    }
                }

                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
