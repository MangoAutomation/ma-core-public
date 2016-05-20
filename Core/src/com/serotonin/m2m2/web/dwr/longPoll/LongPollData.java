/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr.longPoll;

import java.io.Serializable;

import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.web.dwr.BaseDwr;

/**
 * @author Matthew Lohbihler
 */
public class LongPollData implements HttpSessionBindingListener, Serializable {
    private static final long serialVersionUID = 1L;

    private final int pollSessionId;
    private long timestamp;
    private LongPollRequest request;
    private LongPollState state;

    public LongPollData(int pollSessionId) {
        this.pollSessionId = pollSessionId;
        updateTimestamp();
        setRequest(null);
    }

    public int getPollSessionId() {
        return pollSessionId;
    }

    public LongPollRequest getRequest() {
        return request;
    }

    public void updateTimestamp() {
        timestamp = Common.backgroundProcessing.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setRequest(LongPollRequest request) {
        if (request == null) {
            request = new LongPollRequest();
            request.setTerminated(true);
        }
        this.request = request;
    }

    public LongPollState getState() {
        return state;
    }

    public void setState(LongPollState state) {
        this.state = state;
    }

    //
    // /
    // / HttpSessionBindingListener implementation
    // /
    //
    public void valueBound(HttpSessionBindingEvent evt) {
        // no op
    }

    public void valueUnbound(HttpSessionBindingEvent evt) {
        // Terminate any long poll request.
        BaseDwr.terminateLongPollImpl(this);
    }
}
