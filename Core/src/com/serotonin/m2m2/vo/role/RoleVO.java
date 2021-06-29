/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.role;

import java.util.Collections;
import java.util.Set;

import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.vo.AbstractVO;

/**
 * Container for a role. The xid field holds the
 * unique identifier for the role.
 *
 * @author Terry Packer
 *
 */
public class RoleVO extends AbstractVO {

    private static final long serialVersionUID = 1L;
    public static final String XID_PREFIX = "ROLE_";

    @JsonProperty
    private Set<Role> inherited;

    public RoleVO(int id, String xid, String name) {
        this(id, xid, name, Collections.emptySet());
    }

    public RoleVO(int id, String xid, String name, Set<Role> inherited) {
        this.id = id;
        this.xid = xid;
        this.name = name;
        this.inherited = Collections.unmodifiableSet(inherited);
    }

    @Override
    @Deprecated
    public void setXid(String xid) {
        throw new UnsupportedOperationException("Cannot change role xid");
    }

    @Override
    @Deprecated
    public void setName(String name) {
        throw new UnsupportedOperationException("Cannot change role name");
    }

    public Set<Role> getInherited() {
        return inherited;
    }

    public void setInherited(Set<Role> inherited) {
        this.inherited = inherited;
    }

    @Override
    public String getTypeKey() {
        return "event.audit.role";
    }

    public Role getRole() {
        return new Role(this);
    }

}
