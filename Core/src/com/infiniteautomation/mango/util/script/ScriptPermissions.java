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
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
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
        this(Collections.emptySet());
    }

    public ScriptPermissions(Set<String> permissionsSet) {
        this(permissionsSet, "script");
    }

    public ScriptPermissions(User user) {
        this(user.getPermissionsSet(), user.getPermissionHolderName());
    }

    public ScriptPermissions(Set<String> permissionsSet, String permissionHolderName) {
        if (permissionsSet != null) {
            Set<String> permissions = permissionsSet.stream().map(p -> p.trim()).collect(Collectors.toSet());
            this.permissionsSet = Collections.unmodifiableSet(permissions);
        } else {
            this.permissionsSet = Collections.unmodifiableSet(Collections.emptySet());
        }

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
    public Set<String> getPermissionsSet() {
        return permissionsSet;
    }

    @Deprecated
    public String getPermissions() {
        return Permissions.implodePermissionGroups(this.getPermissionsSet());
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

    /**
     * Write the script permissions as a Set<String>
     * @param writer
     * @param scriptPermissions
     * @throws IOException
     * @throws JsonException
     */
    public static void writeJsonSafely(ObjectWriter writer, ScriptPermissions scriptPermissions) throws IOException, JsonException {
        writer.writeEntry("scriptPermissions", scriptPermissions == null ? null : scriptPermissions.getPermissionsSet());
    }

    /**
     * Safely read legacy and new ScriptPermissions
     * @param jsonObject
     * @return
     */
    public static ScriptPermissions readJsonSafely(JsonObject jsonObject) {
        if(jsonObject.containsKey("scriptPermissions")) {
            Set<String> permissions = null;
            try{
                JsonObject o = jsonObject.getJsonObject("scriptPermissions");
                permissions = new HashSet<>();
                permissions.addAll(Permissions.explodePermissionGroups(o.getString("dataSourcePermissions")));
                permissions.addAll(Permissions.explodePermissionGroups(o.getString("dataPointSetPermissions")));
                permissions.addAll(Permissions.explodePermissionGroups(o.getString("dataPointReadPermissions")));
                permissions.addAll(Permissions.explodePermissionGroups(o.getString("customPermissions")));
                return new ScriptPermissions(permissions);
            }catch(ClassCastException e) {
                //Munchy munch, not a legacy script permissions object
            }
            if(permissions == null) {
                Set<String> roles = new HashSet<>();
                JsonArray array = jsonObject.getJsonArray("scriptPermissions");
                for(JsonValue o : array)
                    roles.add(o.toString());
                return new ScriptPermissions(roles);
            }else
                return new ScriptPermissions();
        }else
            return new ScriptPermissions();
    }
}
