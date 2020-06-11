/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractBasicVO other = (AbstractBasicVO) obj;
        if (id != other.id)
            return false;
        return true;
    }
}
