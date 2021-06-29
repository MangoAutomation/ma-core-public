/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util.usage;

/**
 * Statistics about how Data Sources are used in this system
 * 
 * @author Terry Packer
 *
 */
public class DataSourceUsageStatistics {

    private String dataSourceType;
    private Integer count;
    
    /**
     * @return the dataSourceType
     */
    public String getDataSourceType() {
        return dataSourceType;
    }
    /**
     * @param dataSourceType the dataSourceType to set
     */
    public void setDataSourceType(String dataSourceType) {
        this.dataSourceType = dataSourceType;
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
