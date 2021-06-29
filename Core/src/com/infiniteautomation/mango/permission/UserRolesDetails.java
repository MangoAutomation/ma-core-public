/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.permission;

import java.util.HashSet;
import java.util.Set;

import com.serotonin.m2m2.vo.role.Role;

/**
 *
 * @author Terry Packer
 */
public class UserRolesDetails {

    private final String username;
    private final boolean admin;
    private final Set<Role> allRoles;
    private final Set<Role> matchingRoles;

    public UserRolesDetails(String username, boolean admin) {
        this.username = username;
        this.admin = admin;
        this.allRoles = new HashSet<>();
        this.matchingRoles = new HashSet<>();
    }

    public String getUsername() {
        return username;
    }

    public boolean isAdmin() {
        return admin;
    }

    public Set<Role> getAllRoles() {
        return allRoles;
    }

    public Set<Role> getMatchingRoles() {
        return matchingRoles;
    }
}
