/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 * @Author Terry Packer
 *
 */

package com.infiniteautomation.mango.spring.events.audit;

import com.serotonin.m2m2.module.definitions.audit.SystemSettingAuditEventTypeDefinition;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

public class SystemSettingDeleteAuditEvent extends AuditEvent {

    private final String key;
    private final String fromValue;
    private final String toValue;

    public SystemSettingDeleteAuditEvent(PermissionHolder holder, String key, String fromValue, String toValue) {
        super(SystemSettingAuditEventTypeDefinition.TYPE_SYSTEM_SETTING, holder);
        this.key = key;
        this.fromValue = fromValue;
        this.toValue = toValue;
    }

    public String getKey() {
        return key;
    }

    public String getFromValue() {
        return fromValue;
    }

    public String getToValue() {
        return toValue;
    }
}
