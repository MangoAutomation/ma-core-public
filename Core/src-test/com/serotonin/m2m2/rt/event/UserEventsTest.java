/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.junit.Test;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.definitions.permissions.SuperadminPermissionDefinition;
import com.serotonin.m2m2.rt.EventManager;
import com.serotonin.m2m2.rt.EventManagerImpl;
import com.serotonin.m2m2.rt.event.type.EventType.DuplicateHandling;
import com.serotonin.m2m2.rt.event.type.MockEventType;
import com.serotonin.m2m2.vo.User;

/**
 *
 * @author Terry Packer
 */
public class UserEventsTest extends MangoTestBase {

    @Test
    public void testRaiseEvents() throws InterruptedException {
        
        int dataPointId = 17;
        int userCount = 2;
        int eventCount = 100;
        
        //Create some users
        List<User> users = createUsers(userCount, SuperadminPermissionDefinition.GROUP_NAME);
        
        List<MockUserEventListener> listeners = new ArrayList<>();
        for(User u : users) {
            MockUserEventListener l = new MockUserEventListener(u);
            listeners.add(l);
            Common.eventManager.addUserEventListener(l);
        }
        
        //Raise some events
        List<MockEventTypeTime> raised = new ArrayList<>();
        for(int i=0; i<eventCount; i++) {
            MockEventType event = new MockEventType(DuplicateHandling.ALLOW, null, i, dataPointId);
            raised.add(new MockEventTypeTime(event, this.timer.currentTimeMillis()));
            Common.eventManager.raiseEvent(event, 
                    this.timer.currentTimeMillis(), true, AlarmLevels.URGENT, 
                    new TranslatableMessage("common.default", "Mock Event"), null);
            timer.fastForwardTo(timer.currentTimeMillis() + 1);
        }
        
        timer.fastForwardTo(timer.currentTimeMillis() + 1000);
        waitForAllEvents(listeners, EventAction.RAISED, eventCount, 10000);
        
        for(MockUserEventListener l : listeners) {
            //Check to ensure the events were raised
            assertEquals(eventCount, l.getRaised().size());
            
            //Ensure all were returned and not duplicated
            for(MockEventTypeTime m : raised) {
                ListIterator<EventInstance> it  = l.getRaised().listIterator();
                while(it.hasNext()) {
                    EventInstance e = it.next();
                    if(m.time == e.getActiveTimestamp())
                        it.remove();
                        
                }
            }
        }
        
        for(MockUserEventListener l : listeners) {
            assertTrue(l.raised.size() == 0);
        }
        
        //Acknowledge
        List<MockEventTypeTime> acknowledged = new ArrayList<>();
        List<EventInstance> instances = EventDao.instance.getActiveEvents();
        for(EventInstance event : instances) {
            MockEventTypeTime rtn = new MockEventTypeTime((MockEventType)event.getEventType(), this.timer.currentTimeMillis());
            acknowledged.add(rtn);
            Common.eventManager.acknowledgeEventById(event.getId(), timer.currentTimeMillis(), users.get(0), null);
            timer.fastForwardTo(timer.currentTimeMillis() + 1);
        }
        
        timer.fastForwardTo(timer.currentTimeMillis() + 1000);
        waitForAllEvents(listeners, EventAction.ACKNOWLEDGED, eventCount, 10000);
        
        for(MockUserEventListener l : listeners) {
            //Check to ensure the events were acked
            assertEquals(eventCount, l.getAcknowledged().size());
            
            //Ensure all were acked and not duplicated
            for(MockEventTypeTime m : acknowledged) {
                ListIterator<EventInstance> it  = l.getAcknowledged().listIterator();
                while(it.hasNext()) {
                    EventInstance e = it.next();
                    if(m.time == e.getAcknowledgedTimestamp())
                        it.remove();
                        
                }
            }
        }

        for(MockUserEventListener l : listeners) {
            assertTrue(l.acknowledged.size() == 0);
        }
        
        //Return to normal
        List<MockEventTypeTime> returned = new ArrayList<>();
        for(MockEventTypeTime event : raised) {
            MockEventTypeTime rtn = new MockEventTypeTime(event.type, this.timer.currentTimeMillis());
            returned.add(rtn);
            Common.eventManager.returnToNormal(rtn.type, rtn.time);
            timer.fastForwardTo(timer.currentTimeMillis() + 1);
        }
        
        timer.fastForwardTo(timer.currentTimeMillis() + 1000);
        waitForAllEvents(listeners, EventAction.RETURNED, eventCount, 10000);
        
        for(MockUserEventListener l : listeners) {
            //Check to ensure the events were raised
            assertEquals(eventCount, l.getReturned().size());
            
            //Ensure all were returned and not duplicated
            for(MockEventTypeTime m : returned) {
                ListIterator<EventInstance> it  = l.getReturned().listIterator();
                while(it.hasNext()) {
                    EventInstance e = it.next();
                    if(m.time == e.getRtnTimestamp())
                        it.remove();
                        
                }
            }
        }
        
        for(MockUserEventListener l : listeners) {
            assertTrue(l.returned.size() == 0);
        }
        
    }
    
