/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.type.definition;

import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.SystemEventTypeDefinition;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.SystemEventType;

public class BackupSuccessEventTypeDefinition extends SystemEventTypeDefinition {

    public final static String BACKUP_PATH_CONTEXT_KEY = "BACKUP_PATH";

    @Override
    public String getTypeName() {
        return SystemEventType.TYPE_BACKUP_SUCCESS;
    }

    @Override
    public String getDescriptionKey() {
        return "event.system.backupSuccess";
    }

    @Override
    public AlarmLevels getDefaultAlarmLevel() {
        return AlarmLevels.INFORMATION;
    }

    @Override
    public String getEventListLink(int ref1, int ref2, Translations translations) {
        return null;
    }

    @Override
    public boolean supportsReferenceId1() {
        return false;
    }

    @Override
    public boolean supportsReferenceId2() {
        return false;
    }

}
