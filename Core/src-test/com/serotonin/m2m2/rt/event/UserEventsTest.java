/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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

import com.infiniteautomation.mango.spring.components.RunAs;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockEventManager;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.DuplicateHandling;
import com.serotonin.m2m2.rt.event.type.MockEventType;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 *
 * @author Terry Packer
 */
public class UserEventsTest extends MangoTestBase {

    private final int dataPointId = 17;
    private final int userCount = 5;
    private final int eventCount = 3; //1000;

    @Test
    public void testListenerAddRemoveSyncrhonization() throws InterruptedException {
        List<User> users = createUsers(1, PermissionHolder.SUPERADMIN_ROLE);

        ExecutorService executor = Executors.newFixedThreadPool(3);
        SynchronousQueue<MockUserEventListener> queue = new SynchronousQueue<>();
        MockUserEventListener l = new MockUserEventListener(users.get(0));

        AtomicBoolean removerRunning = new AtomicBoolean(true);
        AtomicBoolean generatorRunning = new AtomicBoolean(true);

        RunAs runAs = Common.getBean(RunAs.class);
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
            runAs.runAs(PermissionHolder.SYSTEM_SUPERADMIN, () -> {
                //Raise some events
                List<MockEventTypeTime> raised = new ArrayList<>();
                for (int i = 0; i < 1000; i++) {
                    MockEventType event = new MockEventType(DuplicateHandling.ALLOW, null, i, dataPointId, null);
                    raised.add(new MockEventTypeTime(event, timer.currentTimeMillis()));
                    Common.eventManager.raiseEvent(event,
                            timer.currentTimeMillis(), true, AlarmLevels.URGENT,
                            new TranslatableMessage("common.default", "Mock Event"), null);
                    timer.fastForwardTo(timer.currentTimeMillis() + 1);
                }
                generatorRunning.set(false);
            });
        };

        executor.execute(adder);
        executor.execute(remover);
        executor.execute(generator);
        int maxWaitSeconds = 30;
        for(int i=maxWaitSeconds; i>0; i--) {
            if(!generatorRunning.get()) {
                break;
            }
            try {Thread.sleep(1000);} catch(Exception e) { }
        }
        executor.shutdown();
        executor.awaitTermination(10000, TimeUnit.MILLISECONDS);
        executor.shutdownNow();
    }

    @Test
    public void testRaiseEvents() throws InterruptedException {

        //Create some users
        List<User> users = createUsers(userCount, PermissionHolder.SUPERADMIN_ROLE);

        List<MockUserEventListener> listeners = new ArrayList<>();
        for(User u : users) {
            MockUserEventListener l = new MockUserEventListener(u, MockEventType.class);
            listeners.add(l);
            Common.eventManager.addUserEventListener(l);
        }

        //Raise some events
        List<MockEventTypeTime> raised = new ArrayList<>();
        for(int i=0; i<eventCount; i++) {
            MockEventType event = new MockEventType(DuplicateHandling.ALLOW, null, i, dataPointId, null);
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
        List<User> users = createUsers(userCount, PermissionHolder.SUPERADMIN_ROLE);

        List<MockUserEventListener> listeners = new ArrayList<>();
        for(User u : users) {
            MockUserEventListener l = new MockUserEventListener(u, MockEventType.class);
            listeners.add(l);
            Common.eventManager.addUserEventListener(l);
        }

        //Raise some events
        List<MockEventTypeTime> raised = new ArrayList<>();
        for(int i=0; i<eventCount; i++) {
            MockEventType event = new MockEventType(DuplicateHandling.ALLOW, null, i, dataPointId, null);
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
                assertEquals(deactivatedTime, (long) e.getRtnTimestamp());
            }
        }

    }

    /**
     * Check the number raised and if there are duplicates
     */
    private void assertEvents(List<MockUserEventListener> listeners, EventAction action, List<MockEventTypeTime> events) throws InterruptedException {

        for(int i=0; i<30; i++) {
            int waitingFor = listeners.size();
            for(MockUserEventListener l : listeners) {
                switch(action) {
                    case ACKNOWLEDGED:
                        if(l.getAcknowledged().size() == events.size())
                            waitingFor--;
                        break;
                    case DEACTIVATED:
                        if(l.getDeactivated().size() == events.size())
                            waitingFor--;
                        break;
                    case RAISED:
                        if(l.getRaised().size() == events.size())
                            waitingFor--;
                        break;
                    case RETURNED:
                        if(l.getReturned().size() == events.size())
                            waitingFor--;
                        break;
                }
            }

            if(waitingFor == 0)
                break;
            Thread.sleep(500);
        }

        //Check size of list and wait some time before failing for the other threads to finish
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

    @Override
    public void before() {
        super.before();
        Common.eventManager.purgeAllEvents();
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
         */
        public MockEventTypeTime(MockEventType type, long time) {
            this.type = type;
            this.time = time;
        }
    }

    @Override
    protected MockMangoLifecycle getLifecycle() {
        MockMangoLifecycle lifecycle = super.getLifecycle();
        lifecycle.setEventManager(new MockEventManager(true));
        return lifecycle;
    }
}
