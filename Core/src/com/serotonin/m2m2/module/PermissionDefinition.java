/*
Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
@author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import java.util.Collections;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.github.zafarkhaja.semver.Version;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.service.RoleService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;

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

    @Autowired
    protected UserDao userDao;

    private final LazyInitSupplier<RoleService> roleService = new LazyInitSupplier<>(() -> {
        return Common.getBean(RoleService.class);
    });

    //TODO Mango 4.0 is this the ideal data structure or should be be using sync blocks?
    protected MangoPermission permission;

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
    protected Set<Set<Role>> getDefaultRoles(){
        return Collections.emptySet();
    }

    /**
     * Get the permission with current roles filled in
     * @return
     */
    public MangoPermission getPermission() {
        return permission;
    }

    @Override
    public void postDatabase(Version previousVersion, Version current) {
        //Install our default roles if there are none in the database
        this.permission = roleService.get().getDao().getPermission(getPermissionTypeName());
        if(previousVersion == null) {
            if(permission.getRoles().isEmpty()) {
                this.permission = new MangoPermission(getDefaultRoles());
                roleService.get().getDao().replaceRolesOnPermission(this.permission, getPermissionTypeName());
            }
        }
    }

    /**
     * Does this holder have any roles assigned to this permission?
     * @param holder
     * @return
     */
    public boolean hasPermission(PermissionHolder holder) {
        return this.roleService.get().getPermissionService().hasPermission(holder, permission);
    }

    /**
     * Replace the roles on this permission.  Throws validation exeption if xids DNE
     *
     * @param roles
     */
    public void update(MangoPermission permission) throws ValidationException {
        //TODO Mango 4.0 Transaction rollback etc?
        this.roleService.get().replaceAllRolesOnPermission(permission, this);
        this.permission = permission;
        //notify user cache
        this.userDao.permissionChanged();
    }

}
