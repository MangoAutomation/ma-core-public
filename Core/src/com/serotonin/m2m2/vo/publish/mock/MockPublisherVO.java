/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.publish.mock;

import java.util.List;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.publish.PublisherRT;
import com.serotonin.m2m2.rt.publish.mock.MockPublisherRT;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;

/**
 *
 * @author Terry Packer
 */
public class MockPublisherVO extends PublisherVO<MockPublishedPointVO> {

    private static final long serialVersionUID = -1L;

    private static final ExportCodes EVENT_CODES = new ExportCodes();
    static {
        PublisherVO.addDefaultEventCodes(EVENT_CODES);
    }

    @Override
    public TranslatableMessage getConfigDescription() {
        return new TranslatableMessage("event.audit.publisher");
    }

    @Override
    protected MockPublishedPointVO createPublishedPointInstance() {
        return new MockPublishedPointVO();
    }

    @Override
    public PublisherRT<MockPublishedPointVO> createPublisherRT() {
        return new MockPublisherRT(this);
    }

    @Override
    protected void getEventTypesImpl(List<EventTypeVO> eventTypes) {

    }

    @Override
    public ExportCodes getEventCodes() {
        return EVENT_CODES;
    }

}