    @Test
    public void testDeactivateEvents() throws InterruptedException {
        
        int dataPointId = 17;
        int userCount = 2;
        int eventCount = 100;
        
        //Create some users
        List<User> users = createUsers(userCount, SuperadminPermissionDefinition.GROUP_NAME);
        
        List<MockUserEventListener> listeners = new ArrayList<>();
        for(User u : users) {
            MockUserEventListener l = new MockUserEventListener(u);
            listeners.add(l);
            Common.eventManager.addUserEventListener(l);
        }
        
        //Raise some events
        List<MockEventTypeTime> raised = new ArrayList<>();
        for(int i=0; i<eventCount; i++) {
            MockEventType event = new MockEventType(DuplicateHandling.ALLOW, null, i, dataPointId);
            raised.add(new MockEventTypeTime(event, this.timer.currentTimeMillis()));
            Common.eventManager.raiseEvent(event, 
                    this.timer.currentTimeMillis(), true, AlarmLevels.URGENT, 
                    new TranslatableMessage("common.default", "Mock Event"), null);
            timer.fastForwardTo(timer.currentTimeMillis() + 1);
        }
        
        timer.fastForwardTo(timer.currentTimeMillis() + 1000);
        waitForAllEvents(listeners, EventAction.RAISED, eventCount, 10000);
        
        for(MockUserEventListener l : listeners) {
            //Check to ensure the events were raised
            assertEquals(eventCount, l.getRaised().size());
            
            //Ensure all were returned and not duplicated
            for(MockEventTypeTime m : raised) {
                ListIterator<EventInstance> it  = l.getRaised().listIterator();
                while(it.hasNext()) {
                    EventInstance e = it.next();
                    if(m.time == e.getActiveTimestamp())
                        it.remove();
                        
                }
            }
        }
        
        for(MockUserEventListener l : listeners) {
            assertTrue(l.raised.size() == 0);
        }
        
        //Deactivate
        long deactivatedTime = this.timer.currentTimeMillis();
        Common.eventManager.cancelEventsForDataPoint(dataPointId);
        timer.fastForwardTo(timer.currentTimeMillis() + 1000);
        waitForAllEvents(listeners, EventAction.DEACTIVATED, eventCount, 10000);
        
        
        for(MockUserEventListener l : listeners) {
            //Check to ensure the events were raised
            assertEquals(eventCount, l.getDeactivated().size());
            
            //Ensure the time is correct
            for(EventInstance m : l.getDeactivated()) {
                assertEquals(deactivatedTime, m.getRtnTimestamp());
            }
        }
    }
    
