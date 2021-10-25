/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.util;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.vo.role.Role;

import static org.junit.Assert.*;

/**
 * General assertion utilities
 */
public interface AssertionUtils {

    default void assertPermission(MangoPermission expected, MangoPermission actual) {
        assertTrue(expected.equals(actual));
    }

    default void assertRoles(Set<Role> expected, Set<Role> actual) {
        assertEquals(expected.size(), actual.size());
        Set<Role> missing = new HashSet<>();
        for(Role expectedRole : expected) {
            boolean found = false;
            for(Role actualRole : actual) {
                if(StringUtils.equals(expectedRole.getXid(), actualRole.getXid())) {
                    found = true;
                    break;
                }
            }
            if(!found) {
                missing.add(expectedRole);
            }
        }
        if(missing.size() > 0) {
            StringBuilder missingRoles = new StringBuilder();
            for(Role missingRole : missing) {
                missingRoles.append("< ")
                        .append(missingRole.getId())
                        .append(" - ")
                        .append(missingRole.getXid())
                        .append("> ");
            }
            fail("Not all roles matched, missing: " + missingRoles);
        }
    }
}
