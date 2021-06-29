/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util.script;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;

/**
 * TODO Mango 4.0 re-think if we want these in the mapping table
 *
 * More aptly named ScriptPermissionHolder but since this has been serialized into the DB it is what it is
 *
 * Script Permissions Container, to replace com.serotonin.m2m2.rt.script.ScriptPermissions
 * @author Terry Packer
 *
 */
public class ScriptPermissions implements Serializable, PermissionHolder {

    private final String permissionHolderName; //Name for exception messages

    private Set<Role> roles;

    public ScriptPermissions() {
        this(Collections.emptySet());
    }

    public ScriptPermissions(Set<Role> roles) {
        this(roles, "script");
    }

    public ScriptPermissions(User user) {
        this(user.getRoles(), user.getPermissionHolderName());
    }

    public ScriptPermissions(Set<Role> roles, String permissionHolderName) {
        this.roles = Collections.unmodifiableSet(roles != null ? roles : Collections.emptySet());
        this.permissionHolderName = permissionHolderName;
    }

    @Override
    public String getPermissionHolderName() {
        if(StringUtils.isEmpty(permissionHolderName))
            return "JavaScript";
        else
            return "JavaScript saved by: " + permissionHolderName;
    }

    @Override
    public boolean isPermissionHolderDisabled() {
        return false;
    }

    @Override
    public Set<Role> getRoles() {
        return roles;
    }

    private static final int version = 2;
    private static final long serialVersionUID = 1L;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeObject(roles);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();
        if(ver == 1) {
            PermissionService service = Common.getBean(PermissionService.class);
            this.roles = Collections.unmodifiableSet(service.upgradeScriptRoles((Set<String>) in.readObject()));
        }else if(ver == 2){
            //Will be cleaned in the load relational data method
            this.roles = Collections.unmodifiableSet((Set<Role>)in.readObject());
        }
    }
}