    /**
     * Poll and wait for all listeners to recieve events
     * timeout in ms
     * 
     * @param listeners
     * @param eventCount
     */
    private void waitForAllEvents(List<MockUserEventListener> listeners, EventAction action, int eventCount, int timeout) {
        int waited = 0;
        while(true) {
            int finished = 0;
            for(MockUserEventListener l : listeners) {
                switch(action) {
                    case ACKNOWLEDGED:
                        if(l.getAcknowledged().size() == eventCount)
                            finished++;
                        break;
                    case DEACTIVATED:
                        if(l.getDeactivated().size() == eventCount)
                            finished++;
                        break;
                    case RAISED:
                        if(l.getRaised().size() == eventCount)
                            finished++;
                        break;
                    case RETURNED:
                        if(l.getReturned().size() == eventCount)
                            finished++;
                        break;
                    default:
                        break;
                }
            }
            if(finished == listeners.size())
                break;
            else
                try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}
            waited+=10;
            if(waited > timeout)
                fail("Failed to raise " + eventCount +" events for " + listeners.size() + " users.");
        }
        
    }
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.MangoTestBase#before()
     */
    @Override
    public void before() {
        super.before();
        Common.eventManager.purgeAllEvents();
    }
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.MangoTestBase#getLifecycle()
     */
    @Override
    protected MockMangoLifecycle getLifecycle() {
        EventManagerMockMangoLifecycle lifecycle = new EventManagerMockMangoLifecycle(modules, enableH2Web, h2WebPort);
        return lifecycle;
    }
    
    enum EventAction {
        RAISED,
        RETURNED,
        DEACTIVATED,
        ACKNOWLEDGED;
    }
    class MockEventTypeTime {
        MockEventType type;
        long time;
        
        /**
         * @param type
         * @param time
         */
        public MockEventTypeTime(MockEventType type, long time) {
            this.type = type;
            this.time = time;
        }
    }
    
    class MockUserEventListener implements UserEventListener {

        protected User user;
        protected List<EventInstance> raised = new ArrayList<>();
        protected List<EventInstance> returned = new ArrayList<>();
        protected List<EventInstance> deactivated = new ArrayList<>();
        protected List<EventInstance> acknowledged = new ArrayList<>();
        
        public MockUserEventListener(User user) {
            this.user = user;
        }
        
        /* (non-Javadoc)
         * @see com.serotonin.m2m2.rt.event.UserEventListener#getUserId()
         */
        @Override
        public int getUserId() {
            return user.getId();
        }

        /* (non-Javadoc)
         * @see com.serotonin.m2m2.rt.event.UserEventListener#raised(com.serotonin.m2m2.rt.event.EventInstance)
         */
        @Override
        public void raised(EventInstance evt) {
            this.raised.add(evt);
        }

        /* (non-Javadoc)
         * @see com.serotonin.m2m2.rt.event.UserEventListener#returnToNormal(com.serotonin.m2m2.rt.event.EventInstance)
         */
        @Override
        public void returnToNormal(EventInstance evt) {
            this.returned.add(evt);
        }

        /* (non-Javadoc)
         * @see com.serotonin.m2m2.rt.event.UserEventListener#deactivated(com.serotonin.m2m2.rt.event.EventInstance)
         */
        @Override
        public void deactivated(EventInstance evt) {
            this.deactivated.add(evt);
        }

        /* (non-Javadoc)
         * @see com.serotonin.m2m2.rt.event.UserEventListener#acknowledged(com.serotonin.m2m2.rt.event.EventInstance)
         */
        @Override
        public void acknowledged(EventInstance evt) {
            this.acknowledged.add(evt);
        }

        public User getUser() {
            return user;
        }

        public List<EventInstance> getRaised() {
            return raised;
        }

        public List<EventInstance> getReturned() {
            return returned;
        }

        public List<EventInstance> getDeactivated() {
            return deactivated;
        }

        public List<EventInstance> getAcknowledged() {
            return acknowledged;
        }
        
    }
    
    class EventManagerMockMangoLifecycle extends MockMangoLifecycle {

        /**
         * @param modules
         * @param enableWebConsole
         * @param webPort
         */
        public EventManagerMockMangoLifecycle(List<Module> modules, boolean enableWebConsole,
                int webPort) {
            super(modules, enableWebConsole, webPort);
        }
        
        /* (non-Javadoc)
         * @see com.serotonin.m2m2.MockMangoLifecycle#getEventManager()
         */
        @Override
        protected EventManager getEventManager() {
            return new EventManagerImpl();
        }
    }
}
