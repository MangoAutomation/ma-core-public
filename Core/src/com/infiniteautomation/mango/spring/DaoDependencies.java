/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.dao.AbstractBasicDao;
import com.serotonin.m2m2.db.dao.AbstractVoDao;

/**
 * Hold dependencies passed through to {@link AbstractBasicDao} and {@link AbstractVoDao} constructors.
 * Enables easy addition of dependencies without changing constructor signatures.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public final class DaoDependencies {

    private final DatabaseProxy databaseProxy;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final PermissionService permissionService;

    @Autowired
    public DaoDependencies(DatabaseProxy databaseProxy,
                           @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME) ObjectMapper objectMapper,
                           ApplicationEventPublisher eventPublisher,
                           PermissionService permissionService) {
        this.databaseProxy = databaseProxy;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.permissionService = permissionService;
    }

    public DatabaseProxy getDatabaseProxy() {
        return databaseProxy;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public ApplicationEventPublisher getEventPublisher() {
        return eventPublisher;
    }

    public PermissionService getPermissionService() {
        return permissionService;
    }
}
