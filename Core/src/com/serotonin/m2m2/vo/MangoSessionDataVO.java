/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.serotonin.m2m2.vo;

import org.eclipse.jetty.server.session.SessionData;

import com.serotonin.m2m2.Common;

/**
 * Container for persistent http session information
 * @author Terry Packer
 */
public class MangoSessionDataVO {

    private String sessionId;
    private String contextPath;
    private String virtualHost;
    private String lastNode;
    /**
     * the time of the last access
     */
    private long accessTime;
    /**
     * the time of the last access excluding this one
     */
    private long lastAccessTime;
    private long createTime;
    private long cookieTime;
    /**
     * time in msec since last save
     */
    private long lastSavedTime;
    /**
     * precalculated time of expiry in ms since epoch
     */
    private long expiryTime;
    private long maxInterval;
    private int userId = Common.NEW_ID;

    public MangoSessionDataVO() {

    }

    public MangoSessionDataVO(SessionData data) {
        this.sessionId = data.getId();
        this.contextPath = data.getContextPath();
        this.virtualHost = data.getVhost();
        this.lastNode = data.getLastNode();
        this.accessTime = data.getAccessed();
        this.lastAccessTime = data.getLastAccessed();
        this.createTime = data.getCreated();
        this.cookieTime = data.getCookieSet();
        this.lastSavedTime = data.getLastSaved();
        this.expiryTime = data.getExpiry();
        this.maxInterval = data.getMaxInactiveMs();
    }

    public String getSessionId() {
        return sessionId;
    }
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    public String getContextPath() {
        return contextPath;
    }
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
    public String getVirtualHost() {
        return virtualHost;
    }
    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }
    public String getLastNode() {
        return lastNode;
    }
    public void setLastNode(String lastNode) {
        this.lastNode = lastNode;
    }
    public long getAccessTime() {
        return accessTime;
    }
    public void setAccessTime(long accessTime) {
        this.accessTime = accessTime;
    }
    public long getLastAccessTime() {
        return lastAccessTime;
    }
    public void setLastAccessTime(long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }
    public long getCreateTime() {
        return createTime;
    }
    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
    public long getCookieTime() {
        return cookieTime;
    }
    public void setCookieTime(long cookieTime) {
        this.cookieTime = cookieTime;
    }
    public long getLastSavedTime() {
        return lastSavedTime;
    }
    public void setLastSavedTime(long lastSavedTime) {
        this.lastSavedTime = lastSavedTime;
    }
    public long getExpiryTime() {
        return expiryTime;
    }
    public void setExpiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
    }
    public long getMaxInterval() {
        return maxInterval;
    }
    public void setMaxInterval(long maxInterval) {
        this.maxInterval = maxInterval;
    }
    public int getUserId() {
        return userId;
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }

}
