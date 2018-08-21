/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

public enum DaoEventType {
    CREATE("add"), UPDATE("update"), DELETE("delete"), CUSTOM("");

    private final String action;

    private DaoEventType(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}