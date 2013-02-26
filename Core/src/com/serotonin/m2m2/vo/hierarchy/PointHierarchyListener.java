/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.hierarchy;

abstract public class PointHierarchyListener {
    /**
     * The point hierarchy is cleared whenever a point is saved. Note that this may not mean the hierarchy has actually
     * changed.
     */
    public void pointHierarchyCleared() {
        // Override as required
    }

    /**
     * 
     * @param root
     */
    public void pointHierarchySaved(PointFolder root) {
        // Override as required
    }
}
