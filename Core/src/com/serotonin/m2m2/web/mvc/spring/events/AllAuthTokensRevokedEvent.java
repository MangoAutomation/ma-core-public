/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.events;

import org.springframework.context.ApplicationEvent;

/**
 * @author Jared Wiltshire
 */
public class AllAuthTokensRevokedEvent extends ApplicationEvent {
    private static final long serialVersionUID = 1L;

    public AllAuthTokensRevokedEvent(Object source) {
        super(source);
    }

    @Override
    public String toString() {
        return "AllAuthTokensRevokedEvent []";
    }

}