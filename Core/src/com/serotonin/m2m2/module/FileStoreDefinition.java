/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StreamUtils;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.Permissions;
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
    protected String getReadPermissions() {
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
    protected String getWritePermissions() {
        return null;
    }

    /**
     * Ensure that a User has read permission
     * @throws PermissionException
     */
    public void ensureStoreReadPermission(User user) {
        String roles = getReadPermissions();
        String permissionName = getReadPermissionTypeName();

        if (roles != null) {
            Permissions.ensureHasAnyPermission(user, Permissions.explodePermissionGroups(roles));
        } else if (permissionName != null) {
            Permissions.ensureGrantedPermission(user, permissionName);
        }
    }

    /**
     * Ensure that a User has read permission
     * @throws PermissionException
     */
    public void ensureStoreWritePermission(User user) {
        String roles = getWritePermissions();
        String permissionName = getWritePermissionTypeName();

        if (roles != null) {
            Permissions.ensureHasAnyPermission(user, Permissions.explodePermissionGroups(roles));
        } else if (permissionName != null) {
            Permissions.ensureGrantedPermission(user, permissionName);
        }
    }

    /**
     * Get the root of this filestore
     * @return
     */
    public File getRoot() {
        String location = Common.envProps.getString(FILE_STORE_LOCATION_ENV_PROPERTY);
        if (location == null || location.isEmpty()) {
            location = ROOT;
        }
        return Paths.get(Common.MA_HOME).resolve(location).resolve(getStoreName()).toFile();
    }
    /**
     * Get an array of files to move into the store when the store is created
     * @return
     */
    public File[] getInitialFileStoreContents() {
        return new File[0];
    }

    public void ensureExists() {
        File fsRoot = getRoot();
        if(!fsRoot.exists())
            Common.timer.schedule(new FileStoreCreatorTask(new OneTimeTrigger(0), "Create filestore " + getStoreName(), this));
    }

    private class FileStoreCreatorTask extends TimerTask {
        private final FileStoreDefinition def;
        /**
         * @param trigger
         * @param name
         */
        public FileStoreCreatorTask(TimerTrigger trigger, String name, FileStoreDefinition def) {
            super(trigger, name);
            this.def = def;
        }

        /* (non-Javadoc)
         * @see com.serotonin.timer.Task#run(long)
         */
        @Override
        public void run(long runtime) {
            File fsRoot = def.getRoot();
            if(!fsRoot.exists())
                fsRoot.mkdir();

            for(File f : def.getInitialFileStoreContents()) {
                if(f.exists()) {
                    File newFile = new File(fsRoot, f.getName());
                    if(!newFile.exists()) {
                        try (FileInputStream fis = new FileInputStream(f)) {
                            try(FileOutputStream fos = new FileOutputStream(newFile, false)) {
                                StreamUtils.copy(fis, fos);
                            }
                        }catch(IOException e) {
                            LOG.warn("Failed to copy default file into filestore: " + getStoreName() + " " + f.getName(), e);
                        }
                    }
                }
            }
        }

    }

}
