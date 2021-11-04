/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.rt.publish.mock;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.publish.PublishQueue;
import com.serotonin.m2m2.rt.publish.PublisherRT;
import com.serotonin.m2m2.rt.publish.SendThread;
import com.serotonin.m2m2.vo.publish.mock.MockPublishedPointVO;
import com.serotonin.m2m2.vo.publish.mock.MockPublisherVO;

/**
 *
 * @author Terry Packer
 */
public class MockPublisherRT extends PublisherRT<MockPublisherVO, MockPublishedPointVO, MockPublisherRT.MockSendThread> {

    public MockPublisherRT(MockPublisherVO vo) {
        super(vo);
    }

    @Override
    protected MockSendThread createSendThread() {
        return new MockSendThread();
    }

    @Override
    public void terminateImpl() {

    }

    PublishQueue<MockPublisherVO, MockPublishedPointVO, PointValueTime> getPublishQueue() {
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
