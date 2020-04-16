/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

import com.fasterxml.jackson.databind.JsonNode;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.util.timeout.SystemActionTask;
import com.serotonin.m2m2.vo.User;

/**
 * This class proaction that can be actived via the REST system-action endpoint.
 *
 * @author Terry Packer
 */
/* See SystemActionTemporaryResourceManager and Implement these as single rest controllers inside modules */
@Deprecated
abstract public class SystemActionDefinition extends ModuleElementDefinition {

    @Autowired
    private PermissionService service;

    /**
     * The reference key to the action. Should be unique across all Modules and Mango Core
     *
     * @return the reference key
     */
    abstract public String getKey();

    /**
     * Validate the inputs and create the Task with input that will be scheduled and run
     *
     * @param input
     * @return
     */
    public SystemActionTask getTask(final User user, final JsonNode input)
            throws ValidationException, AccessDeniedException {
        this.hasTaskPermission(user);
        this.validate(input);
        return getTaskImpl(input);
    }

    /**
     * Check the permission to the task
     *
     * @param user
     * @throws AccessDeniedException
     */
    protected void hasTaskPermission(User user) throws AccessDeniedException {
        PermissionDefinition permission = getPermissionDefinition();
        if(permission == null)
            return;
        service.hasPermission(user, permission.getPermission());
    }

    /**
     * Get the permission definition for this action
     *
     * @return
     */
    protected PermissionDefinition getPermissionDefinition() {
        return ModuleRegistry.getPermissionDefinition(getPermissionTypeName());
    }

    /**
     * Get the TypeName of the permission definition
     *
     * @return
     */
    abstract protected String getPermissionTypeName();

    /**
     * Return the Task configured with inputs
     *
     * @param input
     * @return
     */
    abstract protected SystemActionTask getTaskImpl(final JsonNode input);

    /**
     * Validate the inputs for the task
     *
     * @param input
     * @throws ValidationException
     */
    abstract protected void validate(final JsonNode input) throws ValidationException;


}
