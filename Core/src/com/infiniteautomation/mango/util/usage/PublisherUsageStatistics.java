/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util.usage;

/**
 * Statistics about how Publishers are used in this system
 * 
 * @author Terry Packer
 *
 */
public class PublisherUsageStatistics {

    private String publisherType;
    private Integer count;
    
    /**
     * @return the publisherType
     */
    public String getPublisherType() {
        return publisherType;
    }
    /**
     * @param publisherType the publisherType to set
     */
    public void setPublisherType(String publisherType) {
        this.publisherType = publisherType;
    }
    /**
     * @return the count
     */
    public Integer getCount() {
        return count;
    }
    /**
     * @param count the count to set
     */
    public void setCount(Integer count) {
        this.count = count;
    }
    
}
