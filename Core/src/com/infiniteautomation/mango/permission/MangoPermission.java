/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.permission;

import java.util.List;

import com.serotonin.m2m2.vo.RoleVO;

/**
 * @author Terry Packer
 *
 */
public class MangoPermission {

    public static final String READ_TYPE = "READ";
    public static final String EDIT_TYPE = "EDIT";
    public static final String DELETE_TYPE = "DELETE";
    public static final String SET_TYPE = "SET";
    
    private String permissionType;
    private String voType;
    private int voId;
    private List<RoleVO> roles;
}
