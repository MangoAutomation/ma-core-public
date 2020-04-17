/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.vo.role;

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

    public RoleVO(int id, String xid, String name) {
        this.id = id;
        this.xid = xid;
        this.name = name;
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

    @Override
    public String getTypeKey() {
        return "event.audit.role";
    }

    public Role getRole() {
        return new Role(this);
    }

}
