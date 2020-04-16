/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
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
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

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

    //TODO Mango 4.0 improve performance as role dao is not available at construct time
    @Override
    public Object jsonRead(JsonReader reader, JsonValue jsonValue, Type type) throws JsonException {
        Set<Role> roles = new HashSet<>();
        if(jsonValue instanceof JsonArray) {
            for(JsonValue val : (JsonArray)jsonValue) {
                //Just a single string
                RoleVO r = RoleDao.getInstance().getByXid(val.toString());
                if(r != null) {
                    roles.add(r.getRole());
                }else {
                    //TODO Mango 4.0 use this? throw new TranslatableJsonException("emport.error.missingRole", permission, "scriptPermissions");
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
                RoleVO r = RoleDao.getInstance().getByXid(role);
                if(r != null) {
                    roles.add(r.getRole());
                }else {
                    //TODO Mango 4.0 use this? throw new TranslatableJsonException("emport.error.missingRole", permission, "scriptPermissions");
                    roles.add(new Role(Common.NEW_ID, role));
                }
            }
        }
        return new ScriptPermissions(roles);
    }

}
