/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.publish.mock;

import java.util.List;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.publish.PublisherRT;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.publisher.AbstractPublisherModel;

/**
 *
 * @author Terry Packer
 */
public class MockPublisherVO extends PublisherVO<MockPublishedPointVO>{

    /**
     * 
     */
    private static final long serialVersionUID = -1L;

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.vo.publish.PublisherVO#asModel()
     */
    @Override
    public AbstractPublisherModel<?, ?> asModel() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.vo.publish.PublisherVO#getConfigDescription()
     */
    @Override
    public TranslatableMessage getConfigDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.vo.publish.PublisherVO#createPublishedPointInstance()
     */
    @Override
    protected MockPublishedPointVO createPublishedPointInstance() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.vo.publish.PublisherVO#createPublisherRT()
     */
    @Override
    public PublisherRT<MockPublishedPointVO> createPublisherRT() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.vo.publish.PublisherVO#getEventTypesImpl(java.util.List)
     */
    @Override
    protected void getEventTypesImpl(List<EventTypeVO> eventTypes) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.vo.publish.PublisherVO#getEventCodes()
     */
    @Override
    public ExportCodes getEventCodes() {
        // TODO Auto-generated method stub
        return null;
    }

}
