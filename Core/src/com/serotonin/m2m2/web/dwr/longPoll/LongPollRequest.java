/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr.longPoll;

import java.io.Serializable;

import org.apache.commons.lang3.ArrayUtils;

/**
 * @author Matthew Lohbihler
 */
public class LongPollRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean maxAlarm = true;
    private boolean terminated;

    private boolean pointDetails;
    private boolean pendingAlarms;

    private String[] handlers;
    private int refId;

    public boolean hasHandler(String handler) {
        return ArrayUtils.contains(handlers, handler);
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }

    public boolean isMaxAlarm() {
        return maxAlarm;
    }

    public void setMaxAlarm(boolean maxAlarm) {
        this.maxAlarm = maxAlarm;
    }

    public boolean isPointDetails() {
        return pointDetails;
    }

    public void setPointDetails(boolean pointDetails) {
        this.pointDetails = pointDetails;
    }

    public boolean isPendingAlarms() {
        return pendingAlarms;
    }

    public void setPendingAlarms(boolean pendingAlarms) {
        this.pendingAlarms = pendingAlarms;
    }

    public String[] getHandlers() {
        return handlers;
    }

    public void setHandlers(String[] handlers) {
        this.handlers = handlers;
    }

    public int getRefId() {
        return refId;
    }

    public void setRefId(int refId) {
        this.refId = refId;
    }
}
