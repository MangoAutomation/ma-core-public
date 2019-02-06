/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.serotonin.m2m2.db.dao.EventDetectorDao;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * A way to manage only data point event detectors
 * 
 * @author Terry Packer
 *
 */
@Service
public class PointEventDetectorsService extends AbstractVOService<AbstractPointEventDetectorVO<?>, EventDetectorDao<AbstractPointEventDetectorVO<?>>>{

    @Autowired
    public PointEventDetectorsService(EventDetectorDao<AbstractPointEventDetectorVO<?>> dao) {
        super(dao);
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user) {
        return Permissions.hasDataSourcePermission(user);
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, AbstractPointEventDetectorVO<?> vo) {
        return Permissions.hasDataSourcePermission(user, vo.getDataPoint().getDataSourceId());
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, AbstractPointEventDetectorVO<?> vo) {
        return Permissions.hasDataPointReadPermission(user, vo.getDataPoint());
    }

}
