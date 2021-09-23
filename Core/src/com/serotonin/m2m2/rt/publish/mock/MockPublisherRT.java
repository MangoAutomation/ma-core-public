/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.rt.publish.mock;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.publish.PublishQueue;
import com.serotonin.m2m2.rt.publish.PublisherRT;
import com.serotonin.m2m2.rt.publish.SendThread;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.m2m2.vo.publish.mock.MockPublishedPointVO;

/**
 *
 * @author Terry Packer
 */
public class MockPublisherRT extends PublisherRT<MockPublishedPointVO> {
    PublisherVO<MockPublishedPointVO> vo;
    public MockPublisherRT(PublisherVO<MockPublishedPointVO> vo) {
        super(vo);
        this.vo = vo;
    }

    @Override
    public void initialize() {
        super.initialize(new MockSendThread());
    }

    @Override
    public void terminateImpl() {

    }

    PublishQueue<MockPublishedPointVO, PointValueTime> getPublishQueue() {
        return queue;
    }


    class MockSendThread extends SendThread {

        public MockSendThread() {
            super("MockPublisherRT.SendThread");
        }

        @Override
        protected void runImpl() {

        }
    }
}
