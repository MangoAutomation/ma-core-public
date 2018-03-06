/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.publish.mock;

import com.serotonin.m2m2.module.PublisherDefinition;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.publisher.AbstractPublishedPointModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.publisher.AbstractPublisherModel;

/**
 *
 * @author Terry Packer
 */
public class MockPublisherDefinition extends PublisherDefinition{

    public static final String TYPE_NAME = "MOCK";
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.PublisherDefinition#getPublisherTypeName()
     */
    @Override
    public String getPublisherTypeName() {
        return TYPE_NAME;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.PublisherDefinition#getDescriptionKey()
     */
    @Override
    public String getDescriptionKey() {
        return "common.default";
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.PublisherDefinition#createPublisherVO()
     */
    @Override
    protected PublisherVO<? extends PublishedPointVO> createPublisherVO() {
        MockPublisherVO pub = new MockPublisherVO();
        pub.setDefinition(this);
        return pub;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.PublisherDefinition#getEditPagePath()
     */
    @Override
    public String getEditPagePath() {
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.PublisherDefinition#getDwrClass()
     */
    @Override
    public Class<?> getDwrClass() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.PublisherDefinition#getPublisherModelClass()
     */
    @Override
    public Class<? extends AbstractPublisherModel<?, ?>> getPublisherModelClass() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.PublisherDefinition#getPublishedPointModelClass()
     */
    @Override
    public Class<? extends AbstractPublishedPointModel<?>> getPublishedPointModelClass() {
        // TODO Auto-generated method stub
        return null;
    }

}
