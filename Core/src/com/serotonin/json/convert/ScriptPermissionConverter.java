/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.json.convert;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.script.ScriptPermissions;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonTypeWriter;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.role.Role;

/**
 * Uses either the legacy form of ScriptPermissions or the new version that contains a Set<Set<String>> like a MangoPermission
 * @author Terry Packer
 */
public class ScriptPermissionConverter extends ImmutableClassConverter {

    private static final String DATA_SOURCE = "dataSourcePermissions";
    private static final String DATA_POINT_SET = "dataPointSetPermissions";
    private static final String DATA_POINT_READ = "dataPointReadPermissions";
    private static final String CUSTOM = "customPermissions";

    @Override
    public JsonValue jsonWrite(JsonTypeWriter writer, Object value) throws JsonException {
        ScriptPermissions permission = (ScriptPermissions)value;
        JsonArray roles = new JsonArray();
        for(Role role : permission.getRoles()) {
            roles.add(role.getXid());
        }
        return roles;
    }

    @Override
    public void jsonWrite(JsonWriter writer, Object value) throws IOException, JsonException {
        ScriptPermissions permission = (ScriptPermissions)value;
        JsonArray roles = new JsonArray();
        for(Role role : permission.getRoles()) {
            roles.add(role.getXid());
        }
        writer.writeObject(roles);
    }

    //TODO Mango 4.2 improve performance with lazy field as PermissionService is not available at construct time
    @Override
    public Object jsonRead(JsonReader reader, JsonValue jsonValue, Type type) throws JsonException {
        Set<Role> roles = new HashSet<>();
        PermissionService service = Common.getBean(PermissionService.class);
        if(jsonValue instanceof JsonArray) {
            for(JsonValue val : (JsonArray)jsonValue) {
                //Just a single string
                Role r = service.getRole(val.toString());
                if(r != null) {
                    roles.add(r);
                }else {
                    //Let the validation pick this up as a missing role, the response to the user is cleaner
                    roles.add(new Role(Common.NEW_ID, val.toString()));
                }
            }
        }else if(jsonValue instanceof JsonObject) {
            //Could be the super-legacy version with 3 separate sets of roles
            JsonObject o = (JsonObject) jsonValue;
            Set<String> permissions = new HashSet<>();
            permissions.addAll(PermissionService.explodeLegacyPermissionGroups(o.getString(DATA_SOURCE)));
            permissions.addAll(PermissionService.explodeLegacyPermissionGroups(o.getString(DATA_POINT_SET)));
            permissions.addAll(PermissionService.explodeLegacyPermissionGroups(o.getString(DATA_POINT_READ)));
            permissions.addAll(PermissionService.explodeLegacyPermissionGroups(o.getString(CUSTOM)));
            for(String role : permissions) {
                Role r = service.getRole(role);
                if(r != null) {
                    roles.add(r);
                }else {
                    //Let the validation pick this up as a missing role, the response to the user is cleaner
                    roles.add(new Role(Common.NEW_ID, role));
                }
            }
        }
        return new ScriptPermissions(roles);
    }

}
