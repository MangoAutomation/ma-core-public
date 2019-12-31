/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.publish.mock;

import java.util.List;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.publish.PublisherRT;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.publish.PublisherVO;

/**
 *
 * @author Terry Packer
 */
public class MockPublisherVO extends PublisherVO<MockPublishedPointVO>{

    /**
     * 
     */
    private static final long serialVersionUID = -1L;

    @Override
    public TranslatableMessage getConfigDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected MockPublishedPointVO createPublishedPointInstance() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PublisherRT<MockPublishedPointVO> createPublisherRT() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void getEventTypesImpl(List<EventTypeVO> eventTypes) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public ExportCodes getEventCodes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void validate(ProcessResult result, PermissionService service,
            PermissionHolder savingUser) {
        // TODO Auto-generated method stub
        
    }

}
