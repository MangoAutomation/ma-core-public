/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.util.script;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.permission.MangoPermission;
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

    private Set<Role> roles;
    private final String permissionHolderName; //Name for exception messages

    public ScriptPermissions() {
        this(Collections.emptySet());
    }

    public ScriptPermissions(MangoPermission permission) {
        //Always an outer OR group
        this(permission.getUniqueRoles());
    }

    public ScriptPermissions(Set<Role> permissionsSet) {
        this(permissionsSet, "script");
    }

    public ScriptPermissions(User user) {
        this(user.getRoles(), user.getPermissionHolderName());
    }

    public ScriptPermissions(Set<Role> roles, String permissionHolderName) {
        if (roles != null) {
            this.roles = roles;
        } else {
            this.roles = Collections.unmodifiableSet(Collections.emptySet());
        }
        this.permissionHolderName = permissionHolderName;
    }

    public MangoPermission getPermission() {
        //These represent a the roles the script runs on and as such will always be a Set of singleton roles
        Set<Set<Role>> orSet = new HashSet<>();
        for(Role role : roles) {
            orSet.add(Collections.singleton(role));
        }
        return new MangoPermission(orSet);
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
    public Set<Role> getAllInheritedRoles() {
        return roles;
    }

    private static final int version = 2;
    private static final long serialVersionUID = 1L;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();
        if(ver == 1) {
            PermissionService service = Common.getBean(PermissionService.class);
            MangoPermission permission = service.upgradePermissions((Set<String>) in.readObject());
            roles = permission.getUniqueRoles();
        }else if(ver == 2) {
            //Nada
        }
    }
}
