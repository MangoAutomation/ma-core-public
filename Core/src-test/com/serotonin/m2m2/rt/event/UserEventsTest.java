/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private final int dataPointId = 17;
    private final int userCount = 5;
    private final int eventCount = 1000;

    @Test
    public void testListenerAddRemoveSyncrhonization() throws InterruptedException {
        List<User> users = createUsers(1, SuperadminPermissionDefinition.GROUP_NAME);
        
        ExecutorService executor = Executors.newFixedThreadPool(3);
        SynchronousQueue<MockUserEventListener> queue = new SynchronousQueue<>();
        MockUserEventListener l = new MockUserEventListener(users.get(0));

        AtomicBoolean removerRunning = new AtomicBoolean(true);
        AtomicBoolean generatorRunning = new AtomicBoolean(true);
        Runnable adder = () -> {
            while(removerRunning.get()) {
                try {
                    MockUserEventListener adding = queue.take();
                    Common.eventManager.addUserEventListener(adding);
                }catch(Exception e) {
                    e.printStackTrace();
                }
            }
        };
        Runnable remover = () -> {
            while(generatorRunning.get()) {
                try {
                    queue.put(l);
                    Common.eventManager.removeUserEventListener(l);
                }catch(Exception e) {
                    e.printStackTrace();
                }
            }
            removerRunning.set(false);
        };
        
        //Event Generator Thread
        Runnable generator = () -> {

            //Raise some events
            List<MockEventTypeTime> raised = new ArrayList<>();
            for(int i=0; i<1000; i++) {
                MockEventType event = new MockEventType(DuplicateHandling.ALLOW, null, i, dataPointId);
                raised.add(new MockEventTypeTime(event, timer.currentTimeMillis()));
                Common.eventManager.raiseEvent(event, 
                        timer.currentTimeMillis(), true, AlarmLevels.URGENT, 
                        new TranslatableMessage("common.default", "Mock Event"), null);
                timer.fastForwardTo(timer.currentTimeMillis() + 1);
            }
            generatorRunning.set(false);
        };
        executor.execute(adder);
        executor.execute(remover);
        executor.execute(generator);
        executor.awaitTermination(1000, TimeUnit.MILLISECONDS);
        executor.shutdown();
    }
    
    @Test
    public void testRaiseEvents() throws InterruptedException {
        
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
        assertEvents(listeners, EventAction.RAISED, raised);
        
        //Acknowledge
        List<MockEventTypeTime> acknowledged = new ArrayList<>();
        List<EventInstance> instances = EventDao.getInstance().getActiveEvents();
        for(EventInstance event : instances) {
            MockEventTypeTime rtn = new MockEventTypeTime((MockEventType)event.getEventType(), this.timer.currentTimeMillis());
            acknowledged.add(rtn);
            Common.eventManager.acknowledgeEventById(event.getId(), timer.currentTimeMillis(), users.get(0), null);
            timer.fastForwardTo(timer.currentTimeMillis() + 1);
        }
        assertEvents(listeners, EventAction.ACKNOWLEDGED, acknowledged);
        
        //Return to normal
        List<MockEventTypeTime> returned = new ArrayList<>();
        for(MockEventTypeTime event : raised) {
            MockEventTypeTime rtn = new MockEventTypeTime(event.type, this.timer.currentTimeMillis());
            returned.add(rtn);
            Common.eventManager.returnToNormal(rtn.type, rtn.time);
            timer.fastForwardTo(timer.currentTimeMillis() + 1);
        }
        
        assertEvents(listeners, EventAction.RETURNED, returned);
    }
    
    @Test
    public void testDeactivateEvents() throws InterruptedException {
        
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
        assertEvents(listeners, EventAction.RAISED, raised);

        
        //Deactivate
        long deactivatedTime = Common.timer.currentTimeMillis();
        //Simulate the events here so we can verify them, they should be at time now
        Common.eventManager.cancelEventsForDataPoint(dataPointId);
        timer.fastForwardTo(timer.currentTimeMillis() + 1000);

        //Ensure all events are deactivated at the right time
        for(MockUserEventListener l : listeners) {
            assertEquals(raised.size(), l.getDeactivated().size());
            for(EventInstance e : l.getDeactivated()) {
                assertEquals(deactivatedTime, e.getRtnTimestamp());
            }
        }

    }
    
    /**
     * Check the number raised and if there are duplicates
     * @param listeners
     * @param action
     * @param events
     */
    private void assertEvents(List<MockUserEventListener> listeners, EventAction action, List<MockEventTypeTime> events) {
        
        //Check size of list
        StringBuilder b = new StringBuilder();
        for(MockUserEventListener l : listeners) {
            switch(action) {
                case ACKNOWLEDGED:
                    if(l.getAcknowledged().size() != events.size())
                        b.append("Failed to ack all events for " + l.getUser().getUsername() + ", " + l.getAcknowledged().size() + " out of " + eventCount + " ack'd\n");
                    break;
                case DEACTIVATED:
                    if(l.getDeactivated().size() != events.size())
                        b.append("Failed to deactivate all events for " + l.getUser().getUsername() + ", " + l.getDeactivated().size() + " out of " + eventCount + " deactivated\n");
                    break;
                case RAISED:
                    if(l.getRaised().size() != events.size())
                        b.append("Failed to raise all events for " + l.getUser().getUsername() + ", " + l.getRaised().size() + " out of " + eventCount + " raised\n");
                    break;
                case RETURNED:
                    if(l.getReturned().size() != events.size())
                        b.append("Failed to rtn all events for " + l.getUser().getUsername() + ", " + l.getReturned().size() + " out of " + eventCount + " rtn'd\n");
                    break;
                default:
                    break;
            }
        }
        if(!b.toString().isEmpty())
            fail(b.toString());

        //Ensure all were returned and not duplicated
        if(events != null) {
            for(MockUserEventListener l : listeners) {
                List<EventInstance> listenedEvents = new ArrayList<>(events.size());
                for(MockEventTypeTime m : events) {
                    ListIterator<EventInstance> it;
                    switch(action) {
                        case ACKNOWLEDGED:
                            it  = l.getAcknowledged().listIterator();
                            while(it.hasNext()) {
                                EventInstance e = it.next();
                                if(m.time == e.getAcknowledgedTimestamp())
                                    listenedEvents.add(e);
                            }      
                            break;
                        case RAISED:
                            it  = l.getRaised().listIterator();
                            while(it.hasNext()) {
                                EventInstance e = it.next();
                                if(m.time == e.getActiveTimestamp())
                                    listenedEvents.add(e);
                            }
                            break;
                        case RETURNED:
                            it  = l.getReturned().listIterator();
                            while(it.hasNext()) {
                                EventInstance e = it.next();
                                if(m.time == e.getRtnTimestamp())
                                    listenedEvents.add(e);
                            }      
                            break;
                        default:
                            fail("No case for " + action);
                            break;
                    }
                }
                //Ensure that there are no duplicates or doubles
                assertEquals(events.size(), listenedEvents.size());
            }
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
