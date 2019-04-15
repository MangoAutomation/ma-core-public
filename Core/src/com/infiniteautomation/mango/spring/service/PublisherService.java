/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.publish.PublisherVO;

/**
 * @author Terry Packer
 *
 */
@Service
public class PublisherService extends AbstractVOService<PublisherVO<?>, PublisherDao> {

    @Autowired 
    public PublisherService(PublisherDao dao) {
        super(dao);
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user, PublisherVO<?> vo) {
        return user.hasAdminPermission();
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, PublisherVO<?> vo) {
        return user.hasAdminPermission();
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, PublisherVO<?> vo) {
        return user.hasAdminPermission();
    }

}
