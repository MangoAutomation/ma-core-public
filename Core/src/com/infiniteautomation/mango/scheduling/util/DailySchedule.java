/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.scheduling.util;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.infiniteautomation.mango.scheduling.util.ScheduleUtils;
import com.infiniteautomation.mango.scheduling.util.TimeValue;
import com.serotonin.m2m2.i18n.ProcessResult;

/**
 * Schedule for 24hrs starting at 00:00:00.000
 *   Day starts as inactive, first timestamp is when it becomes active, 
 *   next is when it becomes inactive etc.
 * @author Terry Packer
 */
public class DailySchedule {
    
    //Offset from 00:00:00.000 in milliseconds
    List<String> changes;
    
    public DailySchedule() {
        this(new ArrayList<String>());
    }

    @JsonCreator
    public DailySchedule(List<String> changes) {
        this.changes = changes;
    }

    /**
     * @return the changes
     */
    @JsonValue
    public List<String> getChanges() {
        return changes;
    }

    /**
     * @param changes the changes to set
     */
    public void setChanges(List<String> changes) {
        this.changes = changes;
    }

    /**
     * @param msOffset
     */
    public void addChange(String change) {
        this.changes.add(change);
    }


    /**
     * 
     * @param response
     */
    public void validate(ProcessResult response){
        long lastOffset = -1;
        for(String change: changes) {
            try {
                TimeValue tv = ScheduleUtils.parseTimeValue(change);
                long offset = tv.getSeconds();
                if(offset <= lastOffset)
                    response.addContextualMessage("offsets", "schedule.validate.offsetsOutOfOrder");
                lastOffset = offset;
            }catch(Exception e) {
                response.addContextualMessage("changes", "schedule.validate.invalidTimeFormat");
            }
        }
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for(int i=0; i<changes.size(); i++) {
            String change = changes.get(i);
            builder.append(change);
            if(i < changes.size()-1)
                builder.append(",");
        }
        builder.append("]");
        return builder.toString();
    }
}
