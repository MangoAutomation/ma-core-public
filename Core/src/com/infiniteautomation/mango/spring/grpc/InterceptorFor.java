/*
 * Copyright (C) 2023 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.grpc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.grpc.BindableService;

/**
 * Annotation which can be applied to a {@link io.grpc.ServerInterceptor} in order to limit its scope to a set of
 * {@link BindableService}. Any interceptor which does not have this annotation will be applied globally to all services.
 *
 * @author Jared Wiltshire
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InterceptorFor {
    Class<? extends BindableService>[] value();
}
