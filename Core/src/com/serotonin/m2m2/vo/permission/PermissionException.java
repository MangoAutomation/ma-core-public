/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.permission;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.User;

public class PermissionException extends RuntimeException {
    private static final long serialVersionUID = -1;

    private final TranslatableMessage translatableMessage;
    private final User user;

    public PermissionException(TranslatableMessage translatableMessage, User user) {
        super();
        this.translatableMessage = translatableMessage;
        this.user = user;
    }

    public User getUser() {
        return user;
    }
    
    public TranslatableMessage getTranslatableMessage() {
        return translatableMessage;
    }
    
    @Override
    public String getMessage() {
        return this.translatableMessage.translate(Common.getTranslations());
    }
}
