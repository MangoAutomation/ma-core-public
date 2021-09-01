/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Hold dependencies passed through to {@link AbstractBasicVOService} and {@link AbstractVOService} constructors.
 * Enables easy addition of dependencies without changing constructor signatures.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public final class ServiceDependencies {

    private final PermissionService permissionService;
    private final Environment environment;
    private final ExecutorService executorService;

    @Autowired
    public ServiceDependencies(PermissionService permissionService, Environment environment, ExecutorService executorService) {
        this.permissionService = permissionService;
        this.environment = environment;
        this.executorService = executorService;
    }

    public PermissionService getPermissionService() {
        return permissionService;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }
}
