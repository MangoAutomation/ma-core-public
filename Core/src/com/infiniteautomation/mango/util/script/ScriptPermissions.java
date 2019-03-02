/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.util.script;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * Script Permissions Container, to replace com.serotonin.m2m2.rt.script.ScriptPermissions
 * @author Terry Packer
 *
 */
public class ScriptPermissions implements JsonSerializable, Serializable, PermissionHolder {

    private Set<String> permissionsSet;
    private String permissionHolderName; //Name for exception messages
    
    public ScriptPermissions() {
        this(new HashSet<>());
    }
    
    public ScriptPermissions(Set<String> permissionsSet) {
        this(permissionsSet, "script");
    }
    public ScriptPermissions(Set<String> permissionsSet, String permissionHolderName) {
        this.permissionsSet = permissionsSet == null ? new HashSet<>() : permissionsSet;
        this.permissionHolderName = permissionHolderName;
    }
    
    
    public ScriptPermissions(User user) {
        this.permissionsSet = new HashSet<>(user.getPermissionsSet());
        this.permissionHolderName = user.getPermissionHolderName();
    }


    public void validate(ProcessResult response, User user) {
        if (user == null) {
            response.addContextualMessage("scriptPermissions", "validate.invalidPermission", "No User Found");
            return;
        }

        if (user.hasAdminPermission() && permissionsSet != null) {
            //Clean spaces
            String clean = Permissions.implodePermissionGroups(permissionsSet);
            permissionsSet = Permissions.explodePermissionGroups(clean);
            return;
        }

        if(this.permissionsSet != null) {
            // If superadmin then fine or if not then only allow my groups
            if ((!this.permissionsSet.isEmpty()) && (!Permissions.hasAnyPermission(user, this.permissionsSet))) {
                Set<String> invalid = Permissions.findInvalidPermissions(user, this.permissionsSet);
                String notGranted = Permissions.implodePermissionGroups(invalid);
                response.addContextualMessage("scriptPermissions", "validate.invalidPermission", notGranted);
            }
        }
    }

    public void validate(ProcessResult response, User user, ScriptPermissions oldPermissions) {
        if (user.hasAdminPermission() && permissionsSet != null) {
            //Clean spaces
            String clean = Permissions.implodePermissionGroups(permissionsSet);
            permissionsSet = Permissions.explodePermissionGroups(clean);
            return;
        }

        Set<String> nonUserPre = Permissions.findInvalidPermissions(user, oldPermissions.getPermissions());
        Set<String> nonUserPost = Permissions.findInvalidPermissions(user, this.getPermissions());
        if (nonUserPre.size() != nonUserPost.size())
            response.addContextualMessage("scriptPermissions", "validate.invalidPermissionModification", user.getPermissions());
        else {
            for (String s : nonUserPre)
                if (!nonUserPost.contains(s))
                    response.addContextualMessage("scriptPermissions", "validate.invalidPermissionModification", user.getPermissions());
        }
    }

    @Override
    public String getPermissions() {
        if(permissionsSet != null)
            return Permissions.implodePermissionGroups(permissionsSet);
        else
            return null;
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
    public Set<String> getPermissionsSet() {
        return permissionsSet;
    }
    
    public void setPermissionsSet(Set<String> permissionsSet) {
        this.permissionsSet = permissionsSet;
    }
    
    private static final int version = 1;
    private static final long serialVersionUID = 1L;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeObject(permissionsSet);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();
        if(ver == 1)
            permissionsSet = (Set<String>) in.readObject();
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        throw new ShouldNeverHappenException("Permissions should be written out in the parent class as a String.");
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        throw new ShouldNeverHappenException("Permissions should be read in the parent class as a String.");
    }

}
