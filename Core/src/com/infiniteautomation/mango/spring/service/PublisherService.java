/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;

/**
 * @author Terry Packer
 *
 */
@Service
public class PublisherService<T extends PublishedPointVO> extends AbstractVOService<PublisherVO<T>, PublisherDao<T>> {

    @Autowired 
    public PublisherService(PublisherDao<T> dao, PermissionService permissionService) {
        super(dao, permissionService);
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user, PublisherVO<T> vo) {
        return user.hasAdminPermission();
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, PublisherVO<T> vo) {
        return user.hasAdminPermission();
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, PublisherVO<T> vo) {
        return user.hasAdminPermission();
    }

    @Override
    protected PublisherVO<T> insert(PublisherVO<T> vo, PermissionHolder user, boolean full)
            throws PermissionException, ValidationException {
        //Ensure they can create a list
        ensureCreatePermission(user, vo);
        
        //Ensure we don't presume to exist
        if(vo.getId() != Common.NEW_ID) {
            ProcessResult result = new ProcessResult();
            result.addContextualMessage("id", "validate.invalidValue");
            throw new ValidationException(result);
        }
        
        //Generate an Xid if necessary
        if(StringUtils.isEmpty(vo.getXid()))
            vo.setXid(dao.generateUniqueXid());
        
        ensureValid(vo, user);
        Common.runtimeManager.savePublisher(vo);
        
        return vo;
    }
    
    
    @Override
    protected PublisherVO<T> update(PublisherVO<T> existing, PublisherVO<T> vo,
            PermissionHolder user, boolean full) throws PermissionException, ValidationException {
        ensureEditPermission(user, existing);
        
        //Ensure matching data source types
        if(!StringUtils.equals(existing.getDefinition().getPublisherTypeName(), vo.getDefinition().getPublisherTypeName())) {
            ProcessResult result = new ProcessResult();
            result.addContextualMessage("definition.publisherTypeName", "validate.publisher.incompatiblePublisherType");
            throw new ValidationException(result);
        }
        
        vo.setId(existing.getId());
        ensureValid(existing, vo, user);
        Common.runtimeManager.savePublisher(vo);
        return vo;
    }
    
    @Override
    public PublisherVO<T> delete(String xid, PermissionHolder user)
            throws PermissionException, NotFoundException {
        PublisherVO<T> vo = get(xid, user);
        ensureDeletePermission(user, vo);
        Common.runtimeManager.deletePublisher(vo.getId());
        return vo;
    }
    
    /**
     * @param xid
     * @param enabled
     * @param restart
     * @param user
     */
    public void restart(String xid, boolean enabled, boolean restart, User user) {
        PublisherVO<T>  vo = getFull(xid, user);
        ensureEditPermission(user, vo);
        if (enabled && restart) {
            vo.setEnabled(true);
            Common.runtimeManager.savePublisher(vo); //saving will restart it
        } else if(vo.isEnabled() != enabled) {
            vo.setEnabled(enabled);
            Common.runtimeManager.savePublisher(vo);
        }        
    }

}
