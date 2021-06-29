/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.type;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.module.SystemSettingsListenerDefinition;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.vo.event.EventTypeVO;

/**
 *
 * @author Terry Packer
 */
public class SystemEventTypeSettingsListenerDefinition extends SystemSettingsListenerDefinition{

    @Override
    public void systemSettingsSaved(String key, String oldValue, String newValue) {
        String[] parts = key.split(SystemEventType.SYSTEM_SETTINGS_PREFIX);
        if (parts.length > 1) {
            SystemEventType.updateAlarmLevel(parts[1], AlarmLevels.fromValue(Integer.parseInt(newValue)));
        }
    }

    @Override
    public void systemSettingsRemoved(String key, String lastValue, String defaultValue) { }

    @Override
    public List<String> getKeys() {
        List<String> keys = new ArrayList<String>();
        for(EventTypeVO vo : SystemEventType.getRegisteredEventTypes())
            keys.add(SystemEventType.SYSTEM_SETTINGS_PREFIX + vo.getEventType().getEventSubtype());
        return keys;
    }

}
