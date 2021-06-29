/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util;

/**
 * Simple class to relay Work Item info to page
 * @author Terry Packer
 *
 */
public class WorkItemInfo {
    
    private String classname;
    private String description;
    private String priority;
    
    public WorkItemInfo(){ }
    
    /**
     * @param canonincalClassname
     * @param description
     */
    public WorkItemInfo(String canonincalClassname, String description, String priority) {
        super();
        this.classname = canonincalClassname;
        this.description = description;
        this.priority = priority;
    }
    public String getClassname() {
        return classname;
    }
    public void setClassname(String canonincalClassname) {
        this.classname = canonincalClassname;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getPriority(){
        return this.priority;
    }
    public void setPriority(String priority){
        this.priority = priority;
    }
    
    
}
