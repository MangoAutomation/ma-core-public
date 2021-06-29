/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.permission;

import com.infiniteautomation.mango.util.exception.TranslatableRuntimeException;
import com.serotonin.m2m2.i18n.TranslatableMessage;

public class PermissionException extends TranslatableRuntimeException {
    private static final long serialVersionUID = -1;

    private final PermissionHolder permissionHolder;

    public PermissionException(TranslatableMessage translatableMessage, PermissionHolder permissionHolder) {
        super(translatableMessage);
        this.permissionHolder = permissionHolder;
    }

    public PermissionHolder getPermissionHolder() {
        return permissionHolder;
    }
}
