/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.events;

import org.springframework.context.ApplicationEvent;

import com.infiniteautomation.mango.spring.eventMulticaster.PropagatingEvent;

/**
 * @author Jared Wiltshire
 */
public class AuthTokensRevokedEvent extends ApplicationEvent implements PropagatingEvent {
    private static final long serialVersionUID = 1L;

    public AuthTokensRevokedEvent(Object source) {
        super(source);
    }

    @Override
    public String toString() {
        return "AllAuthTokensRevokedEvent []";
    }

}