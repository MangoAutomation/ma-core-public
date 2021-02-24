/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 * @Author Terry Packer
 *
 */

package com.infiniteautomation.mango.spring.events.audit;

import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * Audit DAO event
 */
public abstract class AuditEvent {

    protected String auditEventType;
    protected PermissionHolder raisingHolder;

    public AuditEvent(String auditEventType, PermissionHolder holder) {
        this.auditEventType = auditEventType;
        this.raisingHolder = holder;
    }

    public String getAuditEventType() {
        return auditEventType;
    }

    public void setAuditEventType(String auditEventType) {
        this.auditEventType = auditEventType;
    }

    public PermissionHolder getRaisingHolder() {
        return raisingHolder;
    }

    public void setRaisingHolder(PermissionHolder raisingHolder) {
        this.raisingHolder = raisingHolder;
    }
}
