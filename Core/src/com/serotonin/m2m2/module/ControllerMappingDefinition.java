/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module;

import org.springframework.web.servlet.mvc.Controller;

import com.serotonin.m2m2.module.UriMappingDefinition.Permission;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;

/**
 * @author Terry Packer
 *
 */
public abstract class ControllerMappingDefinition extends ModuleElementDefinition{
    
	/**
     * The user authority required to access the handler.
     * 
     * @return the required authority level.
     */
    abstract public Permission getPermission();

    /**
     * The URI path to which this controller responds. Required.
     * 
     * @return the controller's URI path.
     */
    abstract public String getPath();

    /**
     * An instance of the controller for the URL. Called once upon startup, so the instance must be reusable and thread
     * safe.
     * 
     * 
     * @return an instance of the URL handler
     */
    abstract public Controller getController();

	/**
	 * Override as needed when using CUSTOM permissions type
	 * 
	 * @param user
	 * @return
	 */
	public boolean hasCustomPermission(User user) throws PermissionException{
		return false;
	}
}
