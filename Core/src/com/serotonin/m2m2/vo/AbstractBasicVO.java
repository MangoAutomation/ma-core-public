/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo;

import com.serotonin.m2m2.Common;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractBasicVO {

    /*
     * Mango properties
     */
    protected int id = Common.NEW_ID;

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
	
    /**
     * Useful For Debugging
     */
    @Override
    public String toString() {
        return "id: " + this.id;
    }
}
