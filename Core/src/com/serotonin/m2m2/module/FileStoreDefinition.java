/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.timer.OneTimeTrigger;
import com.serotonin.timer.TimerTask;
import com.serotonin.timer.TimerTrigger;

/**
 * Define a file storage area within the filestore directory of the core
 *
 * @author Terry Packer
 */
public abstract class FileStoreDefinition extends ModuleElementDefinition {
    protected static final Log LOG = LogFactory.getLog(FileStoreDefinition.class);

    //Root directory within core
    public static final String ROOT = "filestore";
    public static final String FILE_STORE_LOCATION_ENV_PROPERTY = "filestore.location";

    private final LazyInitSupplier<PermissionService> permissionService = new LazyInitSupplier<>(() -> {
        return Common.getBean(PermissionService.class);
    });

    /**
     * The translation for the name of the store
     * @return
     */
    abstract public TranslatableMessage getStoreDescription();

    /**
     * The name of the store.  Should be unique across all Modules and Mango Core
     *
     * @return the store name
     */
    abstract public String getStoreName();


    /**
     * Get the TypeName of the read permission definition, return null to allow access to all (including unauthenticated / public users)
     * @return
     */
    abstract protected String getReadPermissionTypeName();

    /**
     * Prefer getting permissions directly if available
     * Return null to use getReadPermissionTypeName instead
     * @return
     */
    protected Set<Role> getReadRoles() {
        return null;
    }

    /**
     * Get the TypeName of the write permission definition, return null to allow access to all (including unauthenticated / public users)
     * @return
     */
    abstract protected String getWritePermissionTypeName();

    /**
     * Prefer getting permissions directly if available
     * Return null to use getWritePermissionTypeName instead
     * @return
     */
    protected Set<Role> getWriteRoles() {
        return null;
    }

    /**
     * Ensure that a User has read permission
     * @throws PermissionException
     */
    public void ensureStoreReadPermission(User user) {
        Set<Role> roles = getReadRoles();
        PermissionDefinition permission = ModuleRegistry.getPermissionDefinition(getReadPermissionTypeName());

        if (roles != null) {
            permissionService.get().ensureHasAnyRole(user, roles);
        } else if (permission != null) {
            permissionService.get().ensureHasAnyRole(user, permission.getRoles());
        }
    }

    /**
     * Ensure that a User has write permission
     * @throws PermissionException
     */
    public void ensureStoreWritePermission(User user) {
        Set<Role> roles = getWriteRoles();
        PermissionDefinition permission = ModuleRegistry.getPermissionDefinition(getWritePermissionTypeName());

        if (roles != null) {
            permissionService.get().ensureHasAnyRole(user, roles);
        } else if (permission != null) {
            permissionService.get().ensureHasAnyRole(user, permission.getRoles());
        }
    }

    /**
     * Get the root of this filestore
     * @return
     * @throws IOException
     */
    public Path getRootPath() {
        String location = Common.envProps.getString(FILE_STORE_LOCATION_ENV_PROPERTY, ROOT);
        return Common.MA_HOME_PATH.resolve(location).resolve(getStoreName());
    }

    public File getRoot() {
        return getRootPath().toFile();
    }

    public void ensureExists() throws IOException {
        Files.createDirectories(getRootPath());
    }
}
