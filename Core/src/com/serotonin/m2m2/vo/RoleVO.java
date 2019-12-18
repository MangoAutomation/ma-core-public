/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.vo;

import com.serotonin.m2m2.db.dao.AbstractDao;

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
    
    @Override
    protected AbstractDao<RoleVO> getDao() {
        return null;
    }

    @Override
    public String getTypeKey() {
        return "event.audit.role";
    }

}
