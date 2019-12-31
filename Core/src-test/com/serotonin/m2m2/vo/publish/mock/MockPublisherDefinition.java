/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.publish.mock;

import com.serotonin.m2m2.module.PublisherDefinition;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;

/**
 *
 * @author Terry Packer
 */
public class MockPublisherDefinition extends PublisherDefinition{

    public static final String TYPE_NAME = "MOCK";
    
    @Override
    public String getPublisherTypeName() {
        return TYPE_NAME;
    }

    @Override
    public String getDescriptionKey() {
        return "common.default";
    }

    @Override
    protected PublisherVO<? extends PublishedPointVO> createPublisherVO() {
        MockPublisherVO pub = new MockPublisherVO();
        pub.setDefinition(this);
        return pub;
    }

}
