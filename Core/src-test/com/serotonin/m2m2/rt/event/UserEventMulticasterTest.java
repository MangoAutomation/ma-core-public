/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.event;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.EventType.DuplicateHandling;
import com.serotonin.m2m2.rt.event.type.MockEventType;
import com.serotonin.m2m2.vo.User;

/**
 * Test the logic of the multicaster
 * @author Terry Packer
 */
public class UserEventMulticasterTest {

    @Test
    public void testMulticastEventsForUser() {
        
        int dataPointId = 1;
        int eventCount = 10000;
        int userCount = 100;
        
        List<User> users = createUsers(userCount);
        List<MockUserEventListener> listeners = new ArrayList<>();
        UserEventListener multicaster = null;
        for(User u : users) {
            MockUserEventListener l = new MockUserEventListener(u);
            listeners.add(l);
            multicaster = UserEventMulticaster.add(l, multicaster);
        }
        

        List<EventInstance> events = new ArrayList<>();
        long time = 0;
        for(int i=0; i<eventCount; i++) {
            EventInstance event = createMockEventInstance(i, dataPointId, time);
            events.add(event);
            multicaster.raised(event);
            time += 1;
        }

        //Ack
        for(EventInstance e : events)
            multicaster.acknowledged(e);
        
        //Rtn
        for(EventInstance e : events)
            multicaster.returnToNormal(e);
        
        //Confirm all 100 were raised
        for(MockUserEventListener l : listeners)
            assertEquals(eventCount, l.getRaised().size());
        
        //Confirm all 100 were acked
        for(MockUserEventListener l : listeners)
            assertEquals(eventCount, l.getAcknowledged().size());
        
        //Confirm all 100 were rtned
        for(MockUserEventListener l : listeners)
            assertEquals(eventCount, l.getReturned().size());
    }
    
    public EventInstance createMockEventInstance(int id, int dataPointId, long time) {
        MockEventType type = new MockEventType(DuplicateHandling.ALLOW, null, id, dataPointId);
        return new EventInstance(type, time, true, AlarmLevels.URGENT, 
                new TranslatableMessage("common.default", "Mock Event " + id), null);
    }
    
    public List<User> createUsers(int size) {
        List<User> users = new ArrayList<>();
        for(int i=0; i<2; i++) {
            User user = new User();
            user.setId(Common.NEW_ID);
            user.setName("User" + i);
            user.setUsername("user" + i);
            user.setPassword("password");
            user.setEmail("user" + i + "@yourMangoDomain.com");
            user.setPhone("");
            user.setPermissions("superadmin");
            user.setDisabled(false);
            users.add(user);
        }
        return users;
    }
    
}
