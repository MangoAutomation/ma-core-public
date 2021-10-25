/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.infiniteautomation.mango.util.script.ScriptPermissions;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.event.handlers.EmailEventHandlerDefinition;
import com.serotonin.m2m2.rt.event.type.EventTypeMatcher;
import com.serotonin.m2m2.rt.event.type.MockEventType;
import com.serotonin.m2m2.vo.event.EmailEventHandlerVO;

import static org.junit.Assert.assertEquals;

public class EmailEventHandlerDaoTest extends AbstractVoDaoTest<EmailEventHandlerVO, EventHandlerDao> {

    @Test
    public void testHandlerMappings() {
        EmailEventHandlerVO handler = newVO();
        dao.insert(handler);
        EmailEventHandlerVO fromDB = (EmailEventHandlerVO) dao.get(handler.getId());
        assertEquals(1, fromDB.getEventTypes().size());

        //Update
        dao.saveEventHandlerMapping(fromDB.getId(), new MockEventType(readRole));
        fromDB = (EmailEventHandlerVO) dao.get(handler.getId());
        assertEquals(1, fromDB.getEventTypes().size());

        //Delete by type and handlerId
        dao.deleteEventHandlerMapping(fromDB.getId(), new MockEventType(readRole));
        fromDB = (EmailEventHandlerVO) dao.get(handler.getId());
        assertEquals(0, fromDB.getEventTypes().size());

        //Insert
        dao.saveEventHandlerMapping(fromDB.getId(), new MockEventType(readRole));
        fromDB = (EmailEventHandlerVO) dao.get(handler.getId());
        assertEquals(1, fromDB.getEventTypes().size());

        //Delete by type
        dao.deleteEventHandlerMappings(new MockEventType(readRole));
        fromDB = (EmailEventHandlerVO) dao.get(handler.getId());
        assertEquals(0, fromDB.getEventTypes().size());
    }

    @Override
    EventHandlerDao getDao() {
        return Common.getBean(EventHandlerDao.class);
    }

    @Override
    EmailEventHandlerVO newVO() {
        EmailEventHandlerVO vo = (EmailEventHandlerVO) ModuleRegistry.getEventHandlerDefinition(EmailEventHandlerDefinition.TYPE_NAME).baseCreateEventHandlerVO();
        vo.setXid(UUID.randomUUID().toString());
        vo.setName(UUID.randomUUID().toString());
        ScriptPermissions permissions = new ScriptPermissions(Collections.singleton(readRole));
        vo.setScriptRoles(permissions);
        List<EventTypeMatcher> eventTypes = Collections.singletonList(new EventTypeMatcher(new MockEventType(readRole)));
        vo.setEventTypes(eventTypes);
        return vo;
    }

    @Override
    EmailEventHandlerVO updateVO(EmailEventHandlerVO existing) {
        EmailEventHandlerVO copy = (EmailEventHandlerVO) existing.copy();
        copy.setName("new name");
        return copy;
    }

    @Override
    void assertVoEqual(EmailEventHandlerVO expected, EmailEventHandlerVO actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getXid(), actual.getXid());
        assertEquals(expected.getName(), actual.getName());
        assertPermission(expected.getReadPermission(), actual.getReadPermission());
        assertPermission(expected.getEditPermission(), actual.getEditPermission());

        assertRoles(((EmailEventHandlerVO)expected).getScriptRoles().getRoles(), ((EmailEventHandlerVO)actual).getScriptRoles().getRoles());

        List<EventTypeMatcher> actualEventTypes = actual.getEventTypes();
        List<EventTypeMatcher> expectedEventTypes = expected.getEventTypes();
        assertEquals(expectedEventTypes.size(), actualEventTypes.size());
        for (int i = 0;  i < expectedEventTypes.size(); i++) {
            EventTypeMatcher actualEventMatcher = actualEventTypes.get(i);
            EventTypeMatcher expectedEventMatcher = expectedEventTypes.get(i);
            assertEquals(expectedEventMatcher.getEventType(), actualEventMatcher.getEventType());
            assertEquals(expectedEventMatcher.getEventSubtype(), actualEventMatcher.getEventSubtype());
            assertEquals(expectedEventMatcher.getReferenceId1(), actualEventMatcher.getReferenceId1());
            assertEquals(expectedEventMatcher.getReferenceId2(), actualEventMatcher.getReferenceId2());
        }

        //TODO assert remaining
    }
}
