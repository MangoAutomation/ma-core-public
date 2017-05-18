/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import org.springframework.security.access.AccessDeniedException;

import com.fasterxml.jackson.databind.JsonNode;
import com.infiniteautomation.mango.rest.v2.exception.ValidationFailedRestException;
import com.infiniteautomation.mango.rest.v2.model.RestValidationResult;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.util.timeout.SystemActionTask;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * This class proaction that can be actived via the REST system-action endpoint.
 * 
 * @author Terry Packer
 */
abstract public class SystemActionDefinition extends ModuleElementDefinition {

    /**
     * The reference key to the action.  Should be unique across all Modules and Mango Core
     * 
     * @return the reference key
     */
    abstract public String getKey();

    /**
     * Validate the inputs and create the Task with input that will be scheduled and run
     * @param input
     * @return
     */
    public SystemActionTask getTask(final User user, final JsonNode input) throws ValidationFailedRestException, AccessDeniedException{
    	this.hasTaskPermission(user);
    	this.validate(input);
    	return getTaskImpl(input);
    }
    
    /**
     * Check the permission to the task
     * @param user
     * @throws AccessDeniedException
     */
    protected void hasTaskPermission(User user) throws AccessDeniedException{
    	PermissionDefinition def = getPermissionDefinition();
    	if(!Permissions.hasPermission(user, SystemSettingsDao.getValue(def.getPermissionTypeName())))
    		throw new AccessDeniedException(new TranslatableMessage("permissions.accessDenied", user.getUsername(), new TranslatableMessage(def.getPermissionKey())).translate(Common.getTranslations()));
    }
    
    /**
     * Get the permission definition for this action
     * @return
     */
    protected PermissionDefinition getPermissionDefinition(){
    	return ModuleRegistry.getPermissionDefinition(getPermissionTypeName());
    }

    protected void validate(JsonNode input) throws ValidationFailedRestException {
    	RestValidationResult result = validateImpl(input);
    	if(result != null)
    		result.ensureValid();
    }
    
    /**
     * Get the TypeName of the permission definition
     * @return
     */
    abstract protected String getPermissionTypeName();
    
    /**
     * Return the Task configured with inputs
     * @param input
     * @return
     */
    abstract protected SystemActionTask getTaskImpl(final JsonNode input);

    /**
     * Validate the inputs for the task
     * @param input
     * @throws ValidationFailedRestException
     */
    abstract protected RestValidationResult validateImpl(final JsonNode input);

    
}
