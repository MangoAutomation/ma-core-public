/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.events;

import java.util.EnumSet;

import org.springframework.context.ApplicationEvent;

import com.serotonin.m2m2.vo.User;

/**
 * @author Jared Wiltshire
 */
public class UserUpdatedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    public static enum UpdatedFields {
        AUTH_TOKEN, PASSWORD, PERMISSIONS
    }

    private final User user;
    private final EnumSet<UpdatedFields> updatedFields;

    public UserUpdatedEvent(Object source, User user, EnumSet<UpdatedFields> updatedFields) {
        super(source);
        this.user = user;
        this.updatedFields = updatedFields;
    }

    public User getUser() {
        return user;
    }

    public EnumSet<UpdatedFields> getUpdatedFields() {
        return updatedFields;
    }

}
