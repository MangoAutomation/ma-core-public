/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.serotonin.m2m2.vo.role;

import java.io.IOException;
import java.util.Set;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonObject;

/**
 * A tree of Role inheritance
 * @author Terry Packer
 */
public class RoleTree extends RoleVO {
    private static final long serialVersionUID = 1L;

    final private Set<RoleTree> inheritedRoles;

    public RoleTree(RoleVO role, Set<RoleTree> inheritedRoles) {
        this(role.getId(), role.getXid(), role.getName(), inheritedRoles);
    }

    public RoleTree(int id, String xid, String name, Set<RoleTree> inheritedRoles) {
        super(id, xid, name);
        this.inheritedRoles = inheritedRoles;
    }

    public Set<RoleTree> getInheritedRoles() {
        return inheritedRoles;
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        // TODO Auto-generated method stub
        super.jsonRead(reader, jsonObject);
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        if(inheritedRoles.size() > 0) {
            writer.writeEntry("inheritedRoles", this.inheritedRoles);
        }
    }
}
