/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.permission;

import com.serotonin.m2m2.vo.User;

public class PermissionException extends RuntimeException {
    private static final long serialVersionUID = -1;

    private final User user;

    public PermissionException(String message, User user) {
        super(message);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
