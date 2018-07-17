/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module;

import java.io.File;
import java.nio.file.Paths;

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
    public static final String ROOT = "filestore";
    // TODO Mango 3.5 rename, not system setting, this is an env prop
    public static final String FILE_STORE_LOCATION_SYSTEM_SETTING = "filestore.location";

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
        if(!Permissions.hasPermission(user, SystemSettingsDao.instance.getValue(def.getPermissionTypeName())))
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
        if(!Permissions.hasPermission(user, SystemSettingsDao.instance.getValue(def.getPermissionTypeName())))
            throw new AccessDeniedException(new TranslatableMessage("permissions.accessDenied", user.getUsername(), new TranslatableMessage(def.getPermissionKey())).translate(Common.getTranslations()));
    }

    /**
     * Get the root of this filestore
     * @return
     */
    public File getRoot() {
        String location = Common.envProps.getString(FILE_STORE_LOCATION_SYSTEM_SETTING);
        if (location == null || location.isEmpty()) {
            location = ROOT;
        }
        return Paths.get(Common.MA_HOME).resolve(location).resolve(getStoreName()).toFile();
    }
}
