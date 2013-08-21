/*
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */

package com.serotonin.m2m2.db.dao;

/**
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */
public class SortOption {
    String attribute;
    boolean desc;
    
    public SortOption() {
        this.desc = false;
    }
    
    public SortOption(String attribute) {
        this.attribute = attribute;
        this.desc = false;
    }

    public SortOption(String attribute, boolean desc) {
        this.attribute = attribute;
        this.desc = desc;
    }
    
    public String getAttribute() {
        return attribute;
    }

    public boolean isDesc() {
        return desc;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public void setDesc(boolean desc) {
        this.desc = desc;
    }
}
