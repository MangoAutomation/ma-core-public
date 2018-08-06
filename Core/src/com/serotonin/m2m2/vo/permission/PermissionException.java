/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.permission;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;

public class PermissionException extends RuntimeException {
    private static final long serialVersionUID = -1;

    private final TranslatableMessage translatableMessage;
    private final PermissionHolder permissionHolder;

    public PermissionException(TranslatableMessage translatableMessage, PermissionHolder permissionHolder) {
        super();
        this.translatableMessage = translatableMessage;
        this.permissionHolder = permissionHolder;
    }

    public PermissionHolder getPermissionHolder() {
        return permissionHolder;
    }

    public TranslatableMessage getTranslatableMessage() {
        return translatableMessage;
    }

    @Override
    public String getMessage() {
        return this.translatableMessage.translate(Common.getTranslations());
    }
}
