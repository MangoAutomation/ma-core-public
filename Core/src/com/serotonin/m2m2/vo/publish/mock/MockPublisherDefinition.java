/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.publish.mock;

import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.ConditionalDefinition;
import com.serotonin.m2m2.module.PublisherDefinition;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 *
 * @author Terry Packer
 */
@ConditionalDefinition("testing.enabled")
public class MockPublisherDefinition extends PublisherDefinition<MockPublisherVO> {

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
    protected MockPublisherVO createPublisherVO() {
        MockPublisherVO pub = new MockPublisherVO();
        pub.setDefinition(this);
        return pub;
    }

    @Override
    public void validate(ProcessResult response, MockPublisherVO pub,
            PermissionHolder user) {
    }

}
