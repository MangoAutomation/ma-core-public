package com.infiniteautomation.mango.emport;

import com.serotonin.m2m2.vo.DataPointSummary;

public class DataPointSummaryPathPair {
    private final DataPointSummary dps;
    private final String path;
    
    public DataPointSummaryPathPair(DataPointSummary dps, String path) {
        this.dps = dps;
        this.path = path;
    }
    
    public DataPointSummary getDataPointSummary() {
        return dps;
    }
    
    public String getPath() {
        return path;
    }
}
