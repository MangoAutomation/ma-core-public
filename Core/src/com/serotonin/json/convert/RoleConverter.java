/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.json.convert;

import java.io.IOException;
import java.lang.reflect.Type;

import com.infiniteautomation.mango.util.LazyInitSupplier;
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
 * Read/Write roles as a string instead of an object  this is for the Role class NOT RoleVOs
 * @author Terry Packer
 */
public class RoleConverter extends ImmutableClassConverter {

    private static final LazyInitSupplier<RoleDao> roleDaoInstance = new LazyInitSupplier<>(() -> {
        return Common.getRuntimeContext().getBean(RoleDao.class);
    });

    @Override
    public JsonValue jsonWrite(JsonTypeWriter writer, Object value) throws JsonException {
        return new JsonString(((Role)value).getXid());
    }

    @Override
    public void jsonWrite(JsonWriter writer, Object value) throws IOException, JsonException {
        writer.quote(((Role)value).getXid());
    }

    //TODO Mango 4.2 improve performance with lazy field as role dao is not available at construct time
    @Override
    public Object jsonRead(JsonReader reader, JsonValue jsonValue, Type type) throws JsonException {
        RoleVO role = roleDaoInstance.get().getByXid(jsonValue.toString());
        if(role != null) {
            return role.getRole();
        }else {
            //TODO mango 4.2? throw new TranslatableJsonException("emport.error.missingRole", jv.toString(), "scriptPermissions");
            return new Role(Common.NEW_ID, jsonValue.toString());
        }
    }

}
