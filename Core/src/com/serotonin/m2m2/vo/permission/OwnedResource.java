/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.vo.permission;

public interface OwnedResource {
    boolean isOwnedBy(PermissionHolder user);
}
