/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.type;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.EventTypeDefinition;
import com.serotonin.m2m2.rt.event.type.EventType.DuplicateHandling;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel;

/**
 *
 * @author Terry Packer
 */
public class MockEventTypeDefinition extends EventTypeDefinition {

    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.EventTypeDefinition#getTypeName()
     */
    @Override
    public String getTypeName() {
        return MockEventType.TYPE_NAME;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.EventTypeDefinition#getEventTypeClass()
     */
    @Override
    public Class<? extends EventType> getEventTypeClass() {
        return MockEventType.class;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.EventTypeDefinition#createEventType(java.lang.String, int, int)
     */
    @Override
    public EventType createEventType(String subtype, int ref1, int ref2) {
        return new MockEventType(DuplicateHandling.ALLOW, subtype, ref1, ref2);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.EventTypeDefinition#getHandlersRequireAdmin()
     */
    @Override
    public boolean getHandlersRequireAdmin() {
        return false;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.EventTypeDefinition#getEventTypeVOs()
     */
    @Override
    public List<EventTypeVO> getEventTypeVOs() {
        //TODO implement when necessary
        return new ArrayList<EventTypeVO>();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.EventTypeDefinition#getIconPath()
     */
    @Override
    public String getIconPath() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.EventTypeDefinition#getDescriptionKey()
     */
    @Override
    public String getDescriptionKey() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.EventTypeDefinition#getEventListLink(java.lang.String, int, int, com.serotonin.m2m2.i18n.Translations)
     */
    @Override
    public String getEventListLink(String subtype, int ref1, int ref2, Translations translations) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.EventTypeDefinition#getSourceDisabledMessage()
     */
    @Override
    public TranslatableMessage getSourceDisabledMessage() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.EventTypeDefinition#getModelClass()
     */
    @Override
    public Class<? extends EventTypeModel> getModelClass() {
        // TODO Auto-generated method stub
        return null;
    }

}
