/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.publish.mock;

import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.PublisherDefinition;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.publish.PublisherVO;

/**
 *
 * @author Terry Packer
 */
public class MockPublisherDefinition extends PublisherDefinition<MockPublishedPointVO> {

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
    protected PublisherVO<MockPublishedPointVO> createPublisherVO() {
        MockPublisherVO pub = new MockPublisherVO();
        pub.setDefinition(this);
        return pub;
    }

    @Override
    public void validate(ProcessResult response, PublisherVO<MockPublishedPointVO> pub,
            PermissionHolder user) {
    }

}
