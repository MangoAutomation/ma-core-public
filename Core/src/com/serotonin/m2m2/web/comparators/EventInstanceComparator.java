/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.comparators;

import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.rt.event.EventInstance;

public class EventInstanceComparator extends BaseComparator<EventInstance> {
    private static final int SORT_ALARM_LEVEL = 1;
    private static final int SORT_ACTIVE_TIME = 2;
    private static final int SORT_MESSAGE = 3;
    private static final int SORT_ID = 4;
    private static final int SORT_RTN_TIME = 5;

    private final Translations translations;

    public EventInstanceComparator(Translations translations, String sortField, boolean descending) {
        this.translations = translations;

        if ("alarmLevel".equals(sortField))
            sortType = SORT_ALARM_LEVEL;
        else if ("time".equals(sortField))
            sortType = SORT_ACTIVE_TIME;
        else if ("msg".equals(sortField))
            sortType = SORT_MESSAGE;
        else if ("id".equals(sortField))
            sortType = SORT_ID;
        else if ("rtntime".equals(sortField))
            sortType = SORT_RTN_TIME;
        this.descending = descending;
    }

    public int compare(EventInstance e1, EventInstance e2) {
        int result = 0;
        if (sortType == SORT_ALARM_LEVEL)
            result = e1.getAlarmLevel() - e2.getAlarmLevel();
        else if (sortType == SORT_ACTIVE_TIME) {
            long diff = e1.getActiveTimestamp() - e2.getActiveTimestamp();
            if (diff < 0)
                result = -1;
            else if (diff > 0)
                result = 1;
        }
        else if (sortType == SORT_MESSAGE) {
            String s1 = e1.getMessage().translate(translations);
            String s2 = e2.getMessage().translate(translations);
            result = s1.compareTo(s2);
        }
        else if (sortType == SORT_ID)
            result = e1.getId() - e2.getId();
        else if (sortType == SORT_RTN_TIME) {
            long diff = e1.getRtnTimestamp() - e2.getRtnTimestamp();
            if (diff < 0)
                result = -1;
            else if (diff > 0)
                result = 1;
        }

        if (descending)
            return -result;
        return result;
    }

}
