/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
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
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * Uses either the legacy form of a String of CSV roles or the new Set<Set<String>> format
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

    //TODO Mango 4.0 improve performance
    @Override
    public Object jsonRead(JsonReader reader, JsonValue jsonValue, Type type) throws JsonException {
        Set<Set<Role>> roles = new HashSet<>();
        RoleDao roleDao = Common.getBean(RoleDao.class);
        if(jsonValue instanceof JsonArray) {
            for(JsonValue val : (JsonArray)jsonValue) {
                if(val instanceof JsonArray) {
                    Set<Role> inner = new HashSet<>();
                    roles.add(inner);
                    for(JsonValue v : (JsonArray)val) {
                        RoleVO r = roleDao.getByXid(v.toString());
                        if(r != null) {
                            inner.add(r.getRole());
                        }else {
                            inner.add(new Role(Common.NEW_ID, v.toString()));
                        }
                    }
                }else {
                    //Just a single string
                    RoleVO r = roleDao.getByXid(val.toString());
                    if(r != null) {
                        roles.add(Collections.singleton(r.getRole()));
                    }else {
                        roles.add(Collections.singleton(new Role(Common.NEW_ID, val.toString())));
                    }
                }
            }
        }else {
            for(String role : PermissionService.explodeLegacyPermissionGroups(jsonValue.toString())) {
                RoleVO r = roleDao.getByXid(role);
                if(r != null) {
                    roles.add(Collections.singleton(r.getRole()));
                }else {
                    roles.add(Collections.singleton(new Role(Common.NEW_ID, role)));
                }
            }
        }
        return new MangoPermission(roles);
    }

}
