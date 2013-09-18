/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.dwr;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.EventInstanceDao;
import com.serotonin.m2m2.module.AuditEventTypeDefinition;
import com.serotonin.m2m2.module.EventTypeDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.SystemEventTypeDefinition;
import com.serotonin.m2m2.vo.event.EventInstanceVO;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;

/**
 * @author Terry Packer
 *
 */
public class EventInstanceDwr extends AbstractDwr<EventInstanceVO, EventInstanceDao>{

	/**
	 * @param dao
	 * @param keyName
	 */
	public EventInstanceDwr() {
		super(EventInstanceDao.instance, "eventInstances");
	}

	// Utility Methods for help with rendering some strings
	@DwrPermission(user = true)
    public static String getSystemEventTypeLink(String subtype, int ref1, int ref2) {
        SystemEventTypeDefinition def = ModuleRegistry.getSystemEventTypeDefinition(subtype);
        if (def != null)
            return def.getEventListLink(ref1, ref2, Common.getTranslations());
        return null;
    }
	
	@DwrPermission(user = true)
    public static String getAuditEventTypeLink(String subtype, int ref1, int ref2) {
        AuditEventTypeDefinition def = ModuleRegistry.getAuditEventTypeDefinition(subtype);
        if (def != null)
            return def.getEventListLink(ref1, ref2, Common.getTranslations());
        return null;
    }
	
	@DwrPermission(user = true)
    public static String getEventTypeLink(String type, String subtype, int ref1, int ref2) {
        EventTypeDefinition def = ModuleRegistry.getEventTypeDefinition(type);
        if (def != null)
            return def.getEventListLink(subtype, ref1, ref2, Common.getTranslations());
        return null;
    }

	
	
}
