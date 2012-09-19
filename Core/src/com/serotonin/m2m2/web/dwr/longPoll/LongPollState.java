/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr.longPoll;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.web.dwr.beans.PointDetailsState;

/**
 * @author Matthew Lohbihler
 */
public class LongPollState implements Serializable {
    private static final long serialVersionUID = 1L;

    private int maxAlarmLevel = -1;
    private long lastAlarmLevelChange = 0;
    private PointDetailsState pointDetailsState = null;
    private String pendingAlarmsContent;

    private Map<String, Object> attributes = new HashMap<String, Object>();

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public int getMaxAlarmLevel() {
        return maxAlarmLevel;
    }

    public void setMaxAlarmLevel(int maxAlarmLevel) {
        this.maxAlarmLevel = maxAlarmLevel;
    }

    public long getLastAlarmLevelChange() {
        return lastAlarmLevelChange;
    }

    public void setLastAlarmLevelChange(long lastAlarmLevelChange) {
        this.lastAlarmLevelChange = lastAlarmLevelChange;
    }

    public PointDetailsState getPointDetailsState() {
        return pointDetailsState;
    }

    public void setPointDetailsState(PointDetailsState pointDetailsState) {
        this.pointDetailsState = pointDetailsState;
    }

    public String getPendingAlarmsContent() {
        return pendingAlarmsContent;
    }

    public void setPendingAlarmsContent(String pendingAlarmsContent) {
        this.pendingAlarmsContent = pendingAlarmsContent;
    }

    /**
     * @param out
     *            required by the serialization API.
     */
    private void writeObject(ObjectOutputStream out) {
        // no op
    }

    /**
     * @param in
     *            required by the serialization API.
     */
    private void readObject(ObjectInputStream in) {
        maxAlarmLevel = -1;
        lastAlarmLevelChange = 0;
        pointDetailsState = null;
        pendingAlarmsContent = null;
        attributes = new HashMap<String, Object>();
    }
}
