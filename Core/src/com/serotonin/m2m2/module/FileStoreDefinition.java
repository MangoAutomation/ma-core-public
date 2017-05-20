/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module;

import java.io.File;

import org.springframework.security.access.AccessDeniedException;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * Define a file storage area within the filestore directory of the core
 * 
 * @author Terry Packer
 */
public abstract class FileStoreDefinition extends ModuleElementDefinition {
	
	//Root directory within core
	private static final String ROOT = "filestore";
	
    /**
     * The name of the store.  Should be unique across all Modules and Mango Core
     * 
     * @return the store name
     */
    abstract public String getStoreName();

    
    /**
     * Get the TypeName of the read permission definition
     * @return
     */
    abstract protected String getReadPermissionTypeName();
    
    /**
     * Get the TypeName of the write permission definition
     * @return
     */
    abstract protected String getWritePermissionTypeName();
    
    
    /**
     * Ensure that a User has read permission
     * @throws PermissionException
     */
    public void ensureStoreReadPermission(User user) throws AccessDeniedException{
    	PermissionDefinition def = ModuleRegistry.getPermissionDefinition(getReadPermissionTypeName());
    	if(def == null)
    		return;
    	if(!Permissions.hasPermission(user, SystemSettingsDao.getValue(def.getPermissionTypeName())))
    		throw new AccessDeniedException(new TranslatableMessage("permissions.accessDenied", user.getUsername(), new TranslatableMessage(def.getPermissionKey())).translate(Common.getTranslations()));
    }

    /**
     * Ensure that a User has read permission
     * @throws PermissionException
     */
    public void ensureStoreWritePermission(User user) throws AccessDeniedException{
    	PermissionDefinition def = ModuleRegistry.getPermissionDefinition(getWritePermissionTypeName());
    	if(def == null)
    		return;
    	if(!Permissions.hasPermission(user, SystemSettingsDao.getValue(def.getPermissionTypeName())))
    		throw new AccessDeniedException(new TranslatableMessage("permissions.accessDenied", user.getUsername(), new TranslatableMessage(def.getPermissionKey())).translate(Common.getTranslations()));
    }

    /**
     * Get the root of this filestore
     * @return
     */
	public File getRoot(){
		return new File(new File(Common.MA_HOME, ROOT), getStoreName());
	}
    
}
