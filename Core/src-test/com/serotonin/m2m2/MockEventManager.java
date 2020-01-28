/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2;

import java.util.List;
import java.util.Map;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventManagerListenerDefinition;
import com.serotonin.m2m2.rt.EventManagerImpl;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.ReturnCause;
import com.serotonin.m2m2.rt.event.UserEventListener;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.User;

/**
 *
 * @author Terry Packer
 */
public class MockEventManager extends EventManagerImpl {

    /**
     * Use database to save events
     */
    protected boolean useDatabase;

    public MockEventManager() {

    }

    public MockEventManager(boolean useDatabase) {
        this.useDatabase = useDatabase;
    }

    @Override
    public int getState() {

        return 0;
    }

    @Override
    public void raiseEvent(EventType type, long time, boolean rtnApplicable, AlarmLevels alarmLevel,
            TranslatableMessage message, Map<String, Object> context) {
        if(useDatabase) {
            super.raiseEvent(type, time, rtnApplicable, alarmLevel, message, context);
        }
    }

    @Override
    public void returnToNormal(EventType type, long time) {
        if(useDatabase) {
            super.returnToNormal(type, time);
        }
    }

    @Override
    public void returnToNormal(EventType type, long time, ReturnCause cause) {
        if(useDatabase) {
            super.returnToNormal(type, time, cause);
        }
    }

    @Override
    public EventInstance acknowledgeEventById(int eventId, long time, User user,
            TranslatableMessage alternateAckSource) {
        if(useDatabase) {
            return super.acknowledgeEventById(eventId, time, user, alternateAckSource);
        }else {
            return null;
        }
    }

    @Override
    public long getLastAlarmTimestamp() {
        if(useDatabase) {
            return super.getLastAlarmTimestamp();
        }else {
            return 0;
        }
    }

    @Override
    public int purgeAllEvents() {
        if(useDatabase) {
            return super.purgeAllEvents();
        }else {
            return 0;
        }
    }

    @Override
    public int purgeEventsBefore(long time) {
        if(useDatabase) {
            return super.purgeEventsBefore(time);
        }
        return 0;
    }

    @Override
    public int purgeEventsBefore(long time, String typeName) {
        if(useDatabase) {
            return super.purgeEventsBefore(time, typeName);
        }
        return 0;
    }

    @Override
    public int purgeEventsBefore(long time, AlarmLevels alarmLevel) {
        if(useDatabase) {
            return super.purgeEventsBefore(time, alarmLevel);
        }
        return 0;
    }

    @Override
    public void cancelEventsForDataPoint(int dataPointId) {
        if(useDatabase) {
            super.cancelEventsForDataPoint(dataPointId);
        }
    }

    @Override
    public void cancelEventsForDataSource(int dataSourceId) {
        if(useDatabase) {
            super.cancelEventsForDataSource(dataSourceId);
        }
    }

    @Override
    public void cancelEventsForPublisher(int publisherId) {
        if(useDatabase) {
            super.cancelEventsForPublisher(publisherId);
        }
    }

    @Override
    public void initialize(boolean safe) {
        if(useDatabase) {
            super.initialize(safe);
        }
    }

    @Override
    public void terminate() {
        if(useDatabase) {
            super.terminate();
        }
    }

    @Override
    public void joinTermination() {
        if(useDatabase) {
            super.joinTermination();
        }
    }

    @Override
    public void addListener(EventManagerListenerDefinition l) {
        if(useDatabase) {
            super.addListener(l);
        }
    }

    @Override
    public void removeListener(EventManagerListenerDefinition l) {
        if(useDatabase) {
            super.removeListener(l);
        }
    }

    @Override
    public void addUserEventListener(UserEventListener l) {
        if(useDatabase) {
            super.addUserEventListener(l);
        }
    }

    @Override
    public void removeUserEventListener(UserEventListener l) {
        if(useDatabase) {
            super.removeUserEventListener(l);
        }
    }

    @Override
    public List<EventInstance> getAllActive() {
        if(useDatabase) {
            super.getAllActive();
        }
        return null;
    }

}
