/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.rt.event.type;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.module.SystemSettingsListenerDefinition;
import com.serotonin.m2m2.vo.event.EventTypeVO;

/**
 *
 * @author Terry Packer
 */
public class AuditEventTypeSettingsListenerDefinition extends SystemSettingsListenerDefinition{

    @Override
    public void SystemSettingsSaved(String key, String oldValue, String newValue) {
        String[] parts = key.split(AuditEventType.AUDIT_SETTINGS_PREFIX);
        if (parts.length > 1) {
            AuditEventType.updateAlarmLevel(parts[1], Integer.parseInt(newValue));
        }
    }

    @Override
    public void SystemSettingsRemoved(String key, String lastValue) {
    }

    @Override
    public List<String> getKeys() {
        List<String> keys = new ArrayList<String>();
        for(EventTypeVO vo : AuditEventType.getRegisteredEventTypes())
            keys.add(AuditEventType.AUDIT_SETTINGS_PREFIX + vo.getEventType().getEventSubtype());
        return keys;
    }

}
