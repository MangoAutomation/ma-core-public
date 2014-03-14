/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo;

import com.serotonin.m2m2.web.taglib.Functions;

public class UserComment {
    public static final int TYPE_EVENT = 1;
    public static final int TYPE_POINT = 2;

    // Configuration fields
    private int userId;
    private long ts;
    private String comment;

    // Relational fields
    private String username;

    public String getPrettyTime() {
        return Functions.getTime(ts);
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
