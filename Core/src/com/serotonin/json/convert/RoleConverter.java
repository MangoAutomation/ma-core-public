/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.serotonin.json.convert;

import java.io.IOException;
import java.lang.reflect.Type;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonString;
import com.serotonin.json.type.JsonTypeWriter;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * Read/Write roles as a string instead of an object
 * @author Terry Packer
 */
public class RoleConverter extends ImmutableClassConverter {

    @Override
    public JsonValue jsonWrite(JsonTypeWriter writer, Object value) throws JsonException {
        return new JsonString(((Role)value).getXid());
    }

    @Override
    public void jsonWrite(JsonWriter writer, Object value) throws IOException, JsonException {
        writer.append(((Role)value).getXid());
    }

    @Override
    public Object jsonRead(JsonReader reader, JsonValue jsonValue, Type type) throws JsonException {
        RoleVO role = RoleDao.getInstance().getByXid(jsonValue.toString());
        if(role != null) {
            return role.getRole();
        }else {
            return new Role(Common.NEW_ID, jsonValue.toString());
        }
    }

}
