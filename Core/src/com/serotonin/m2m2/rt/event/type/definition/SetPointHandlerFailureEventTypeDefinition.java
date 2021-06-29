/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.type.definition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.SystemEventTypeDefinition;
import com.serotonin.m2m2.module.definitions.event.handlers.SetPointEventHandlerDefinition;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
public class SetPointHandlerFailureEventTypeDefinition extends SystemEventTypeDefinition {

    @Override
    public String getTypeName() {
        return SystemEventType.TYPE_SET_POINT_HANDLER_FAILURE;
    }

    @Override
    public String getDescriptionKey() {
        return "event.system.setPoint";
    }

    @Override
    public String getEventListLink(int ref1, int ref2, Translations translations) {
        return null;
    }

    @Override
    public boolean supportsReferenceId1() {
        return true;
    }

    @Override
    public boolean supportsReferenceId2() {
        return false;
    }

    @Override
    public List<EventTypeVO> generatePossibleEventTypesWithReferenceId1(PermissionHolder user,
            String subtype) {
        if(StringUtils.equals(subtype, SystemEventType.TYPE_SET_POINT_HANDLER_FAILURE)) {
            EventTypeVO type = SystemEventType.getEventType(SystemEventType.TYPE_SET_POINT_HANDLER_FAILURE);
            List<EventTypeVO> types = new ArrayList<EventTypeVO>();
            List<AbstractEventHandlerVO> handlers = EventHandlerDao.getInstance().getEventHandlersByType(SetPointEventHandlerDefinition.TYPE_NAME);
            for(AbstractEventHandlerVO handler : handlers) {
                types.add(new EventTypeVO(new SystemEventType(SystemEventType.TYPE_SET_POINT_HANDLER_FAILURE, handler.getId()),
                        new TranslatableMessage("event.system.setPointFailureName", handler.getName()),
                        type.getAlarmLevel()
                        ));
            }
            return types;
        }else
            return Collections.emptyList();
    }

}
