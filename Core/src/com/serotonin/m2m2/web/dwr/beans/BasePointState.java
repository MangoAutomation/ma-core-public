/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr.beans;

import org.apache.commons.lang3.StringUtils;

abstract public class BasePointState implements Cloneable {
    private String id;
    private String change;
    private String chart;
    private String messages;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChange() {
        return change;
    }

    public void setChange(String change) {
        this.change = change;
    }

    public String getChart() {
        return chart;
    }

    public void setChart(String chart) {
        this.chart = chart;
    }

    public String getMessages() {
        return messages;
    }

    public void setMessages(String messages) {
        this.messages = messages;
    }

    public void removeEqualValue(BasePointState that) {
        if (StringUtils.equals(change, that.change))
            change = null;
        if (StringUtils.equals(chart, that.chart))
            chart = null;
        if (StringUtils.equals(messages, that.messages))
            messages = null;
    }

    public boolean isEmpty() {
        return change == null && chart == null && messages == null;
    }
}
