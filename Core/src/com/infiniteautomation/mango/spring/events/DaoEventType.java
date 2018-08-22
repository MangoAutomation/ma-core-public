/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.events;

public enum DaoEventType {
    CREATE("add"), UPDATE("update"), DELETE("delete");

    private final String action;

    private DaoEventType(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}