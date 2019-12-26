/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.vo;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.db.dao.RoleDao;

/**
 * Container for a role. The xid field holds the 
 * unique identifier for the role.
 * 
 * @author Terry Packer
 *
 */
public class RoleVO extends AbstractVO<RoleVO> {

    private static final long serialVersionUID = 1L;
    public static final String XID_PREFIX = "ROLE_";
    
    public RoleVO(String xid, String name) {
        this.xid = xid;
        this.name = name;
    }
    
    @Override
    @Deprecated
    public void setXid(String xid) {
        throw new ShouldNeverHappenException("Cannot change role xid");
    }
    
    @Override
    @Deprecated
    public void setName(String name) {
        throw new ShouldNeverHappenException("Cannot change role name");
    }
    
    @Override
    protected AbstractDao<RoleVO> getDao() {
        return RoleDao.getInstance();
    }

    @Override
    public String getTypeKey() {
        return "event.audit.role";
    }
}
