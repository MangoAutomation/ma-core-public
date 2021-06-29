/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Terry Packer
 *
 */
public class DataPointEventLevelSummary {

    /**
     * Xid of dta
     */
    private String xid;
    /**
     * Count of events for a given level
     */
    private Map<AlarmLevels, Integer> counts;
    
    public DataPointEventLevelSummary(String xid) {
        this.xid = xid;
        this.counts = new HashMap<>();
    }
    
    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public Map<AlarmLevels, Integer> getCounts() {
        return counts;
    }

    public void setCounts(Map<AlarmLevels, Integer> counts) {
        this.counts = counts;
    }

    public void update(EventInstance instance) {
        counts.compute(instance.getAlarmLevel(), (level, count) ->{
            if(count == null) {
                return 1;
            }else {
                return count++;
            }
        });
    }
    
}
