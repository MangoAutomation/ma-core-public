/*
Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
@author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import org.springframework.context.event.EventListener;

import com.github.zafarkhaja.semver.Version;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemPermissionDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * A permission definition allows a module to define a single permission string. The enforcement of this permission is
 * the responsibility of the module itself. The core will present a text box on the system settings permissions page
 * to allow for the editing of the permission string.
 *
 * The permission string value will be stored in the system settings table using the permission type name as the key.
 *
 * @author Matthew Lohbihler
 */
abstract public class PermissionDefinition extends ModuleElementDefinition {

    public static final PermissionGroup SCRIPTING_ENGINES_GROUP = new PermissionGroupImpl("scriptingEngines",
            new TranslatableMessage("permission.groups.scriptingEngines.title"),
            new TranslatableMessage("permission.groups.scriptingEngines.description"));
    public static final PermissionGroup USERS_GROUP = new PermissionGroupImpl("users",
            new TranslatableMessage("permission.groups.users.title"),
            new TranslatableMessage("permission.groups.users.description"));

    protected volatile MangoPermission permission;

    /**
     * A  human readable and translatable brief description of the permission.
     *  Descriptions are used in the system settings permission section and so should be as brief
     *  as possible.
     *
     * @return the reference key to the permission description.
     */
    abstract public TranslatableMessage getDescription();

    /**
     * An internal identifier for this type of permission. Must be unique within an MA instance, and is recommended
     * to have the form "&lt;moduleName&gt;.&lt;permissionName&gt;" so as to be unique across all modules.
     *
     * This will be used in the system settings table to store the permission's groups
     *
     * @return the permission type name.
     */
    abstract public String getPermissionTypeName();

    /**
     * Offers the implementer the option to add default roles to the permission when the module is upgraded
     * or installed.  The roles must already exist in the roles table
     * @return - Set of roles to assign to permission
     */
    protected MangoPermission getDefaultPermission() {
        return MangoPermission.superadminOnly();
    }

    /**
     * Get the permission with current roles filled in
     */
    public MangoPermission getPermission() {
        return permission;
    }

    public void setPermission(MangoPermission permission) {
        this.permission = permission;
    }

    @Override
    public void postDatabase(Version previousVersion, Version current) {

        //Can't autowire this due to circular dependencies with the PermissionService
        SystemPermissionDao systemPermissionDao = Common.getBean(SystemPermissionDao.class);

        //Install our default roles if there are none in the database
        this.permission = systemPermissionDao.get(getPermissionTypeName());
        if(this.permission == null) {
            this.permission =  getDefaultPermission();
            systemPermissionDao.insert(getPermissionTypeName(), this.permission);
        }
    }

    @EventListener
    protected void handleEvent(DaoEvent<? extends RoleVO> event) {
        switch(event.getType()) {
            case DELETE:
                this.permission = this.permission.withoutRole(event.getVo().getRole());
                break;
            default:
                break;
        }
    }

    /**
     * Logical group for permission, used for sorting into groups on the UI.
     * Default implementation returns module name and description.
     *
     * @return group for this permission
     */
    public PermissionGroup getGroup() {
        return new ModulePermissionGroup();
    }

    public class ModulePermissionGroup implements PermissionGroup {
        @Override
        public String getName() {
            return getModule().getName();
        }
        @Override
        public TranslatableMessage getTitle() {
            return new TranslatableMessage("permission.groups.module.title", getModule().getName());
        }
        @Override
        public TranslatableMessage getDescription() {
            return getModule().getDescription();
        }
    }
}
