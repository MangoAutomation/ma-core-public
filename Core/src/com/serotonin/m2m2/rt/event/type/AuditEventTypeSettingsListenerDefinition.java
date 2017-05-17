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

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.systemSettings.SystemSettingsListener#SystemSettingsSaved(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void SystemSettingsSaved(String key, String oldValue, String newValue) {
        String[] parts = key.split(AuditEventType.AUDIT_SETTINGS_PREFIX);
 		EventTypeVO et = AuditEventType.getEventType(parts[1]);
        et.setAlarmLevel(Integer.parseInt(newValue));
	}


	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.systemSettings.SystemSettingsListener#SystemSettingsRemoved(java.lang.String, java.lang.String)
	 */
	@Override
	public void SystemSettingsRemoved(String key, String lastValue) {
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.systemSettings.SystemSettingsListener#getKeys()
	 */
	@Override
	public List<String> getKeys() {
		List<String> keys = new ArrayList<String>();
		for(EventTypeVO vo : AuditEventType.getAllRegisteredEventTypes())
			keys.add(AuditEventType.AUDIT_SETTINGS_PREFIX + vo.getSubtype());
		return keys;
	}

}
