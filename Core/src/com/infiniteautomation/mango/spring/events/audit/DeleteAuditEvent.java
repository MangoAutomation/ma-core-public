/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 * @Author Terry Packer
 *
 */

package com.infiniteautomation.mango.spring.events.audit;

import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

public class DeleteAuditEvent <T extends AbstractVO> extends AuditEvent {

    private final T vo;

    public DeleteAuditEvent(String auditEventType, PermissionHolder holder, T vo) {
        super(auditEventType, holder);
        this.vo = vo;
    }

    public T getVo() {
        return vo;
    }
}
