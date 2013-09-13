/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.dwr;

import com.serotonin.m2m2.db.dao.EventInstanceDao;
import com.serotonin.m2m2.vo.event.EventInstanceVO;

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

}
