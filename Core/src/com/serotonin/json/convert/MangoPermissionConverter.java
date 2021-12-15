/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.json.convert;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonTypeWriter;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.role.Role;

/**
 * Uses either the legacy form of a String of CSV roles or the new {@code Set<Set<String>>} format
 * @author Terry Packer
 */
public class MangoPermissionConverter extends ImmutableClassConverter {

    @Override
    public JsonValue jsonWrite(JsonTypeWriter writer, Object value) throws JsonException {
        MangoPermission permission = (MangoPermission)value;
        JsonArray outerRolesArray = new JsonArray();
        for(Set<Role> roleSet : permission.getRoles()) {
            JsonArray roles = new JsonArray();
            for(Role role : roleSet) {
                roles.add(role.getXid());
            }
            outerRolesArray.add(roles);
        }
        return outerRolesArray;
    }

    @Override
    public void jsonWrite(JsonWriter writer, Object value) throws IOException, JsonException {
        MangoPermission permission = (MangoPermission)value;
        JsonArray outerRolesArray = new JsonArray();
        for(Set<Role> roleSet : permission.getRoles()) {
            JsonArray roles = new JsonArray();
            for(Role role : roleSet) {
                roles.add(role.getXid());
            }
            outerRolesArray.add(roles);
        }
        writer.writeObject(outerRolesArray);
    }

    @Override
    public Object jsonRead(JsonReader reader, JsonValue jsonValue, Type type) throws JsonException {
        Set<Set<Role>> roles = new HashSet<>();
        PermissionService permissionService = Common.getBean(PermissionService.class);
        if(jsonValue instanceof JsonArray) {
            for(JsonValue val : (JsonArray)jsonValue) {
                if(val instanceof JsonArray) {
                    Set<Role> inner = new HashSet<>();
                    roles.add(inner);
                    for(JsonValue v : (JsonArray)val) {
                        Role r = permissionService.getRole(v.toString());
                        if(r != null) {
                            inner.add(r);
                        }else {
                            inner.add(new Role(Common.NEW_ID, v.toString()));
                        }
                    }
                }else {
                    //Just a single string
                    Role r = permissionService.getRole(val.toString());
                    if(r != null) {
                        roles.add(Collections.singleton(r));
                    }else {
                        roles.add(Collections.singleton(new Role(Common.NEW_ID, val.toString())));
                    }
                }
            }
        }else {
            for(String role : PermissionService.explodeLegacyPermissionGroups(jsonValue.toString())) {
                Role r = permissionService.getRole(role);
                if(r != null) {
                    roles.add(Collections.singleton(r));
                }else {
                    roles.add(Collections.singleton(new Role(Common.NEW_ID, role)));
                }
            }
        }
        return new MangoPermission(roles);
    }

}
