/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 * @Author Terry Packer
 *
 */

package com.serotonin.m2m2.module.definitions.audit;

import com.serotonin.m2m2.module.AuditEventTypeDefinition;

public class SystemSettingAuditEventTypeDefinition extends AuditEventTypeDefinition {

    public static final String TYPE_SYSTEM_SETTING = "SYSTEM_SETTING";

    @Override
    public String getTypeName() {
        return TYPE_SYSTEM_SETTING;
    }

    @Override
    public String getDescriptionKey() {
        return "event.audit.systemSetting";
    }
}
