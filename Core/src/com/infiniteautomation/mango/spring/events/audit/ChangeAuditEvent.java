/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 * @Author Terry Packer
 *
 */

package com.infiniteautomation.mango.spring.events.audit;

import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

public class ChangeAuditEvent<T extends AbstractVO> extends AuditEvent {

    private final T from;
    private final T vo;

    public ChangeAuditEvent(String auditEventType, PermissionHolder holder, T from, T vo) {
        super(auditEventType, holder);
        this.from = from;
        this.vo = vo;
    }

    public T getFrom() {
        return from;
    }

    public T getVo() {
        return vo;
    }
}
