/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.vo.role;

import java.io.Serializable;

/**
 *
 * @author Terry Packer
 *
 */
public class Role implements Serializable {

    private static final long serialVersionUID = 6960583419249000443L;

    private final int id;
    private final String xid;

    public Role(RoleVO vo) {
        this(vo.getId(), vo.getXid());
    }

    public Role(int id, String xid){
        this.id = id;
        this.xid = xid;
    }

    public int getId() {
        return id;
    }

    public String getXid() {
        return xid;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        result = prime * result + ((xid == null) ? 0 : xid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Role other = (Role) obj;
        if (id != other.id)
            return false;
        if (xid == null) {
            if (other.xid != null)
                return false;
        } else if (!xid.equals(other.xid))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "(" + id + ") " + xid;
    }

}
