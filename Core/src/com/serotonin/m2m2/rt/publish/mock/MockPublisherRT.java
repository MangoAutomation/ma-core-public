/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.rt.publish.mock;

import com.serotonin.m2m2.rt.publish.PublisherRT;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.m2m2.vo.publish.mock.MockPublishedPointVO;

/**
 *
 * @author Terry Packer
 */
public class MockPublisherRT extends PublisherRT<MockPublishedPointVO> {

    public MockPublisherRT(PublisherVO<MockPublishedPointVO> vo) {
        super(vo);
    }

    @Override
    public void initialize() {

    }

    @Override
    public void terminateImpl() {

    }

}
