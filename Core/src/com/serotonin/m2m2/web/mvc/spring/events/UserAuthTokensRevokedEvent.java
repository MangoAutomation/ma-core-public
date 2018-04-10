/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.events;

import org.springframework.context.ApplicationEvent;

import com.serotonin.m2m2.vo.User;

/**
 * @author Jared Wiltshire
 */
public class UserAuthTokensRevokedEvent extends ApplicationEvent {
    private static final long serialVersionUID = 1L;
    private final User user;

    public UserAuthTokensRevokedEvent(Object source, User user) {
        super(source);
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public String toString() {
        return "UserAuthTokensRevokedEvent [user=" + user + "]";
    }
}