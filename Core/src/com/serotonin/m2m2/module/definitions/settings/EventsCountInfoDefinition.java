/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.settings;

import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.module.SystemInfoDefinition;

/**
 * Class to define Read only settings/information that can be provided
 *
 * @author Terry Packer
 */
public class EventsCountInfoDefinition extends SystemInfoDefinition<Integer>{

    public final String KEY = "eventsCount";

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public Integer getValue() {
        return EventDao.getInstance().getEventCount();
    }

    @Override
    public String getDescriptionKey() {
        return "systemInfo.eventCountsDesc";
    }

}
